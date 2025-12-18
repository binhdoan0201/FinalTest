package com.ucop.edu.controller;

import com.ucop.edu.entity.Order;
import com.ucop.edu.service.impl.DuyetDonStaffService;
import com.ucop.edu.util.CurrentUser;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class DuyetDonStaffController {

    @FXML private TableView<Order> tblOrders;
    @FXML private TableColumn<Order, Long> colId;
    @FXML private TableColumn<Order, String> colStudent;
    @FXML private TableColumn<Order, String> colCreatedAt;
    @FXML private TableColumn<Order, String> colTotal;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Void> colAction;
    @FXML private Label lblMsg;

    private final DuyetDonStaffService service = new DuyetDonStaffService();

    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        setupColumns();
        loadData();
    }

    private void setupColumns() {
        colId.setCellValueFactory(c ->
                new SimpleLongProperty(c.getValue().getId()).asObject()
        );

        colStudent.setCellValueFactory(c -> {
            var st = c.getValue().getStudent();
            String name = (st == null || st.getUsername() == null) ? "" : st.getUsername();
            return new SimpleStringProperty(name);
        });

        colCreatedAt.setCellValueFactory(c -> {
            var t = c.getValue().getCreatedAt();
            return new SimpleStringProperty(t == null ? "" : dtf.format(t));
        });

        colTotal.setCellValueFactory(c -> {
            var money = c.getValue().getTotalAmount();
            String txt = (money == null) ? "0" : vnd.format(money);
            return new SimpleStringProperty(txt);
        });

        colStatus.setCellValueFactory(c ->
                new SimpleStringProperty(
                        c.getValue().getStatus() == null ? "" : c.getValue().getStatus()
                )
        );

        // Cột Action: nút Duyệt mỗi dòng
        colAction.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("Duyệt");

            {
                btn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    handleApprove(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Order order = getTableView().getItems().get(getIndex());

                // chỉ cho duyệt khi còn PENDING
                boolean canApprove = order != null
                        && "PENDING".equalsIgnoreCase(order.getStatus());

                btn.setDisable(!canApprove);
                setGraphic(btn);
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void loadData() {
        try {
            lblMsg.setText("");

            List<Order> orders = service.getPendingOrders();
            tblOrders.setItems(FXCollections.observableArrayList(orders));

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("Không tải được danh sách PENDING!");
        }
    }

    private void handleApprove(Order order) {
        if (order == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận duyệt");
        confirm.setHeaderText(null);
        confirm.setContentText("Duyệt Order #" + order.getId() + " ?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            service.approveOrder(order.getId());
            lblMsg.setText("✅ Đã duyệt Order #" + order.getId());
            loadData(); // order vừa duyệt sẽ biến mất khỏi list PENDING

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("❌ Duyệt thất bại: " + e.getMessage());
        }
    }

    @FXML
    private void handleApproveAll() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận duyệt tất cả");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn muốn duyệt TẤT CẢ order đang PENDING thành SUCCESS?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            lblMsg.setText("");

            int updated = service.approveAllPending();
            lblMsg.setText("✅ Đã duyệt " + updated + " order.");

            loadData(); // refresh lại table (sẽ trống nếu duyệt hết)

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("❌ Duyệt tất cả thất bại: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Đăng xuất");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn chắc chắn muốn đăng xuất?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            // 1) Clear session user
            CurrentUser.logout(); // nếu bạn chưa có method này thì xem mục (3)

            // 2) Mở lại màn login
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml")); // đổi đúng path login của bạn
            Scene scene = new Scene(root);

            Stage stage = (Stage) tblOrders.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
            lblMsg.setText("❌ Logout lỗi: " + e.getMessage());
        }
    }
}
