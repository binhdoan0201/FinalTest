package com.ucop.edu.controller;

import com.ucop.edu.entity.*;
import com.ucop.edu.service.impl.OrderService;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

public class StudentDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label cartCountLabel;
    @FXML private Label cartTotalLabel;

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, Long> colCourseId;
    @FXML private TableColumn<Course, String> colCourseName;
    @FXML private TableColumn<Course, BigDecimal> colCourseFee;
    @FXML private TableColumn<Course, String> colCourseStatus;

    private final ObservableList<Course> courseData = FXCollections.observableArrayList();
    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML
    private void initialize() {
        if (CurrentUser.getCurrentAccount() != null) {
            welcomeLabel.setText("Xin chào, " + CurrentUser.getCurrentAccount().getUsername());
        } else {
            welcomeLabel.setText("Xin chào");
        }

        colCourseId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCourseName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCourseFee.setCellValueFactory(new PropertyValueFactory<>("tuitionFee"));
        colCourseStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // format học phí
        colCourseFee.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : (vnd.format(item) + " VNĐ"));
            }
        });

        courseTable.setItems(courseData);

        // cột nút "Thêm vào giỏ"
        TableColumn<Course, Void> actionCol = new TableColumn<>("Thao tác");
        actionCol.setPrefWidth(160);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Thêm vào giỏ");
            {
                btn.setOnAction(e -> {
                    Course c = getTableView().getItems().get(getIndex());
                    addToCart(c);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        // tránh add trùng cột khi reload controller
        if (courseTable.getColumns().stream().noneMatch(c -> "Thao tác".equals(c.getText()))) {
            courseTable.getColumns().add(actionCol);
        }

        loadCoursesFromDb();
        refreshCartSummary();
    }

    // ===== COURSES =====
    @FXML
    private void showCourses() {
        loadCoursesFromDb();
    }

    private void loadCoursesFromDb() {
        courseData.clear();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Course> list = session.createQuery("FROM Course", Course.class).list();
            courseData.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
            error("Lỗi", "Không thể tải danh sách khóa học.");
        }
    }

    // ===== CART =====
    private void addToCart(Course course) {
        if (course == null || course.getId() == null) return;

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Account student = session.get(Account.class, CurrentUser.getCurrentAccount().getId());

            Cart cart = session.createQuery(
                    "FROM Cart c WHERE c.student.id = :sid", Cart.class)
                .setParameter("sid", student.getId())
                .uniqueResult();

            if (cart == null) {
                cart = new Cart();
                cart.setStudent(student);
                cart.setCreatedAt(LocalDateTime.now());
                session.persist(cart);
            }

            CartItem existing = session.createQuery(
                    "FROM CartItem ci WHERE ci.cart.id = :cid AND ci.course.id = :courseId",
                    CartItem.class)
                .setParameter("cid", cart.getId())
                .setParameter("courseId", course.getId())
                .uniqueResult();

            if (existing != null) {
                int q = existing.getQuantity() == null ? 0 : existing.getQuantity();
                existing.setQuantity(q + 1);
                session.merge(existing);
            } else {
                CartItem item = new CartItem();
                item.setCart(cart);
                item.setCourse(session.get(Course.class, course.getId()));
                item.setQuantity(1);
                item.setPriceAtAdd(course.getTuitionFee()); // ✅
                session.persist(item);
            }

            tx.commit();

            refreshCartSummary();
            info("Đã thêm vào giỏ", "Đã thêm khóa học vào giỏ hàng.");

        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
            error("Lỗi", "Không thể thêm vào giỏ.");
        }
    }

    private void refreshCartSummary() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long sid = CurrentUser.getCurrentAccount().getId();

            Cart cart = session.createQuery(
                    "FROM Cart c WHERE c.student.id = :sid", Cart.class)
                .setParameter("sid", sid)
                .uniqueResult();

            if (cart == null) {
                cartCountLabel.setText("0");
                cartTotalLabel.setText("0 VNĐ");
                return;
            }

            List<CartItem> items = session.createQuery(
                    "FROM CartItem ci WHERE ci.cart.id = :cid", CartItem.class)
                .setParameter("cid", cart.getId())
                .list();

            int count = 0;
            BigDecimal total = BigDecimal.ZERO;

            for (CartItem ci : items) {
                int q = ci.getQuantity() == null ? 0 : ci.getQuantity();
                count += q;

                BigDecimal price = ci.getPriceAtAdd() == null ? BigDecimal.ZERO : ci.getPriceAtAdd();
                total = total.add(price.multiply(BigDecimal.valueOf(q)));
            }

            cartCountLabel.setText(String.valueOf(count));
            cartTotalLabel.setText(vnd.format(total) + " VNĐ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showCart() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long sid = CurrentUser.getCurrentAccount().getId();

            Cart cart = session.createQuery(
                    "FROM Cart c WHERE c.student.id = :sid", Cart.class)
                .setParameter("sid", sid)
                .uniqueResult();

            if (cart == null) {
                info("Giỏ hàng", "Giỏ hàng đang trống.");
                return;
            }

            List<CartItem> items = session.createQuery(
                    "FROM CartItem ci WHERE ci.cart.id = :cid", CartItem.class)
                .setParameter("cid", cart.getId())
                .list();

            if (items.isEmpty()) {
                info("Giỏ hàng", "Giỏ hàng đang trống.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            BigDecimal total = BigDecimal.ZERO;

            for (CartItem ci : items) {
                Course c = ci.getCourse();
                String name = (c != null ? c.getName() : "Course");
                int q = ci.getQuantity() == null ? 0 : ci.getQuantity();
                BigDecimal price = ci.getPriceAtAdd() == null ? BigDecimal.ZERO : ci.getPriceAtAdd();
                BigDecimal line = price.multiply(BigDecimal.valueOf(q));
                total = total.add(line);

                sb.append("- ").append(name)
                  .append(" | SL: ").append(q)
                  .append(" | ").append(vnd.format(line)).append(" VNĐ\n");
            }

            sb.append("\nTỔNG: ").append(vnd.format(total)).append(" VNĐ");

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Giỏ hàng");
            a.setHeaderText("Danh sách giỏ hàng");
            a.setContentText(sb.toString());
            a.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            error("Lỗi", "Không thể xem giỏ hàng.");
        }
    }

    // ===== CHECKOUT -> ORDER + ORDERITEM =====
    @FXML
    private void checkout() {
        try {
            Long studentId = CurrentUser.getCurrentAccount().getId();

            // ✅ tránh lỗi do import sai: nhớ import đúng com.ucop.edu.service.impl.OrderService
            OrderService orderService = new OrderService();
            Order order = orderService.checkout(studentId);

            info("Thành công",
                    "Đặt hàng thành công!\n" +
                    "Mã đơn: " + order.getId() + "\n" +
                    "Tổng tiền: " + vnd.format(order.getTotalAmount()) + " VNĐ");

            // ✅ checkout xong refresh lại summary
            refreshCartSummary();

        } catch (Exception e) {
            e.printStackTrace();
            error("Lỗi", e.getMessage());
        }
    }

    // ===== SHOW ORDERS (SV3) =====
    @FXML
    private void showOrders() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long sid = CurrentUser.getCurrentAccount().getId();

            List<Order> orders = session.createQuery(
                    "FROM Order o WHERE o.student.id = :sid ORDER BY o.createdAt DESC", Order.class)
                .setParameter("sid", sid)
                .list();

            if (orders == null || orders.isEmpty()) {
                info("Đơn hàng", "Bạn chưa có đơn hàng nào.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (Order o : orders) {
                sb.append("• Order #").append(o.getId())
                  .append(" | ").append(o.getStatus())
                  .append(" | ").append(o.getCreatedAt())
                  .append(" | ").append(vnd.format(o.getTotalAmount())).append(" VNĐ")
                  .append("\n");
            }

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Đơn hàng của tôi");
            a.setHeaderText("Danh sách đơn hàng");
            a.setContentText(sb.toString());
            a.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            error("Lỗi", "Không thể tải đơn hàng.");
        }
    }

    // ===== LOGOUT =====
    @FXML
    private void handleLogout() {
        try {
            CurrentUser.setCurrentAccount(null);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            stage.setScene(new Scene(root, 1200, 800));
            stage.setTitle("UCOP Education - Login");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            error("Lỗi", "Không thể logout: " + e.getMessage());
        }
    }

    // ===== placeholders nếu FXML có gọi =====
    @FXML
    private void checkoutDummy() { }

    // ===== UI helpers =====
    private void info(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void error(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
