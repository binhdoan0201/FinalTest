package com.ucop.edu.controller;

import com.ucop.edu.entity.Order;
import com.ucop.edu.entity.OrderItem;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class StudentOrdersController {

    @FXML private TableView<Order> orderTable;
    @FXML private TableColumn<Order, Long> colOrderId;
    @FXML private TableColumn<Order, Object> colCreated;
    @FXML private TableColumn<Order, BigDecimal> colTotal;
    @FXML private TableColumn<Order, String> colStatus;
    @FXML private TableColumn<Order, Void> colRefund;

    private final ObservableList<Order> data = FXCollections.observableArrayList();
    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    private void initialize() {

        colOrderId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        // Format ngày tạo + căn giữa
        colCreated.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText("");
                    return;
                }

                if (item instanceof LocalDateTime dt) setText(fmt.format(dt));
                else setText(item.toString());

                setAlignment(Pos.CENTER);
            }
        });

        // Format tiền + căn phải
        colTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : (vnd.format(item) + " VNĐ"));
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        setupRefundColumn();
        // Canh giữa các cột còn lại
        colOrderId.setStyle("-fx-alignment: CENTER;");
        colStatus.setStyle("-fx-alignment: CENTER;");

        orderTable.setItems(data);

        // Cân cột theo %
        bindColumnWidths();

        reload();
    }

    private void bindColumnWidths() {
        var w = orderTable.widthProperty().subtract(28);
        colOrderId.prefWidthProperty().bind(w.multiply(0.12)); // 12%
        colCreated.prefWidthProperty().bind(w.multiply(0.38)); // 38%
        colTotal.prefWidthProperty().bind(w.multiply(0.28));   // 28%
        colStatus.prefWidthProperty().bind(w.multiply(0.22));  // 22%
        colRefund.prefWidthProperty().bind(w.multiply(0.18));
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

    // Nút "↻ Refresh"
    @FXML
    private void reloadOrders() {
        reload();
    }

    // Nút "Xem chi tiết"
    @FXML
    private void viewSelectedOrder() {
        Order selected = orderTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            alert(Alert.AlertType.WARNING, "Chưa chọn đơn", "Bạn hãy click chọn 1 dòng trước nhé.");
            return;
        }
        showDetailDialog(selected);
    }

    // =================== DETAIL DIALOG (ẢNH + GIÁ) ===================
    private void showDetailDialog(Order selected) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Order o = session.get(Order.class, selected.getId());
            if (o == null) {
                alert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy đơn hàng.");
                return;
            }

            // ép load items (LAZY)
            o.getItems().size();

            VBox root = new VBox(12);
            root.setPadding(new Insets(14));

            Label header = new Label(
                    "Order #" + o.getId() +
                    "\nTrạng thái: " + (o.getStatus() == null ? "" : o.getStatus()) +
                    "\nTổng tiền: " + vnd.format(nz(o.getTotalAmount())) + " VNĐ"
            );
            header.setStyle("-fx-font-weight: 800; -fx-text-fill: #0f172a;");

            VBox listBox = new VBox(10);

            if (o.getItems() == null || o.getItems().isEmpty()) {
                listBox.getChildren().add(new Label("Đơn hàng chưa có sản phẩm."));
            } else {
                for (OrderItem oi : o.getItems()) {
                    listBox.getChildren().add(buildOrderItemRow(oi));
                }
            }

            ScrollPane sp = new ScrollPane(listBox);
            sp.setFitToWidth(true);
            sp.setPrefViewportHeight(360);
            sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp.setStyle("-fx-background-color: transparent;");

            root.getChildren().addAll(header, sp);

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Chi tiết đơn hàng");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            dialog.getDialogPane().setContent(root);
            dialog.getDialogPane().setMinWidth(680);
            dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

            dialog.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", "Không xem được chi tiết: " + e.getMessage());
        }
    }

    private HBox buildOrderItemRow(OrderItem oi) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle(
                "-fx-background-color: #f8fafc;" +
                "-fx-border-color: #e2e8f0;" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 12;"
        );

        ImageView iv = new ImageView();
        iv.setFitWidth(86);
        iv.setFitHeight(58);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);

        String name = "Course";
        String imgUrl = null;

        if (oi.getCourse() != null) {
            if (oi.getCourse().getName() != null) name = oi.getCourse().getName();
            imgUrl = oi.getCourse().getImageUrl();
        }

        int qty = oi.getQuantity() == null ? 0 : oi.getQuantity();

        BigDecimal unitPrice = getUnitPrice(oi);
        BigDecimal lineTotal = nz(oi.getLineTotal());
        if (lineTotal.compareTo(BigDecimal.ZERO) == 0) {
            lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
        }

        // load ảnh
        try {
            if (imgUrl != null && !imgUrl.isBlank()) {
                Image img;
                if (imgUrl.startsWith("http://") || imgUrl.startsWith("https://")) {
                    img = new Image(imgUrl, true);
                } else {
                    String path = imgUrl.startsWith("/") ? imgUrl : ("/images/" + imgUrl);
                    img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
                }
                iv.setImage(img);
            }
        } catch (Exception ignore) {}

        VBox info = new VBox(4);

        Label nameLb = new Label(name);
        nameLb.setStyle("-fx-font-weight: 800; -fx-text-fill: #0f172a; -fx-font-size: 13;");

        Label priceLb = new Label("Giá: " + vnd.format(unitPrice) + " VNĐ");
        priceLb.setStyle("-fx-text-fill: #334155;");

        Label qtyLb = new Label("Số lượng: " + qty);
        qtyLb.setStyle("-fx-text-fill: #334155;");

        Label totalLb = new Label("Thành tiền: " + vnd.format(lineTotal) + " VNĐ");
        totalLb.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: 800;");

        info.getChildren().addAll(nameLb, priceLb, qtyLb, totalLb);

        row.getChildren().addAll(iv, info);
        return row;
    }

    // ✅ Lấy giá đúng theo entity OrderItem bạn đưa:
    // ưu tiên priceAtPurchase -> unitPrice -> course.tuitionFee -> 0
    private BigDecimal getUnitPrice(OrderItem oi) {
        if (oi == null) return BigDecimal.ZERO;

        BigDecimal p = nz(oi.getPriceAtPurchase());
        if (p.compareTo(BigDecimal.ZERO) > 0) return p;

        p = nz(oi.getUnitPrice());
        if (p.compareTo(BigDecimal.ZERO) > 0) return p;

        if (oi.getCourse() != null && oi.getCourse().getTuitionFee() != null) {
            return oi.getCourse().getTuitionFee();
        }

        return BigDecimal.ZERO;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        a.showAndWait();
    }
    
    private void setupRefundColumn() {
        colRefund.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("Refund");

            {
                btn.setStyle("-fx-background-color:#ef4444; -fx-text-fill:white; "
                        + "-fx-background-radius:10; -fx-padding:6 12; -fx-font-weight:800; "
                        + "-fx-cursor: hand;");
                btn.setOnAction(e -> {
                    Order order = getTableView().getItems().get(getIndex());
                    requestRefund(order);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                if (empty) {
                    setGraphic(null);
                    return;
                }

                Order o = getTableView().getItems().get(getIndex());

                // ✅ chỉ hiện nút khi PAID
                boolean isPaid = (o != null && o.getStatus() != null
                        && "PAID".equalsIgnoreCase(o.getStatus()));

                setGraphic(isPaid ? btn : null);
            }
        });

        // canh giữa cột nút (đẹp hơn)
        colRefund.setStyle("-fx-alignment: CENTER;");
    }

    private void requestRefund(Order selected) {
        if (selected == null) return;

        // chỉ cho refund khi PAID (double-check)
        if (selected.getStatus() == null || !"PAID".equalsIgnoreCase(selected.getStatus())) {
            alert(Alert.AlertType.WARNING, "Không hợp lệ", "Chỉ refund đơn đang PAID.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận refund");
        confirm.setHeaderText(null);
        confirm.setContentText("Gửi yêu cầu refund Order #" + selected.getId()
                + "?\nTrạng thái sẽ chuyển: PAID → REFUND_PENDING");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            var tx = session.beginTransaction();

            Long sid = CurrentUser.getCurrentAccount().getId();

            Order o = session.get(Order.class, selected.getId());
            if (o == null) {
                tx.rollback();
                alert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy đơn hàng.");
                return;
            }

            // đảm bảo là đơn của chính student
            if (o.getStudent() == null || o.getStudent().getId() == null || !o.getStudent().getId().equals(sid)) {
                tx.rollback();
                alert(Alert.AlertType.ERROR, "Lỗi", "Bạn không có quyền refund đơn này.");
                return;
            }

            // tránh bấm 2 lần / đổi trạng thái bởi nơi khác
            if (o.getStatus() == null || !"PAID".equalsIgnoreCase(o.getStatus())) {
                tx.rollback();
                alert(Alert.AlertType.WARNING, "Không hợp lệ", "Đơn không còn ở trạng thái PAID.");
                return;
            }

            o.setStatus("REFUND_PENDING");
            session.merge(o);

            tx.commit();

            alert(Alert.AlertType.INFORMATION, "Thành công", "✅ Đã gửi yêu cầu refund (REFUND_PENDING).");
            reload();

        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", "Refund thất bại: " + e.getMessage());
        }
    }
}
