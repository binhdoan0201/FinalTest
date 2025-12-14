package com.ucop.edu.controller;

import com.ucop.edu.entity.Categories;
import com.ucop.edu.repository.CategoryRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.util.List;

public class CategoryManagementController {
	

    @FXML private TableView<Categories> tblCategories;
    @FXML private TableColumn<Categories, Long> colId;
    @FXML private TableColumn<Categories, String> colName;
    @FXML private TableColumn<Categories, String> colParent;

    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private ComboBox<Categories> cbParent;
    @FXML private Label lblMsg;

    private final CategoryRepository repo = new CategoryRepository();
    private final ObservableList<Categories> data = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        colParent.setCellValueFactory(cell -> {
            Categories p = cell.getValue().getParent();
            String text = (p == null) ? "" : p.getName();
            return new javafx.beans.property.SimpleStringProperty(text);
        });

        // ComboBox show name
        cbParent.setConverter(new StringConverter<>() {
            @Override public String toString(Categories c) { return c == null ? "" : c.getName(); }
            @Override public Categories fromString(String s) { return null; }
        });

        // Select row -> fill form
        tblCategories.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) fillForm(newV);
        });
        

        reloadAll();
        handleClear();
    }

    private void reloadAll() {
        List<Categories> list = repo.findAll();
        data.setAll(list);
        tblCategories.setItems(data);

        // parent list = all categories
        cbParent.setItems(FXCollections.observableArrayList(list));
    }

    private void fillForm(Categories c) {
        txtId.setText(String.valueOf(c.getId()));
        txtName.setText(c.getName() == null ? "" : c.getName());
        cbParent.setValue(c.getParent());
        lblMsg.setText("");
    }

    private boolean wouldCreateCycle(Categories current, Categories newParent) {
        // Nếu chọn parent là chính nó hoặc parent nằm trong con cháu => tạo vòng lặp
        if (current == null || newParent == null) return false;
        if (current.getId() != null && current.getId().equals(newParent.getId())) return true;

        Categories p = newParent;
        while (p != null) {
            if (p.getId() != null && current.getId() != null && p.getId().equals(current.getId())) return true;
            p = p.getParent();
        }
        return false;
    }

    @FXML
    private void handleAdd() {
        String name = txtName.getText() == null ? "" : txtName.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Name không được để trống!");
            return;
        }

        Categories c = new Categories();
        c.setName(name);

        Categories parent = cbParent.getValue();
        c.setParent(parent);

        try {
            repo.save(c);
            lblMsg.setText("✅ Added category: " + name);
            reloadAll();
            handleClear();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thêm được category.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        Categories selected = tblCategories.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Bạn chưa chọn category để sửa.");
            return;
        }

        String name = txtName.getText() == null ? "" : txtName.getText().trim();
        if (name.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Name không được để trống!");
            return;
        }

        Categories newParent = cbParent.getValue();
        if (wouldCreateCycle(selected, newParent)) {
            showAlert(Alert.AlertType.WARNING, "Sai parent", "Không được chọn parent gây vòng lặp (parent = chính nó/thuộc con cháu).");
            return;
        }

        selected.setName(name);
        selected.setParent(newParent);

        try {
            repo.update(selected);
            lblMsg.setText("✅ Updated category: " + name);
            reloadAll();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không cập nhật được category.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        Categories selected = tblCategories.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Bạn chưa chọn category để xóa.");
            return;
        }

        long childCnt = repo.countChildren(selected.getId());
        long courseCnt = repo.countCourses(selected.getId());

        if (childCnt > 0) {
            showAlert(Alert.AlertType.WARNING, "Không thể xóa", "Category đang có " + childCnt + " danh mục con.");
            return;
        }
        if (courseCnt > 0) {
            showAlert(Alert.AlertType.WARNING, "Không thể xóa", "Category đang có " + courseCnt + " course.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Xóa category: " + selected.getName());
        confirm.setContentText("Bạn chắc chắn muốn xóa?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            repo.delete(selected);
            lblMsg.setText("✅ Deleted category: " + selected.getName());
            reloadAll();
            handleClear();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không xóa được category.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        tblCategories.getSelectionModel().clearSelection();
        txtId.setText("");
        txtName.setText("");
        cbParent.setValue(null);
        lblMsg.setText("");
    }

    @FXML
    private void backToDashboard() throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin-dashboard.fxml"));
        Stage stage = (Stage) tblCategories.getScene().getWindow();
        
        // Set lại kích thước chuẩn như lần đầu mở Dashboard
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
