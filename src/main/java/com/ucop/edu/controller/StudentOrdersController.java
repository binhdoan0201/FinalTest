package com.ucop.edu.controller;

import com.ucop.edu.entity.Order;
import com.ucop.edu.entity.OrderItem;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.hibernate.Session;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class StudentOrdersController {

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Long> colOrderId;
    @FXML private TableColumn<Order, Object> colCreated;
    @FXML private TableColumn<Order, BigDecimal> colTotal;
    @FXML private TableColumn<Order, String> colStatus;

    private final ObservableList<Order> data = FXCollections.observableArrayList();
    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML
    private void initialize() {

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // ✅ Format ngày tạo + căn giữa
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        colCreated.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText("");
                    return;
                }

                if (item instanceof LocalDateTime dt) {
                    setText(fmt.format(dt));
                } else {
                    setText(item.toString());
                }

                setAlignment(Pos.CENTER);
            }
        });

        // ✅ Format tiền + căn phải
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : (vnd.format(item) + " VNĐ"));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });

        // ✅ Canh giữa các cột còn lại
        colOrderId.setStyle("-fx-alignment: CENTER;");
        colStatus.setStyle("-fx-alignment: CENTER;");


        orderTable.setItems(data);
        reload();
    }


    private void bindColumnWidths() {
        // trừ chút cho border/scrollbar
        var w = orderTable.widthProperty().subtract(30);

        colOrderId.prefWidthProperty().bind(w.multiply(0.10)); // 10%
        colCreated.prefWidthProperty().bind(w.multiply(0.35)); // 35%
        colTotal.prefWidthProperty().bind(w.multiply(0.22));   // 22%
        colStatus.prefWidthProperty().bind(w.multiply(0.18));  // 18%
  
    }

    @FXML
    public void reload() {
        data.clear();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long sid = CurrentUser.getCurrentAccount().getId();

            List<Order> orders = session.createQuery(
                            "FROM Order o WHERE o.student.id = :sid ORDER BY o.createdAt DESC",
                            Order.class
                    )
                    .setParameter("sid", sid)
                    .list();

            data.addAll(orders);
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", "Không tải được đơn hàng: " + e.getMessage());
        }
    }

    // ✅ NÚT "↻ Refresh" trong FXML gọi đúng hàm này
    @FXML
    private void reloadOrders() {
        reload();
    }

    // ✅ NÚT "Xem chi tiết" trong FXML gọi đúng hàm này
    @FXML
    private void viewSelectedOrder() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Chưa chọn đơn", "Bạn hãy click chọn 1 dòng trước nhé.");
            return;
        }
        showDetail(selected);
    }

    private void showDetail(Order order) {
        if (order == null) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Order o = session.get(Order.class, order.getId());

            StringBuilder sb = new StringBuilder();
            sb.append("Order #").append(o.getId())
              .append("\nStatus: ").append(o.getStatus())
              .append("\nTotal: ").append(vnd.format(o.getTotalAmount())).append(" VNĐ\n\n");

            for (OrderItem oi : o.getItems()) {
                String name = (oi.getCourse() != null ? oi.getCourse().getName() : "Course");
                int q = (oi.getQuantity() == null ? 0 : oi.getQuantity());
                sb.append("- ").append(name).append(" | SL: ").append(q).append("\n");
            }

            alert(Alert.AlertType.INFORMATION, "Chi tiết đơn hàng", sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", "Không xem được chi tiết: " + e.getMessage());
        }
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }
}
