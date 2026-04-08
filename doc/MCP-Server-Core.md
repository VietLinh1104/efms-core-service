# Tích hợp Model Context Protocol (MCP) Server cho EFMS Core

Tài liệu này mô tả thiết kế và đặc tả kỹ thuật để xây dựng một **MCP Server** cho `efms-core-service`. Mục tiêu là cung cấp một giao diện chuẩn HTTP/SSE hoặc Stdio giúp các trợ lý ảo AI (như Antigravity, Claude, Cursor) có thể hiểu ngữ cảnh dữ liệu tài chính, trực tiếp truy vấn, và thực thi các nghiệp vụ trong hệ thống EFMS Core (kế toán, hóa đơn, quản lý Camunda workflow).

---

## 1. Kiến trúc & Tầng Giao tiếp (Transport Layer)

Có thể triển khai MCP Server cho Core Service thông qua hai hướng chính tùy thuộc vào môi trường sử dụng:

1. **Local Sidecar (Stdio Transport)**: Xây dựng một mini server (bằng TypeScript hoặc Python) chạy dạng process độc lập trên máy local của developer. Server này giao tiếp với AI Client qua `stdio` và kết nối trực tiếp với PostgreSQL/API cục bộ của Core.
2. **Spring Boot Native (SSE Transport)**: Tích hợp thư viện Java MCP (như Spring AI MCP) trực tiếp vào `efms-core-service`. Expose endpoint dạng Server-Sent Events (ví dụ `/mcp/messages`) để các AI Agent giao tiếp phân tán trong môi trường Cloud/Staging. Dùng chung Security/JWT của ứng dụng hiện tại.

---

## 2. Resources (Tài nguyên cấp ngữ cảnh cho AI)

Resources là các điểm neo (URI) cho phép AI trực tiếp đọc nội dung để thu thập thông tin bối cảnh. Hệ thống nên định nghĩa các custom scheme sau:

*   `efms-core://database/schema-v4`: Toàn bộ cấu trúc DDL mô tả các bảng (Invoices, Journals, Accounts) của Database V4.
*   `efms-core://invoices/{invoiceId}`: Trả về nội dung JSON chi tiết của một AP Bill / AR Invoice (bao gồm cả trạng thái thanh toán, status, process instance key của Camunda).
*   `efms-core://journals/{journalId}`: Cung cấp nội dung bút toán (debist/credits), lý do tạo bút toán và liên kết hóa đơn.
*   `efms-core://chart-of-accounts/{companyId}`: Trả về danh sách cấu trúc hệ thống tài khoản hiện hành (Sổ cái) của một công ty cụ thể.
*   `efms-core://camunda/status/{processInstanceKey}`: Thông tin runtime lấy từ Camunda Tasklist và Zeebe engine xem quy trình đang mắc kẹt hay dừng ở node nào.

---

## 3. Tools (Công cụ AI có thể thực thi - Function Calling)

Đây là các hàm mà AI Agent có thể yêu cầu server thực thi. Cần cung cấp JSON Schema định nghĩa các arguments rõ ràng:

### 3.1. `search_invoices`
*   **Mô tả**: Tìm kiếm hóa đơn dựa theo trạng thái, loại chứng từ và phân giới thời gian.
*   **Tham số**: 
    *   `type` (enum): `AP_BILL` hoặc `AR_INVOICE`.
    *   `status` (enum): `draft`, `open`, `paid`, ...
    *   `approval_status` (enum): `pending`, `approved`, `rejected`
*   **Trả về**: Danh sách tóm tắt các Invoices thỏa mãn tiêu chí.

### 3.2. `approve_ap_bill`
*   **Mô tả**: Phê duyệt hoặc từ chối một AP Bill, tự động tương tác với Camunda Zeebe để hoàn tất User Task (điều hướng Workflow).
*   **Tham số**:
    *   `invoiceId` (uuid)
    *   `approved` (boolean): `true` (Duyệt) / `false` (Từ chối).
    *   `comment` (string): Ghi chú phê duyệt đính kèm.
*   **Trả về**: Kết quả của thao tác gọi hệ thống Camunda và trạng thái sau cùng.

### 3.3. `create_draft_journal_entry`
*   **Mô tả**: Đề xuất tạo ráp một bút toán ghi sổ đôi trước khi User Confirm.
*   **Tham số**:
    *   `description` (string)
    *   `journalLines` (array): Danh sách các Object gồm `accountId`, `type` (DEBIT/CREDIT), và `amount` (number/BigDecimal).
*   **Trả về**: Thông tin Journal vừa được tạo dưới dạng `draft`.

### 3.4. `get_account_balance`
*   **Mô tả**: Truy vấn số dư tức thì của một mã tài khoản kế toán.
*   **Tham số**:
    *   `accountId` (uuid)
    *   `fiscalPeriodId` (uuid - tùy chọn).

---

## 4. Prompts (Kịch bản xây dựng sẵn)

MCP cung cấp các prompt template để user dễ dàng gõ cho AI:

*   **`review_ap_bill`**:
    *   *Mô tả*: Hỗ trợ xét duyệt hóa đơn nhà cung cấp.
    *   *Nội dung Prompt*: "Hãy lấy thông tin chi tiết của hóa đơn {invoiceId} từ resource `efms-core://invoices/{invoiceId}`. Rà soát xem tổng tiền có phù hợp với chính sách (VD: > 100M cần phê duyệt đặc biệt không) rồi đề suất việc duyệt (approve) hoặc từ chối (reject)."
*   **`reconcile_bank_transaction`**:
    *   *Mô tả*: Trợ lý gạch nợ (Đối soát ngân hàng).
    *   *Nội dung Prompt*: "Tôi vừa có dòng tiền ngân hàng {transactionId}. Hãy gọi tool `search_invoices` tìm các hóa đơn ở trạng thái OPEN chưa thanh toán để đề xuất tài khoản phân bổ thích hợp."

---

## 5. Vấn đề Phân quyền & Định danh (Multi-Company & RBAC)

Vì kiến trúc EFMS phân tách dữ liệu đa công ty (`Multi-tenancy`), MCP Server đối với Core cần cẩn trọng trong việc thiết lập Application Context:
*   Nếu AI chạy với tư cách Local Admin: Phải inject một Context tùy chỉnh, cần phải chủ động truyền tham số `companyId` vào mỗi tool.
*   Nếu trên Production (REST/SSE): Yêu cầu người dùng (và LLM Client) truyền Bearer JWT đính kèm yêu cầu (từ `efms-identity-service`) để phân quyền tự động giống như Client Web/App. Ngăn việc AI vô tình lấy thông tin Invoice của Company khác.
