package com.ucop.edu.controller;

import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.hibernate.Session;

public class StudentDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private StackPane contentPane;
    @FXML private Label cartBadgeLabel;

    // ✅ để controller con gọi refresh badge (static)
    private static StudentDashboardController INSTANCE;

    /** Controller con gọi: StudentDashboardController.requestCartBadgeRefresh(); */
    public static void requestCartBadgeRefresh() {
        if (INSTANCE != null) {
            Platform.runLater(INSTANCE::refreshCartBadgeFromDb);
        }
    }

    @FXML
    private void initialize() {
        INSTANCE = this;

        if (CurrentUser.getCurrentAccount() != null) {
            welcomeLabel.setText("Xin chào, " + CurrentUser.getCurrentAccount().getUsername());
        } else {
            welcomeLabel.setText("Xin chào");
        }

        // ✅ init badge trước (để label không bị null/nhảy layout)
        setBadge(0);

        // ✅ load badge từ DB + mở trang mặc định
        refreshCartBadgeFromDb();
        showCourses();
    }

    // ✅ query DB để ra tổng số lượng trong cart
    private void refreshCartBadgeFromDb() {
        try {
            if (cartBadgeLabel == null) return;

            if (CurrentUser.getCurrentAccount() == null) {
                setBadge(0);
                return;
            }

            Long sid = CurrentUser.getCurrentAccount().getId();

            try (Session session = HibernateUtil.getSessionFactory().openSession()) {
                Long qty = session.createQuery(
                        "select coalesce(sum(ci.quantity), 0) " +
                        "from CartItem ci " +
                        "where ci.cart.student.id = :sid",
                        Long.class
                ).setParameter("sid", sid)
                 .uniqueResult();

                setBadge(qty == null ? 0 : qty.intValue());
            }

        } catch (Exception e) {
            // không crash UI nếu lỗi
            setBadge(0);
        }
    }

    private void setBadge(int count) {
        if (cartBadgeLabel == null) return;

        cartBadgeLabel.setText(String.valueOf(count));

        boolean show = count > 0;
        cartBadgeLabel.setVisible(show);  // ẩn/hiện
        cartBadgeLabel.setManaged(show);  // ẩn luôn khoảng trống layout
    }

    // ===== LOAD TRANG CON VÀO contentPane =====
    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent page = loader.load();

            contentPane.getChildren().setAll(page);

            // ✅ mỗi lần chuyển trang refresh badge cho chắc
            refreshCartBadgeFromDb();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Không load được trang: " + fxmlPath + "\n" + e.getMessage()).showAndWait();
        }
    }

    // ===== MENU ACTIONS =====
    @FXML
    private void showCourses() {
        loadPage("/fxml/student-courses.fxml");
    }

    @FXML
    private void showCart() {
        loadPage("/fxml/student-cart.fxml");
    }

    @FXML
    private void showOrders() {
        loadPage("/fxml/student-orders.fxml");
    }

    // ===== LOGOUT =====
    @FXML
    private void handleLogout() {
        try {
            CurrentUser.setCurrentAccount(null);
            INSTANCE = null; // ✅ tránh controller cũ bị gọi refresh nữa

            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();

            Scene scene = new Scene(root, 1200, 800);
            stage.setScene(scene);
            stage.setTitle("UCOP Education - Login");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Không thể logout: " + e.getMessage()).showAndWait();
        }
    }
}
