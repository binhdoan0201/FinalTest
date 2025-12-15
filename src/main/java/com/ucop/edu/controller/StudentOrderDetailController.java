package com.ucop.edu.controller;

import com.ucop.edu.entity.Order;
import com.ucop.edu.entity.OrderItem;
import com.ucop.edu.util.HibernateUtil;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.hibernate.Session;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class StudentOrderDetailController {

    public static class ItemRow {
        private final StringProperty imageUrl = new SimpleStringProperty();
        private final StringProperty courseName = new SimpleStringProperty();
        private final IntegerProperty quantity = new SimpleIntegerProperty();
        private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>();
        private final ObjectProperty<BigDecimal> lineTotal = new SimpleObjectProperty<>();

        public ItemRow(String imageUrl, String courseName, int quantity, BigDecimal price) {
            this.imageUrl.set(imageUrl);
            this.courseName.set(courseName);
            this.quantity.set(quantity);
            this.price.set(price);
            this.lineTotal.set(price.multiply(BigDecimal.valueOf(quantity)));
        }

        public String getImageUrl() { return imageUrl.get(); }
        public String getCourseName() { return courseName.get(); }
        public int getQuantity() { return quantity.get(); }
        public BigDecimal getPrice() { return price.get(); }
        public BigDecimal getLineTotal() { return lineTotal.get(); }
    }

    @FXML private Label titleLabel;
    @FXML private Label subLabel;
    @FXML private Label totalLabel;

    @FXML private TableView<ItemRow> itemTable;
    @FXML private TableColumn<ItemRow, String> colImg;
    @FXML private TableColumn<ItemRow, String> colName;
    @FXML private TableColumn<ItemRow, Integer> colQty;
    @FXML private TableColumn<ItemRow, BigDecimal> colPrice;
    @FXML private TableColumn<ItemRow, BigDecimal> colLine;

    private final ObservableList<ItemRow> data = FXCollections.observableArrayList();
    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @FXML
    private void initialize() {
        // Ảnh
        colImg.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getImageUrl()));
        colImg.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(80);
                iv.setFitHeight(55);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                setAlignment(Pos.CENTER);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                if (empty || url == null || url.isBlank()) { setGraphic(null); return; }
                try {
                    iv.setImage(loadImage(url));
                    setGraphic(iv);
                } catch (Exception e) {
                    setGraphic(null);
                }
            }
        });

        colName.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseName()));
        colQty.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getQuantity()).asObject());
        colPrice.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getPrice()));
        colLine.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getLineTotal()));

        colQty.setStyle("-fx-alignment: CENTER;");
        colPrice.setStyle("-fx-alignment: CENTER-RIGHT;");
        colLine.setStyle("-fx-alignment: CENTER-RIGHT;");

        colPrice.setCellFactory(c -> moneyCell(Pos.CENTER_RIGHT));
        colLine.setCellFactory(c -> moneyCell(Pos.CENTER_RIGHT));

        itemTable.setItems(data);
        itemTable.setFixedCellSize(70);
    }

    private TableCell<ItemRow, BigDecimal> moneyCell(Pos align) {
        return new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : (vnd.format(item) + " VNĐ"));
                setAlignment(align);
            }
        };
    }

    private Image loadImage(String url) {
        // hỗ trợ: "Java.png" hoặc "/images/Java.png" hoặc "http..."
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return new Image(url, true);
        }
        String path = url.startsWith("/") ? url : ("/images/" + url);
        return new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
    }

    // ✅ StudentOrdersController sẽ gọi hàm này
    public void setOrderId(Long orderId) {
        if (orderId == null) return;

        data.clear();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Order o = session.get(Order.class, orderId);

            titleLabel.setText("Chi tiết đơn hàng #" + o.getId());
            subLabel.setText("Trạng thái: " + o.getStatus());

            BigDecimal total = (o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount());
            totalLabel.setText(vnd.format(total) + " VNĐ");

            // load items
            for (OrderItem oi : o.getItems()) {
                String name = oi.getCourse() != null ? oi.getCourse().getName() : "Course";
                String img = (oi.getCourse() != null ? oi.getCourse().getImageUrl() : null);

                int q = oi.getQuantity() == null ? 0 : oi.getQuantity();
                BigDecimal price = oi.getPriceAtPurchase() != null ? oi.getPriceAtPurchase() : BigDecimal.ZERO;


                data.add(new ItemRow(img, name, q, price));
            }
        }
    }
}
