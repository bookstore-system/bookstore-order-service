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
- **`OrderStatus` (Enum)**: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `COMPLETED`, `CANCELLED`, `RETURNED`.

### Luồng trạng thái Order

```
                  ┌─── payment.failed ────► CANCELLED
                  │
PENDING ──► CONFIRMED ──► PROCESSING ──► SHIPPED ──► DELIVERED ──► COMPLETED
  │                                                                    │
  ├─── user cancel ───► CANCELLED                                       │
  └─── admin cancel ──► CANCELLED  ◄────── admin cancel ────────────────┘
                            │
                            └── publish order.cancelled event
```

`payment.completed` đẩy Order `PENDING → CONFIRMED` rồi publish `order.placed`. `payment.failed` đẩy `PENDING → CANCELLED` (không publish vì đã được Payment Service kích hoạt event chain).

### Các DTO (Data Transfer Object)
- **`CheckoutRequest`**: Payload dùng để người dùng tạo đơn. Chứa `addressId`, `paymentMethod`, `bookIds` (danh sách ID cuốn sách muốn mua), `discountCode`, v.v.
- **`OrderResponse` / `OrderItemResponse`**: DTO trả về thông tin sạch sẽ, không chứa các thuộc tính lộ cấu trúc Entity nội bộ.
- **`ApiResponse<T>`**: Wrapper chuẩn hóa định dạng trả về bao gồm `code`, `message`, và `result`.

---

## 3. Các API Endpoints
Tất cả các API dành cho khách hàng đều yêu cầu Header **`X-User-Id`** (được đẩy xuống từ `api-gateway` sau khi phân giải token). Base path của service là: `/api/v1/orders`.

### Dành cho Người dùng (Client)
1. **[POST] `/api/v1/orders/checkout`**
   - Chức năng: Lưu đơn hàng mới (COD).
   - Header bắt buộc: `X-User-Id`
   - Body: `CheckoutRequest`

2. **[POST] `/api/v1/orders/checkout/vnpay`**
   - Chức năng: Tạo đơn hàng và yêu cầu thanh toán VNPay.
   - Header bắt buộc: `X-User-Id`
   - Body: `CheckoutRequest`

3. **[POST] `/api/v1/orders/checkout/zalopay`**
   - Chức năng: Tạo đơn hàng và yêu cầu thanh toán ZaloPay.
   - Header bắt buộc: `X-User-Id`
   - Body: `CheckoutRequest`

4. **[POST] `/api/v1/orders/checkout/momo`**
   - Chức năng: Tạo đơn hàng và yêu cầu thanh toán MoMo.
   - Header bắt buộc: `X-User-Id`
   - Body: `CheckoutRequest`

5. **[GET] `/api/v1/orders`**
   - Chức năng: Lấy danh sách các đơn hàng của user đang đăng nhập.
   - Header bắt buộc: `X-User-Id`

6. **[GET] `/api/v1/orders/{orderId}`**
   - Chức năng: Lấy chi tiết một đơn đặt hàng qua UUID.
   - Header bắt buộc: `X-User-Id`

7. **[POST] `/api/v1/orders/{orderId}/cancel`**
   - Chức năng: Hủy đơn hàng (Chỉ khách hàng sở hữu mới được hủy).
   - Header bắt buộc: `X-User-Id`

8. **[GET] `/api/v1/orders/count`**
   - Chức năng: Đếm số lượng đơn hàng của user đang đăng nhập.
   - Header bắt buộc: `X-User-Id`

### Dành cho Quản trị viên (Admin)
5. **[GET] `/api/v1/orders/admin/all`**
   - Chức năng: Lọc danh sách (Phân trang giới hạn size 100/lần). Dùng Param `?page=0&size=20`.

6. **[PUT] `/api/v1/orders/admin/{orderId}/status`**
   - Chức năng: Cập nhật trạng thái đơn hàng qua query param `?status=CONFIRMED`.

7. Các API chuyển đổi trạng thái (Method `POST`):
   - `/admin/{orderId}/confirm` -> Cập nhật thành `CONFIRMED`
   - `/admin/{orderId}/process` -> Cập nhật thành `PROCESSING`
   - `/admin/{orderId}/ship` -> Cập nhật thành `SHIPPED`
   - `/admin/{orderId}/deliver` -> Cập nhật thành `DELIVERED`

