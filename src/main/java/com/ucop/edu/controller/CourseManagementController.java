package com.ucop.edu.controller;

import com.ucop.edu.entity.Categories;
import com.ucop.edu.entity.Course;
import com.ucop.edu.entity.enums.CourseStatus;
import com.ucop.edu.service.impl.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CourseManagementController {

    // ========= IMAGE STORAGE (lưu ở máy) =========
	private static final Path BASE_DIR = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
	private static final Path COURSE_IMG_DIR = BASE_DIR.resolve("images");

    // Table
    @FXML private TableView<Course> tblCourses;
    @FXML private TableColumn<Course, String> colImage; // NEW
    @FXML private TableColumn<Course, Long> colId;
    @FXML private TableColumn<Course, String> colName;
    @FXML private TableColumn<Course, String> colCategory;
    @FXML private TableColumn<Course, BigDecimal> colFee;
    @FXML private TableColumn<Course, Integer> colDuration;
    @FXML private TableColumn<Course, Integer> colMaxSeats;
    @FXML private TableColumn<Course, Integer> colAvailableSeats;
    @FXML private TableColumn<Course, String> colStatus;

    // Filter
    @FXML private ComboBox<Categories> cbFilterCategory;
    @FXML private TextField txtSearch;

    // Form
    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private ComboBox<Categories> cbCategory;
    @FXML private ComboBox<CourseStatus> cbStatus;
    @FXML private TextField txtFee;
    @FXML private TextField txtDuration;
    @FXML private TextField txtMaxSeats;
    @FXML private TextField txtCreatedBy;
    @FXML private TextField txtUpdatedBy;
    @FXML private TextArea txtDescription;

    // NEW: image controls
    @FXML private TextField txtImageUrl;
    @FXML private ImageView imgPreview;

    @FXML private Label lblMsg;

    private final CourseService courseService = new CourseService();
    private final CategoryService categoryService = new CategoryService();

    private final ObservableList<Course> data = FXCollections.observableArrayList();

    public void refreshCategories() {
        reloadCategories();
    }
    @FXML
    public void initialize() {
        // ===== columns =====
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colFee.setCellValueFactory(new PropertyValueFactory<>("tuitionFee"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationHours"));
        colMaxSeats.setCellValueFactory(new PropertyValueFactory<>("maxSeats"));
        colAvailableSeats.setCellValueFactory(new PropertyValueFactory<>("availableSeats"));

        colStatus.setCellValueFactory(cell -> new SimpleStringProperty(
                cell.getValue().getStatus() == null ? "" : cell.getValue().getStatus().name()
        ));

        colCategory.setCellValueFactory(cell -> {
            Categories c = cell.getValue().getCategory();
            return new SimpleStringProperty(c == null ? "" : c.getName());
        });

        // ===== NEW: image column thumbnail =====
        colImage.setCellValueFactory(new PropertyValueFactory<>("imageUrl"));
        colImage.setCellFactory(col -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            {
                iv.setFitWidth(70);
                iv.setFitHeight(50);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
            }

            @Override
            protected void updateItem(String storedPath, boolean empty) {
                super.updateItem(storedPath, empty);
                if (empty || storedPath == null || storedPath.trim().isEmpty()) {
                    setGraphic(null);
                    return;
                }
                try {
                    String uri = toImageUri(storedPath);
                    iv.setImage(new Image(uri, 70, 50, true, true, true));
                    setGraphic(iv);
                } catch (Exception e) {
                    setGraphic(null);
                }
            }
        });

        // ComboBox show category name
        StringConverter<Categories> catConverter = new StringConverter<>() {
            @Override public String toString(Categories c) { return c == null ? "" : c.getName(); }
            @Override public Categories fromString(String s) { return null; }
        };
        cbCategory.setConverter(catConverter);
        cbFilterCategory.setConverter(catConverter);

        cbStatus.setItems(FXCollections.observableArrayList(CourseStatus.values()));

        tblCourses.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) fillForm(newV);
        });

        reloadCategories();
        reloadCourses();
        handleClear();
    }

    private void reloadCategories() {
        List<Categories> cats = categoryService.findAll();
        cbCategory.setItems(FXCollections.observableArrayList(cats));

        // Filter: thêm option null = All
        ObservableList<Categories> filter = FXCollections.observableArrayList();
        filter.add(null);
        filter.addAll(cats);
        cbFilterCategory.setItems(filter);
        cbFilterCategory.setValue(null);
    }

    private void reloadCourses() {
        List<Course> list = courseService.findAll();
        data.setAll(list);
        tblCourses.setItems(data);
    }

    private void fillForm(Course c) {
        txtName.setText(nvl(c.getName()));
        txtDescription.setText(nvl(c.getDescription()));
        cbCategory.setValue(c.getCategory());
        cbStatus.setValue(c.getStatus());

        txtFee.setText(c.getTuitionFee() == null ? "" : c.getTuitionFee().toPlainString());
        txtDuration.setText(c.getDurationHours() == null ? "" : String.valueOf(c.getDurationHours()));
        txtMaxSeats.setText(c.getMaxSeats() == null ? "" : String.valueOf(c.getMaxSeats()));

        txtCreatedBy.setText(nvl(c.getCreatedBy()));
        txtUpdatedBy.setText(nvl(c.getUpdatedBy()));

        // NEW: image
        txtImageUrl.setText(nvl(c.getImageUrl()));
        showPreviewFromStoredPath(c.getImageUrl());

        lblMsg.setText("");
    }

    private void showPreviewFromStoredPath(String storedPath) {
        try {
            if (storedPath == null || storedPath.trim().isEmpty()) {
                imgPreview.setImage(null);
                return;
            }
            String uri = toImageUri(storedPath);
            imgPreview.setImage(new Image(uri, 240, 150, true, true, true));
        } catch (Exception e) {
            imgPreview.setImage(null);
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }

    // ===================== NEW: Choose Image =====================
    @FXML
    private void handleChooseImage() {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Choose course image");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                    new FileChooser.ExtensionFilter("All files", "*.*")
            );

            Stage stage = (Stage) tblCourses.getScene().getWindow();
            File chosen = fc.showOpenDialog(stage);
            if (chosen == null) return;

            Files.createDirectories(COURSE_IMG_DIR);

            String ext = getFileExt(chosen.getName());
            String newName = "course_" + System.currentTimeMillis() + "_" + UUID.randomUUID() + ext;

            Path target = COURSE_IMG_DIR.resolve(newName);
            Files.copy(chosen.toPath(), target, StandardCopyOption.REPLACE_EXISTING);

            // ✅ lưu dạng resource path
            String stored = "images/" + newName;
            txtImageUrl.setText(stored);

            // preview
            showPreviewFromStoredPath(stored);

            lblMsg.setText("✅ Selected image: " + stored);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không chọn/lưu được ảnh.\n" + e.getMessage());
        }
    }

    private String getFileExt(String name) {
        int idx = name.lastIndexOf('.');
        if (idx < 0) return "";
        return name.substring(idx);
    }

    /** Convert stored path (images/courses/xxx.jpg OR http...) to usable URI for Image() */
    private String toImageUri(String storedPath) {
        if (storedPath == null) return null;
        String s = storedPath.trim();
        if (s.isEmpty()) return null;

        if (s.startsWith("http://") || s.startsWith("https://") || s.startsWith("file:")) {
            return s;
        }

        if (s.startsWith("/")) s = s.substring(1); // support cũ

        Path p = Paths.get(s);
        if (!p.isAbsolute()) {
            p = BASE_DIR.resolve(p).normalize(); // ✅ dùng BASE_DIR
        }

        File f = p.toFile();
        if (!f.exists()) return null;

        return f.toURI().toString();
    }

    // ===================== CRUD =====================
    @FXML
    private void handleAdd() {
        try {
            Course c = readFormForCreate();
            courseService.save(c);
            lblMsg.setText("✅ Added course: " + c.getName());
            reloadCourses();
            handleClear();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        Course selected = tblCourses.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Bạn chưa chọn course để sửa.");
            return;
        }
        try {
            applyFormToExisting(selected);
            courseService.update(selected);
            lblMsg.setText("✅ Updated course: " + selected.getName());
            reloadCourses();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Course selected = tblCourses.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Bạn chưa chọn course để xóa.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Xóa course: " + selected.getName());
        confirm.setContentText("Bạn chắc chắn muốn xóa?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            courseService.delete(selected.getId());
            lblMsg.setText("✅ Deleted course: " + selected.getName());
            reloadCourses();
            handleClear();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        tblCourses.getSelectionModel().clearSelection();
        txtName.clear();
        txtDescription.clear();
        cbCategory.setValue(null);
        cbStatus.setValue(CourseStatus.OPEN);
        txtFee.clear();
        txtDuration.clear();
        txtMaxSeats.clear();
        txtCreatedBy.clear();
        txtUpdatedBy.clear();

        // NEW
        txtImageUrl.clear();
        imgPreview.setImage(null);

        lblMsg.setText("");
    }

    @FXML
    private void handleSearch() {
        Categories cat = cbFilterCategory.getValue();
        String key = txtSearch.getText() == null ? "" : txtSearch.getText().trim();

        List<Course> list = courseService.search(cat == null ? null : cat.getId(), key);
        data.setAll(list);
        tblCourses.setItems(data);
    }

    @FXML
    private void handleResetFilter() {
        cbFilterCategory.setValue(null);
        txtSearch.clear();
        reloadCourses();
    }

    private Course readFormForCreate() {
        String name = safeTrim(txtName.getText());
        if (name.isEmpty()) throw new IllegalArgumentException("Name không được để trống.");

        Categories category = cbCategory.getValue();
        if (category == null) throw new IllegalArgumentException("Bạn phải chọn Category.");

        BigDecimal fee = parseBigDecimalRequired(txtFee.getText(), "Tuition Fee");
        Integer duration = parseIntOptional(txtDuration.getText());
        Integer maxSeats = parseIntRequired(txtMaxSeats.getText(), "Max Seats");

        CourseStatus status = cbStatus.getValue();
        if (status == null) status = CourseStatus.OPEN;

        Course c = new Course();
        c.setName(name);
        c.setCategory(category);
        c.setStatus(status);
        c.setTuitionFee(fee);
        c.setDurationHours(duration);
        c.setMaxSeats(maxSeats);
        c.setAvailableSeats(maxSeats);
        c.setDescription(txtDescription.getText());

        // NEW: imageUrl
        c.setImageUrl(safeTrim(txtImageUrl.getText()));

        c.setCreatedBy("ADMIN");
        c.setUpdatedBy("ADMIN");
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return c;
    }

    private void applyFormToExisting(Course c) {
        String name = safeTrim(txtName.getText());
        if (name.isEmpty()) throw new IllegalArgumentException("Name không được để trống.");

        Categories category = cbCategory.getValue();
        if (category == null) throw new IllegalArgumentException("Bạn phải chọn Category.");

        c.setName(name);
        c.setCategory(category);
        c.setStatus(cbStatus.getValue() == null ? CourseStatus.OPEN : cbStatus.getValue());
        c.setTuitionFee(parseBigDecimalRequired(txtFee.getText(), "Tuition Fee"));
        c.setDurationHours(parseIntOptional(txtDuration.getText()));
        c.setMaxSeats(parseIntRequired(txtMaxSeats.getText(), "Max Seats"));
        c.setDescription(txtDescription.getText());

        // NEW: imageUrl
        c.setImageUrl(safeTrim(txtImageUrl.getText()));

        c.setUpdatedBy(safeTrim(txtUpdatedBy.getText()));
        c.setUpdatedAt(LocalDateTime.now());
    }

    private String safeTrim(String s) { return s == null ? "" : s.trim(); }

    private Integer parseIntOptional(String s) {
        String t = safeTrim(s);
        if (t.isEmpty()) return null;
        try { return Integer.parseInt(t); }
        catch (Exception e) { throw new IllegalArgumentException("Giá trị số không hợp lệ: " + t); }
    }

    private Integer parseIntRequired(String s, String fieldName) {
        String t = safeTrim(s);
        if (t.isEmpty()) throw new IllegalArgumentException(fieldName + " không được để trống.");
        try { return Integer.parseInt(t); }
        catch (Exception e) { throw new IllegalArgumentException(fieldName + " phải là số."); }
    }

    private BigDecimal parseBigDecimalRequired(String s, String fieldName) {
        String t = safeTrim(s);
        if (t.isEmpty()) throw new IllegalArgumentException(fieldName + " không được để trống.");
        try { return new BigDecimal(t); }
        catch (Exception e) { throw new IllegalArgumentException(fieldName + " không hợp lệ."); }
    }

    @FXML
    private void backToDashboard() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin-dashboard.fxml"));
        Stage stage = (Stage) tblCourses.getScene().getWindow();

        stage.setWidth(1200);
        stage.setHeight(800);
        stage.centerOnScreen();

        stage.setScene(new Scene(root));
        stage.setTitle("UCOP - Universal Commerce & Operations Platform");
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
