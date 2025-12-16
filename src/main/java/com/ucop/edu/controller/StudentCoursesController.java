package com.ucop.edu.controller;

import com.ucop.edu.entity.Cart;
import com.ucop.edu.entity.CartItem;
import com.ucop.edu.entity.Course;
import com.ucop.edu.entity.Account;
import com.ucop.edu.util.CurrentUser;
import com.ucop.edu.util.HibernateUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StudentCoursesController {

    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, String> colImg;
    @FXML private TableColumn<Course, Long> colId;
    @FXML private TableColumn<Course, String> colName;
    @FXML private TableColumn<Course, BigDecimal> colFee;
    @FXML private TableColumn<Course, String> colStatus;
    @FXML private TableColumn<Course, Integer> colRegistered;
    @FXML private TableColumn<Course, Void> colAction;

    private final ObservableList<Course> data = FXCollections.observableArrayList();
    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    // map courseId -> số đã đăng ký (sum quantity trong order_items)
    private Map<Long, Integer> registeredMap = new HashMap<>();

    private static final Path BASE_DIR = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

    private String toImageUri(String storedPath) {
        if (storedPath == null) return null;
        String s = storedPath.trim();
        if (s.isEmpty()) return null;

        if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("file:")) return s;
        if (s.startsWith("/")) s = s.substring(1);

        Path p = Paths.get(s);
        if (!p.isAbsolute()) p = BASE_DIR.resolve(p).normalize();

        if (!Files.exists(p)) return null;
        return p.toUri().toString();
    }
    
    @FXML
    private void initialize() {

        // ====== 1) BẢNG: FIX layout để cột không lệch ======
        courseTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        courseTable.setFixedCellSize(70);

        // set width rõ ràng (cột nào bạn đang dùng thì chỉnh đúng fx:id)
        colImg.setPrefWidth(110);    colImg.setMinWidth(110);   colImg.setMaxWidth(110);
        colId.setPrefWidth(70);      colId.setMinWidth(70);     colId.setMaxWidth(70);
        colName.setPrefWidth(420);   colName.setMinWidth(420);
        colFee.setPrefWidth(160);    colFee.setMinWidth(160);
        colStatus.setPrefWidth(120); colStatus.setMinWidth(120);
        colRegistered.setPrefWidth(120); colRegistered.setMinWidth(120);
        colAction.setPrefWidth(80);  colAction.setMinWidth(80); colAction.setMaxWidth(80);

        // canh header + cell cho đều
        colImg.setStyle("-fx-alignment: CENTER;");
        colId.setStyle("-fx-alignment: CENTER;");
        colName.setStyle("-fx-alignment: CENTER-LEFT;");
        colFee.setStyle("-fx-alignment: CENTER-RIGHT;");
        colStatus.setStyle("-fx-alignment: CENTER;");
        colRegistered.setStyle("-fx-alignment: CENTER;");
        colAction.setStyle("-fx-alignment: CENTER;");

        // ====== 2) CELL VALUE FACTORY ======
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colFee.setCellValueFactory(new PropertyValueFactory<>("tuitionFee"));

        colStatus.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(
                cd.getValue().getStatus() == null ? "" : cd.getValue().getStatus().toString()
            )
        );

        // ====== 3) FORMAT TIỀN ======
        colFee.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : (vnd.format(item) + " VNĐ"));
                setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            }
        });

        // ====== 4) ẢNH ======
        colImg.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));
        colImg.setCellFactory(c -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(80);
                iv.setFitHeight(55);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
            }

            @Override protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                setText(null);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(javafx.geometry.Pos.CENTER);

                if (empty || url == null || url.isBlank()) { setGraphic(null); return; }

                try {
                    String uri = toImageUri(url);
                    if (uri == null) { setGraphic(null); return; }
                    iv.setImage(new Image(uri, 80, 55, true, true, true));
                    setGraphic(iv);
                } catch (Exception e) {
                    setGraphic(null);
                }
            }
        });

        // ====== 5) ĐÃ ĐĂNG KÝ ======
        colRegistered.setCellValueFactory(cd -> {
            Course c = cd.getValue();
            int reg = registeredMap.getOrDefault(c.getId(), 0);
            return new javafx.beans.property.SimpleIntegerProperty(reg).asObject();
        });

        // ====== 6) ACTION (ICON GIỎ) ======
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                btn.getStyleClass().add("btn-icon");
                btn.setMinSize(32, 32);
                btn.setPrefSize(32, 32);
                btn.setMaxSize(32, 32);
                btn.setTooltip(new Tooltip("Thêm vào giỏ"));

                try {
                    ImageView icon = new ImageView(new Image(
                            java.util.Objects.requireNonNull(getClass().getResourceAsStream("/static_images/cart.png"))
                    ));
                    icon.setFitWidth(18);
                    icon.setFitHeight(18);
                    icon.setPreserveRatio(true);
                    btn.setGraphic(icon);
                } catch (Exception e) {
                    btn.setText("🛒");
                }

                btn.setOnAction(e -> {
                    Course c = getTableView().getItems().get(getIndex());
                    addToCart(c);
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                setGraphic(empty ? null : btn);
            }
        });

        // ====== 7) DATA ======
        courseTable.setItems(data);
        reload();
    }


    @FXML
    public void reload() {
        data.clear();
        registeredMap = loadRegisteredMap();

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Course> list = session.createQuery("FROM Course", Course.class).list();
            data.addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", "Không tải được danh sách khóa học: " + e.getMessage());
        }

        courseTable.refresh();
    }

    private Map<Long, Integer> loadRegisteredMap() {
        Map<Long, Integer> map = new HashMap<>();
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            List<Object[]> rows = session.createQuery(
                    "select oi.course.id, sum(oi.quantity) " +
                    "from OrderItem oi group by oi.course.id",
                    Object[].class
            ).list();

            for (Object[] r : rows) {
                Long courseId = (Long) r[0];
                Number sum = (Number) r[1];
                map.put(courseId, sum == null ? 0 : sum.intValue());
            }
        } catch (Exception ignore) { }
        return map;
    }

    private void addToCart(Course course) {
        if (course == null || course.getId() == null) return;

        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();

            Long sid = CurrentUser.getCurrentAccount().getId();
            Account student = session.get(Account.class, sid);

            Cart cart = session.createQuery("FROM Cart c WHERE c.student.id = :sid", Cart.class)
                    .setParameter("sid", sid)
                    .uniqueResult();

            if (cart == null) {
                cart = new Cart();
                cart.setStudent(student);
                cart.setCreatedAt(LocalDateTime.now());
                session.persist(cart);
            }

            CartItem existing = session.createQuery(
                            "FROM CartItem ci WHERE ci.cart.id = :cid AND ci.course.id = :courseId",
                            CartItem.class
                    )
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
                item.setPriceAtAdd(course.getTuitionFee());
                session.persist(item);
            }

            tx.commit();

            // ✅ CẬP NHẬT BADGE MENU (giỏ hàng)
            StudentDashboardController.requestCartBadgeRefresh();

            alert(Alert.AlertType.INFORMATION, "OK", "Đã thêm vào giỏ!");
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            e.printStackTrace();
            alert(Alert.AlertType.ERROR, "Lỗi", "Không thể thêm vào giỏ: " + e.getMessage());
        }
    }


    private void alert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