8. **[GET] `/api/v1/orders/admin/status/{status}`**
   - Chức năng: Lọc đơn hàng theo trạng thái. Hỗ trợ `startDate` và `endDate` (yyyy-MM-dd).

9. **[GET] `/api/v1/orders/admin/revenue`**
   - Chức năng: Lấy tổng doanh thu. Hỗ trợ `startDate` và `endDate` (yyyy-MM-dd).

### API Thống kê & Hỗ trợ Service khác
10. **[GET] `/api/v1/orders/stats`**
   - Chức năng: Thống kê tổng quan (doanh thu, tổng đơn hàng, v.v.). Hỗ trợ filter theo `startDate` và `endDate`.

11. **[GET] `/api/v1/orders/admin/stats`**
   - Chức năng: API cung cấp số liệu mở rộng (Pending, Completed, Cancelled orders) để phục vụ việc generate báo cáo từ AI Service.

12. **[GET] `/api/v1/orders/top-spenders` & `/api/v1/orders/top-buyers`**
   - Chức năng: Truy xuất danh sách những khách hàng chi tiêu nhiều nhất hoặc order nhiều nhất.

13. **[GET] `/api/v1/orders/users/{userId}/summary`**
    - Chức năng: Tóm lược thông tin mua hàng (số đơn, số tiền, ngày tạo cuối) của cá nhân một user.

14. **[GET] `/api/v1/orders/check-purchased?userId=...&bookId=...`**
    - Chức năng: Giao tiếp với **Review Service**. Trả về cờ `isPurchased: true` nếu user đã chọn mua và nhận sách thành công để Review Service kiểm tra quyền đánh giá (chống review rác).

---

## 4. Sự phụ thuộc vào các Service Khác (Dependencies)

`bookstore-order-service` **không hoạt động biệt lập hoàn toàn**. Nó phụ thuộc thiết yếu vào các service sau để có thể khởi tạo được một Order hoàn chỉnh.

### A. Phụ thuộc vào `bookstore-book-service`
Tại API tạo đơn hàng (`checkout`), Service cần biết Sách đó tên gì, giá bao nhiêu, tồn kho để cộng tổng tiền.
- **Phương thức giao tiếp**: Đồng bộ qua `Spring Cloud OpenFeign` (xem `BookClient.java`).
- **Cấu hình môi trường**: URL của book-service được đọc từ `${BOOK_SERVICE_URL:http://localhost:8082}`.
- **API Cần từ Book Service**:
   - `POST /api/v1/books/batch-details`: Lấy giá, salePrice, title, stockQuantity cho danh sách bookIds.
   - `POST /api/v1/books/reduce-stock`: Trừ tồn kho sau khi tạo đơn.

### B. Phụ thuộc vào `bookstore-user-service`
- **Phương thức giao tiếp**: OpenFeign (xem `UserClient.java`).
- **Cấu hình môi trường**: `${USER_SERVICE_URL:http://localhost:8081}`.
- **API Cần từ User Service**:
   - `GET /api/v1/users/{userId}/addresses/{addressId}`: Lấy thông tin giao hàng.

### C. Phụ thuộc vào `bookstore-promotion-service`
- **Phương thức giao tiếp**: OpenFeign (xem `PromotionClient.java`).
- **Cấu hình môi trường**: `${PROMOTION_SERVICE_URL:http://localhost:8086}`.
- **API Cần từ Promotion Service**:
   - `POST /api/v1/promotions/apply`: Kiểm tra mã giảm giá và tính discount.

### D. Phụ thuộc vào `bookstore-payment-service`
- **Phương thức giao tiếp**: OpenFeign (sync) + RabbitMQ (async).
- **Cấu hình môi trường**: `${PAYMENT_SERVICE_URL:http://localhost:8085}`.
- **Sync — API Cần từ Payment Service**:
   - `POST /api/v1/payment/vnpay/create`: Tạo URL thanh toán VNPay.
   - `POST /api/v1/payment/zalopay/create`: Tạo giao dịch ZaloPay.
   - `POST /api/v1/payment/momo/create`: Tạo URL thanh toán MoMo.
