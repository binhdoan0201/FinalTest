package com.ucop.edu.controller;

import com.ucop.edu.entity.Cart;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.hibernate.Session;

import java.net.URL;

public class StudentDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private StackPane contentPane;

    // badge giỏ hàng
    @FXML private Label cartBadgeLabel;

    // để controller khác gọi điều hướng
    private static StudentDashboardController INSTANCE;

    // lưu orderId vừa checkout để Payment auto chọn đúng đơn
    private static Long TARGET_ORDER_ID;

    /** Controller khác gọi: checkout xong -> chuyển Payment và auto chọn đúng order */
    public static void openPaymentAfterCheckout(Long orderId) {
        TARGET_ORDER_ID = orderId;
        if (INSTANCE != null) {
            Platform.runLater(INSTANCE::showPayment);
        }
    }

    /** StudentPaymentController gọi 1 lần để lấy orderId mục tiêu */
    public static Long consumeTargetOrderId() {
        Long x = TARGET_ORDER_ID;
        TARGET_ORDER_ID = null;
        return x;
    }

    /** Controller khác gọi: refresh badge giỏ hàng */
    public static void requestCartBadgeRefresh() {
        if (INSTANCE != null) {
            Platform.runLater(INSTANCE::refreshCartBadge);
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

        refreshCartBadge();
        showCourses();
    }

    // ===== MENU NAV =====
    @FXML public void showCourses() { loadToCenter("/fxml/student-courses.fxml"); }
    @FXML public void showCart()    { loadToCenter("/fxml/student-cart.fxml"); }
    @FXML public void showOrders()  { loadToCenter("/fxml/student-orders.fxml"); }
    @FXML public void showPayment() { loadToCenter("/fxml/student-payment.fxml"); }
    @FXML public void showWallet()  { loadToCenter("/fxml/student-wallet.fxml"); }

    // ✅ NEW: Hoàn tiền / Hoàn đổi
    @FXML public void showReturnRequests() { loadToCenter("/fxml/student-return-request.fxml"); }

    @FXML
    public void handleLogout(ActionEvent event) {
        try {
            CurrentUser.setCurrentAccount(null);
            TARGET_ORDER_ID = null;
            INSTANCE = null;

            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UCOP - Login");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText(null);
            a.setContentText("Không thể quay về login.fxml\n" + e.getMessage());
            a.showAndWait();
        }
    }

    private void loadToCenter(String fxmlPath) {
        try {
            if (contentPane == null) throw new IllegalStateException("contentPane chưa bind (fx:id=contentPane).");

            URL url = getClass().getResource(fxmlPath);
            if (url == null) throw new IllegalStateException("Không tìm thấy FXML: " + fxmlPath);

            Parent view = FXMLLoader.load(url);
            contentPane.getChildren().setAll(view);

            // mỗi lần đổi view cũng refresh badge cho chắc
            refreshCartBadge();

        } catch (Exception e) {
            e.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setHeaderText(null);
            a.setContentText("Không thể load view: " + fxmlPath + "\n" + e.getMessage());
            a.showAndWait();
        }
    }

    private void refreshCartBadge() {
        try {
            if (cartBadgeLabel == null) return;

            Long sid = (CurrentUser.getCurrentAccount() == null) ? null : CurrentUser.getCurrentAccount().getId();
            if (sid == null) {
                cartBadgeLabel.setVisible(false);
                cartBadgeLabel.setManaged(false);
                cartBadgeLabel.setText("0");
                return;
            }

            try (Session s = HibernateUtil.getSessionFactory().openSession()) {
                Cart cart = s.createQuery("from Cart c where c.student.id = :sid", Cart.class)
                        .setParameter("sid", sid)
                        .uniqueResult();

                long count = 0;
                if (cart != null) {
                    Object rs = s.createQuery(
                            "select coalesce(sum(ci.quantity),0) from CartItem ci where ci.cart.id = :cid"
                    ).setParameter("cid", cart.getId())
                     .uniqueResult();

                    if (rs instanceof Number) count = ((Number) rs).longValue();
                }

                if (count > 0) {
                    cartBadgeLabel.setText(String.valueOf(count));
                    cartBadgeLabel.setVisible(true);
                    cartBadgeLabel.setManaged(true);
                } else {
                    cartBadgeLabel.setText("0");
                    cartBadgeLabel.setVisible(false);
                    cartBadgeLabel.setManaged(false);
                }
            }
        } catch (Exception ignore) {
            // badge lỗi không làm crash UI
        }
    }
}
