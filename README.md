# Bookstore Order Service Documentation

Đây là tài liệu chi tiết mô tả kiến trúc, các API, cấu hình và sự phụ thuộc của `bookstore-order-service` sau quá trình chuyển đổi từ monolithic sang microservice.

---

## 1. Giới thiệu chức năng
`bookstore-order-service` chịu trách nhiệm toàn bộ về quy trình đặt hàng: 
- Tạo mới đơn hàng (Checkout).
- Quản lý trạng thái đơn đặt hàng.
- Lưu trữ thông tin chi tiết các mặt hàng trong đơn, tổng tiền, thuế, phí ship.
- Hỗ trợ admin cập nhật trạng thái đơn (Xác nhận, Đóng gói, Vận chuyển, Hoàn thành).

---

## 2. Kiến trúc & Model hệ thống (Entity & DTO)

### Các Entity Chính
- **`Order`**: Lưu thông tin cốt lõi của một đơn đặt hàng bao gồm `userId` (chuỗi string, không khóa ngoại), tổng giá, trạng thái đơn, và `ShippingDetails` (embedded).
- **`OrderItem`**: Mô tả chi tiết sách (Book ID dưới dạng String để mapping lỏng lẻo với `book-service`), số lượng, đơn giá và tổng phụ.
- **`ShippingDetails`**: Một `@Embeddable` class chứa thông tin người nhận, số điện thoại, địa chỉ cụ thể.
- **`OrderStatus` (Enum)**: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`.

### Các DTO (Data Transfer Object)
- **`CheckoutRequest`**: Payload dùng để người dùng tạo đơn. Chứa `addressId`, `paymentMethod`, `bookIds` (danh sách ID cuốn sách muốn mua), `discountCode`, v.v.
- **`OrderResponse` / `OrderItemResponse`**: DTO trả về thông tin sạch sẽ, không chứa các thuộc tính lộ cấu trúc Entity nội bộ.
- **`ApiResponse<T>`**: Wrapper chuẩn hóa định dạng trả về bao gồm `code`, `message`, và `result`.

---

## 3. Các API Endpoints
Tất cả các API dành cho khách hàng đều yêu cầu Header **`X-User-Id`** (được đẩy xuống từ `api-gateway` sau khi phân giải token). Base path của service là: `/api/v1/orders`.

### Dành cho Người dùng (Client)
1. **[POST] `/api/v1/orders/checkout`**
   - Chức năng: Lưu đơn hàng mới.
   - Header bắt buộc: `X-User-Id`
   - Body: `CheckoutRequest`

2. **[GET] `/api/v1/orders`**
   - Chức năng: Lấy danh sách các đơn hàng của user đang đăng nhập.
   - Header bắt buộc: `X-User-Id`

3. **[GET] `/api/v1/orders/{orderId}`**
   - Chức năng: Lấy chi tiết một đơn đặt hàng qua UUID.
   - Header bắt buộc: `X-User-Id`

4. **[POST] `/api/v1/orders/{orderId}/cancel`**
   - Chức năng: Hủy đơn hàng (Chỉ khách hàng sở hữu mới được hủy).
   - Header bắt buộc: `X-User-Id`

### Dành cho Quản trị viên (Admin)
5. **[GET] `/api/v1/orders/admin/all`**
   - Chức năng: Lọc danh sách (Phân trang giới hạn size 100/lần). Dùng Param `?page=0&size=20`.

6. Các API chuyển đổi trạng thái (Method `POST`):
   - `/admin/{orderId}/confirm` -> Cập nhật thành `CONFIRMED`
   - `/admin/{orderId}/process` -> Cập nhật thành `PROCESSING`
   - `/admin/{orderId}/ship` -> Cập nhật thành `SHIPPED`
   - `/admin/{orderId}/deliver` -> Cập nhật thành `DELIVERED`

---

## 4. Sự phụ thuộc vào các Service Khác (Dependencies)

`bookstore-order-service` **không hoạt động biệt lập hoàn toàn**. Nó phụ thuộc thiết yếu vào các service sau để có thể khởi tạo được một Order hoàn chỉnh.

### A. Phụ thuộc vào `bookstore-book-service`
Tại API tạo đơn hàng (`checkout`), Service cần biết Sách đó tên gì, giá bao nhiêu để cộng tổng tiền.
- **Phương thức giao tiếp**: Đồng bộ qua `Spring Cloud OpenFeign` (xem `BookClient.java`).
- **Cấu hình môi trường**: URL của book-service được đọc từ biến môi trường `${BOOK_SERVICE_URL:http://localhost:8082}`.
- **API Cần từ Book Service**:
  - `GET /api/books/{id}`: Đầu vào là một chuỗi ID sách, đầu ra phải là dữ liệu JSON chứa giá tiền (price) và thông tin giảm giá hoặc các chi tiết khác để tính toán.

### B. Phụ thuộc vào `bookstore-api-gateway`
- Vì lý do bảo mật và theo chuẩn Microservice, `order-service` đã **xóa bỏ hoàn toàn lớp SecurityConfig**.
- Nó ủy thác toàn bộ trách nhiệm xác thực/giải mã JWT cho API Gateway.
- Vì thế API Gateway khi định tuyến yêu cầu xuống `order-service` **bắt buộc phải gắn kèm Header `X-User-Id`**. Nếu không, service không có cơ sở để định danh ai là người mua.

---

## 5. Hướng dẫn Chạy và Kiểm thử (Cấu hình)

### A. Công cụ tích hợp Swagger (OpenAPI)
Hệ thống đã tích hợp sẵn công cụ tự động sinh tài liệu Swagger theo version 2.8.6.
- Swagger tự động gắn thêm ô Request Header `X-User-Id` cho tính năng thử nghiệm.
- **Link truy cập (khi debug)**: `http://localhost:8084/swagger-ui/index.html`

### B. Chạy Service Môi trường Development
1. **Khởi động Database**:
   - Order Service sử dụng MySQL làm Data Store cho mình trên Port mapped `3309`.
   - Lệnh chạy:
     ```bash
     cd bookstore-order-service
     docker network create bookstore-network # Nếu mạng này chưa tồn tại
     docker-compose -f docker-compose.dev.yml up -d order-db
     ```

2. **Khởi động Ứng dụng**:
   - Ứng dụng yêu cầu profile `dev` để nạp các config như `HikariCP params`, OpenFeign timeout.
   - Chạy lệnh CLI (Hoặc cài profile `dev` thẳng vào IDE IntelliJ):
     ```bash
     ./mvnw clean compile spring-boot:run -Dspring-boot.run.profiles=dev
     ```

3. **Cấu hình Properties hiện hành**:
   - Chạy tại Port nội bộ IDE: `8084`
   - Cấu hình Timeout cho OpenFeign gọi sang service khác: `Connect(5s), Read(10s)`.
   - Kết nối DB an toàn Pool `HikariCP`: Max pool: 10, Min idle: 5.