- **Async — Consume events từ `payment.exchange`** (`PaymentEventConsumer`):
   - `payment.completed` → set Order `CONFIRMED` + publish `order.placed`.
   - `payment.failed` → set Order `CANCELLED`.

### F. Outbound Events (RabbitMQ Producer)
Exchange: `order.exchange` (topic). Routing keys:
| Routing key | Trigger | Consumer |
|---|---|---|
| `order.placed` | Order chuyển `PENDING → CONFIRMED` (sau khi payment completed event nhận được) | Notification Service |
| `order.cancelled` | `cancelOrder` (user) **hoặc** `updateOrderStatus` admin chuyển sang `CANCELLED` | Notification Service |

Payload `OrderPlacedEvent`:
```json
{
  "orderId": "uuid",
  "userId": "string",
  "totalAmount": 150000.0,
  "paymentMethod": "VNPAY",
  "createdAt": "2026-05-16T10:00:00"
}
```

### E. Phụ thuộc vào `bookstore-api-gateway`
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

---

## 6. Unit Tests

Service có bộ unit tests hoàn chỉnh. **Không cần** chạy DB, RabbitMQ hay service khác — toàn bộ dependency được mock bằng Mockito.

### Các file test

| File | Số test | Phạm vi |
|---|---|---|
| `service/impl/OrderServiceImplTest.java` | 32 | Business logic: createOrder (14 cases), cancelOrder (6), updateStatus, stats, messaging |
| `controller/OrderControllerTest.java` | 22 | REST layer: auth guard 401, response codes, error handling |
| `messaging/PaymentEventConsumerTest.java` | 4 | RabbitMQ consumer: COMPLETED→CONFIRMED, FAILED→CANCELLED, unknown, exception swallow |
| `messaging/OrderEventProducerTest.java` | 2 | RabbitMQ producer: routing key đúng exchange/queue |
| **Tổng** | **60** | |

### Chạy test

```powershell
cd bookstore-order-service

# Chạy toàn bộ unit tests (bỏ qua contextLoads vì cần DB thật)
mvn test -Dtest="OrderServiceImplTest,OrderControllerTest,PaymentEventConsumerTest,OrderEventProducerTest"

# Chạy từng file riêng
mvn test -Dtest=OrderServiceImplTest
mvn test -Dtest=OrderControllerTest

# Chạy 1 method cụ thể
mvn test -Dtest="OrderServiceImplTest#createOrder_happyPath_noDiscount_returnsOrderResponse"
```

### Lưu ý kỹ thuật — SB4 Breaking Changes trong Test

- **`@WebMvcTest` và `@MockBean` bị xóa** trong Spring Boot 4 (package `org.springframework.boot.test.autoconfigure.web.servlet` và `org.springframework.boot.test.mock.mockito` không còn tồn tại).
- Controller test dùng `MockMvcBuilders.standaloneSetup()` thay thế:
  ```java
  mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService, paymentClient))
          .setControllerAdvice(new GlobalExceptionHandler())
          .build();
  ```
- `@Mock` từ Mockito thay `@MockBean` — không cần Spring context.
- Jackson serialize `boolean isPurchased` getter `isPurchased()` → JSON key là `"purchased"` (strip prefix `is`). Test phải check `$.purchased` không phải `$.isPurchased`.
- `.hasMessageContaining()` của AssertJ là **case-sensitive** — tránh so sánh tiếng Việt có dấu.

---

## 7. Changelog

### 2026-05-16 — Phase 1 fixes
- **M1** `cancelOrder` publish `order.cancelled` event (qua `OrderEventProducer.publishOrderCancelled`).
- **M1** `updateOrderStatus` admin → CANCELLED cũng publish `order.cancelled` (chỉ khi previous status != CANCELLED).
- README cập nhật endpoint payment-service prefix `/api/v1/payment/*` (trước ghi sai `/api/payment/*`).
- README bổ sung section Outbound Events + sơ đồ trạng thái Order.
