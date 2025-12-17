package com.ucop.edu.controller;

import com.ucop.edu.entity.Cart;
import com.ucop.edu.entity.CartItem;
import com.ucop.edu.service.impl.OrderService;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class StudentCartController {

    public static class CartRow {
        private final LongProperty cartItemId = new SimpleLongProperty();
        private final StringProperty courseName = new SimpleStringProperty();
        private final StringProperty imageUrl = new SimpleStringProperty();
        private final IntegerProperty quantity = new SimpleIntegerProperty();
        private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>();
        private final ObjectProperty<BigDecimal> lineTotal = new SimpleObjectProperty<>();

        public CartRow(long cartItemId, String courseName, String imageUrl, int quantity, BigDecimal price) {
            this.cartItemId.set(cartItemId);
            this.courseName.set(courseName);
            this.imageUrl.set(imageUrl);
            this.quantity.set(quantity);
            this.price.set(price == null ? BigDecimal.ZERO : price);
            this.lineTotal.set(this.price.get().multiply(BigDecimal.valueOf(quantity)));
        }

        public long getCartItemId() { return cartItemId.get(); }
        public String getCourseName() { return courseName.get(); }
        public String getImageUrl() { return imageUrl.get(); }
        public int getQuantity() { return quantity.get(); }
        public BigDecimal getPrice() { return price.get(); }
        public BigDecimal getLineTotal() { return lineTotal.get(); }
    }

    @FXML private TableView<CartRow> cartTable;
    @FXML private TableColumn<CartRow, String> colImg;
    @FXML private TableColumn<CartRow, String> colCourse;
    @FXML private TableColumn<CartRow, Integer> colQty;
    @FXML private TableColumn<CartRow, BigDecimal> colPrice;
    @FXML private TableColumn<CartRow, BigDecimal> colLine;
    @FXML private TableColumn<CartRow, Void> colRemove;

    @FXML private Label totalLabel;

    private final ObservableList<CartRow> data = FXCollections.observableArrayList();
    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML
    private void initialize() {
        colImg.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));
        colImg.setCellFactory(col -> new TableCell<>() {
            private final javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView();
            {
                iv.setFitWidth(70);
                iv.setFitHeight(45);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
            }
            @Override protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                if (empty || url == null || url.isBlank()) { setGraphic(null); return; }
                try {
                    iv.setImage(loadImage(url));
                    setGraphic(iv);
                    setText(null);
                } catch (Exception e) {
                    setGraphic(null);
                }
            }
        });

        colCourse.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        colQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        colLine.setCellValueFactory(new PropertyValueFactory<>("lineTotal"));

        colPrice.setCellFactory(c -> moneyCell());
        colLine.setCellFactory(c -> moneyCell());

        colRemove.setCellFactory(c -> new TableCell<>() {
            private final Button btn = new Button("🗑");
            {
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    CartRow row = getTableView().getItems().get(getIndex());
                    removeCartItem(row.getCartItemId());
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        });

        cartTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        colImg.setPrefWidth(110);   colImg.setMinWidth(110);
        colCourse.setPrefWidth(420);colCourse.setMinWidth(420);
        colQty.setPrefWidth(80);    colQty.setMinWidth(80);
        colPrice.setPrefWidth(140); colPrice.setMinWidth(140);
        colLine.setPrefWidth(160);  colLine.setMinWidth(160);
        colRemove.setPrefWidth(70); colRemove.setMinWidth(70);

        colImg.setStyle("-fx-alignment: CENTER;");
        colQty.setStyle("-fx-alignment: CENTER;");
        colPrice.setStyle("-fx-alignment: CENTER-RIGHT;");
        colLine.setStyle("-fx-alignment: CENTER-RIGHT;");
        colRemove.setStyle("-fx-alignment: CENTER;");

        cartTable.setFixedCellSize(70);
        cartTable.setItems(data);

        reload();
    }

    private Image loadImage(String url) {
        if (url == null) throw new RuntimeException("Ảnh null");

        String u = url.trim().replace("\\", "/");

        if (u.startsWith("http://") || u.startsWith("https://") || u.startsWith("file:/")) {
            return new Image(u, true);
        }

        // "images/abc.jpg" -> "/images/abc.jpg"
        if (!u.startsWith("/")) u = "/" + u;

        var resUrl = getClass().getResource(u);
        if (resUrl != null) return new Image(resUrl.toExternalForm(), true);

        // fallback nếu db chỉ lưu tên file
        String fileNameOnly = u.substring(u.lastIndexOf("/") + 1);
        var resUrl2 = getClass().getResource("/images/" + fileNameOnly);
        if (resUrl2 != null) return new Image(resUrl2.toExternalForm(), true);

        throw new RuntimeException("Không load được ảnh: " + url);
    }



    private TableCell<CartRow, BigDecimal> moneyCell() {
        return new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : (vnd.format(item) + " VNĐ"));
            }
        };
    }

    @FXML
    public void reload() {
        data.clear();
        BigDecimal total = BigDecimal.ZERO;

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long sid = CurrentUser.getCurrentAccount().getId();

            Cart cart = session.createQuery("from Cart c where c.student.id = :sid", Cart.class)
                    .setParameter("sid", sid)
                    .uniqueResult();

            if (cart == null) { totalLabel.setText("0 VNĐ"); return; }

            List<CartItem> items = session.createQuery(
                            "from CartItem ci where ci.cart.id = :cid", CartItem.class)
                    .setParameter("cid", cart.getId())
                    .list();

            for (CartItem ci : items) {
                String name = (ci.getCourse() != null ? ci.getCourse().getName() : "Course");
                String img = (ci.getCourse() != null ? ci.getCourse().getImageUrl() : null);
                int q = ci.getQuantity() == null ? 0 : ci.getQuantity();
                BigDecimal price = ci.getPriceAtAdd() == null ? BigDecimal.ZERO : ci.getPriceAtAdd();

                if (q > 0) {
                    data.add(new CartRow(ci.getId(), name, img, q, price));
                    total = total.add(price.multiply(BigDecimal.valueOf(q)));
                }
            }

            totalLabel.setText(vnd.format(total) + " VNĐ");

        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", "Không tải được giỏ hàng: " + e.getMessage());
        }
    }

    private void removeCartItem(long cartItemId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            CartItem ci = session.get(CartItem.class, cartItemId);
            if (ci != null) session.remove(ci);

            tx.commit();

            StudentDashboardController.requestCartBadgeRefresh();
            reload();
        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) {}
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", "Không xóa được: " + e.getMessage());
        }
    }

    @FXML
    private void checkout() {
        try {
            Long sid = CurrentUser.getCurrentAccount().getId();

            OrderService orderService = new OrderService();
            var order = orderService.checkout(sid);

            // ✅ mở payment và auto chọn đúng order
            StudentDashboardController.openPaymentAfterCheckout(order.getId());

            // ✅ cập nhật badge + reload giỏ
            StudentDashboardController.requestCartBadgeRefresh();
            reload();

        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
        }
    }

    @FXML
    private void clearCart() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận");
        confirm.setHeaderText("Xóa tất cả khóa học trong giỏ?");
        confirm.setContentText("Hành động này không thể hoàn tác.");

        ButtonType btnYes = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo  = new ButtonType("Hủy", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        var result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != btnYes) return;

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Long sid = CurrentUser.getCurrentAccount().getId();

            tx = session.beginTransaction();

            int deleted = session.createQuery(
                    "delete from CartItem ci where ci.cart.student.id = :sid"
            ).setParameter("sid", sid).executeUpdate();

            tx.commit();

            alert(Alert.AlertType.INFORMATION, "OK", "Đã xóa " + deleted + " khóa học trong giỏ.");
            reload();
            StudentDashboardController.requestCartBadgeRefresh();

        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ignore) {}
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", "Không xóa được giỏ hàng: " + e.getMessage());
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
