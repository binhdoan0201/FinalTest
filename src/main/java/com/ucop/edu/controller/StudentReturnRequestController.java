package com.ucop.edu.controller;

import com.ucop.edu.dto.OrderOption;
import com.ucop.edu.dto.ReturnRequestRow;
import com.ucop.edu.entity.enums.ReturnRequestType;
import com.ucop.edu.repository.ReturnRequestRepository;
import com.ucop.edu.service.impl.ReturnRequestService;
import com.ucop.edu.util.CurrentUser;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class StudentReturnRequestController {

    @FXML private ComboBox<OrderOption> cbOrder;
    @FXML private ComboBox<ReturnRequestType> cbType;
    @FXML private TextField txtAmount;
    @FXML private TextArea txtReason;
    @FXML private Label lblMsg;

    @FXML private TableView<ReturnRequestRow> tblRequests;
    @FXML private TableColumn<ReturnRequestRow, Long> colId;
    @FXML private TableColumn<ReturnRequestRow, String> colOrderCode;
    @FXML private TableColumn<ReturnRequestRow, String> colType;
    @FXML private TableColumn<ReturnRequestRow, String> colStatus;
    @FXML private TableColumn<ReturnRequestRow, String> colAmount;
    @FXML private TableColumn<ReturnRequestRow, String> colCreatedAt;

    private final ReturnRequestRepository repo = new ReturnRequestRepository();
    private final ReturnRequestService service = new ReturnRequestService();

    private final NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        cbType.setItems(FXCollections.observableArrayList(ReturnRequestType.values()));
        cbType.setValue(ReturnRequestType.REFUND);

        cbOrder.setConverter(new StringConverter<>() {
            @Override public String toString(OrderOption o) {
                if (o == null) return "";
                BigDecimal due = o.getDue() == null ? BigDecimal.ZERO : o.getDue();
                String status = (o.getOrderStatus() == null ? "" : o.getOrderStatus());
                return "Order #" + o.getOrderId() + " | Còn nợ: " + vnd.format(due) + " VNĐ | " + status;
            }
            @Override public OrderOption fromString(String s) { return null; }
        });

        // table
        colId.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getId()));
        colOrderCode.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getOrderCode())));
        colType.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getType())));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(nvl(c.getValue().getStatus())));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(formatRefundAmount(c.getValue())));
        colCreatedAt.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCreatedAt() == null ? "" : dtf.format(c.getValue().getCreatedAt())
        ));

        handleRefresh();
    }

    @FXML
    private void handleRefresh() {
        try {
            Long sid = CurrentUser.getCurrentAccount().getId();

            List<OrderOption> opts = repo.findOrderOptionsForStudent(sid);
            cbOrder.setItems(FXCollections.observableArrayList(opts));
            if (!opts.isEmpty() && cbOrder.getValue() == null) cbOrder.setValue(opts.get(0));

            List<ReturnRequestRow> history = repo.findHistoryRowsByStudent(sid);
            tblRequests.setItems(FXCollections.observableArrayList(history));

            txtAmount.clear();
            txtReason.clear();

            setInfo("ℹ Đã tải danh sách Order & lịch sử yêu cầu.");
        } catch (Exception e) {
            e.printStackTrace();
            setErr("Load lỗi: " + e.getMessage());
        }
    }

    @FXML
    private void handleSubmit() {
        try {
            Long sid = CurrentUser.getCurrentAccount().getId();

            OrderOption opt = cbOrder.getValue();
            if (opt == null || opt.getOrderId() == null) { setErr("Chưa chọn Order"); return; }

            ReturnRequestType type = cbType.getValue();
            if (type == null) type = ReturnRequestType.REFUND;

            BigDecimal amount = parseMoneyOrNull(txtAmount.getText()); // null = FULL/AUTO
            String reason = (txtReason.getText() == null) ? "" : txtReason.getText().trim();
            if (reason.isBlank()) { setErr("Vui lòng nhập lý do"); return; }

            service.createByOrder(sid, opt.getOrderId(), type, amount, reason);

            setOk("✅ Đã gửi yêu cầu " + type.name() + " thành công.");
            handleRefresh();

        } catch (Exception ex) {
            ex.printStackTrace();
            setErr(ex.getMessage());
        }
    }

    private String formatRefundAmount(ReturnRequestRow r) {
        if (r == null) return "";
        if (r.getRefundAmount() == null) return "FULL/AUTO";
        return vnd.format(r.getRefundAmount()) + " VNĐ";
    }

    private BigDecimal parseMoneyOrNull(String text) {
        try {
            if (text == null) return null;
            String t = text.trim();
            if (t.isEmpty()) return null;
            t = t.replace(" ", "");

            if (t.contains(".") && t.indexOf('.') != t.lastIndexOf('.')) t = t.replace(".", "");
            t = t.replace(",", "");

            if (t.isEmpty()) return null;
            return new BigDecimal(t);
        } catch (Exception e) {
            return null;
        }
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private void setOk(String msg) {
        lblMsg.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:800;");
        lblMsg.setText(msg);
    }
    private void setErr(String msg) {
        lblMsg.setStyle("-fx-text-fill:#ef4444; -fx-font-weight:800;");
        lblMsg.setText("❌ " + msg);
    }
    private void setInfo(String msg) {
        lblMsg.setStyle("-fx-text-fill:#2563eb; -fx-font-weight:800;");
        lblMsg.setText(msg);
    }
}
