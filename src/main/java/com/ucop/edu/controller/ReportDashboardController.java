package com.ucop.edu.controller;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.ucop.edu.dto.PaymentMethodRevenueDTO;
import com.ucop.edu.dto.RevenuePointDTO;
import com.ucop.edu.dto.TopCourseDTO;
import com.ucop.edu.service.impl.ReportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ReportDashboardController {

    @FXML private DatePicker fromPicker;
    @FXML private DatePicker toPicker;

    @FXML private LineChart<String, Number> revenueLineChart;
    @FXML private BarChart<String, Number> paymentMethodBarChart;

    @FXML private TableView<TopCourseDTO> topTable;
    @FXML private TableColumn<TopCourseDTO, String> colCourse;
    @FXML private TableColumn<TopCourseDTO, Long> colCount;
    @FXML private TableColumn<TopCourseDTO, String> colRevenue;

    @FXML private Label lblTotalRefund;

    private final ReportService reportService = new ReportService();
    private final ObservableList<TopCourseDTO> topData = FXCollections.observableArrayList();

    private final NumberFormat vndFmt = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter ymd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter fileFmt = DateTimeFormatter.ofPattern("yyyyMMdd");

    @FXML
    public void initialize() {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(30);
        fromPicker.setValue(from);
        toPicker.setValue(to);

        colCourse.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getCourseName()));
        colCount.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getEnrollCount()));
        colRevenue.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(formatVnd(c.getValue().getRevenue())));

        topTable.setItems(topData);
        reload();
    }

    @FXML
    public void reload() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null) { alert("Chọn From/To trước."); return; }
        if (from.isAfter(to)) { alert("From không được sau To."); return; }

        loadRevenueChart(from, to);
        loadPaymentMethodChart(from, to);
        loadTopCourses(from, to);

        BigDecimal refund = reportService.totalRefund(from, to);
        lblTotalRefund.setText("Total Refund: " + formatVnd(refund));
    }

    private void loadRevenueChart(LocalDate from, LocalDate to) {
        revenueLineChart.getData().clear();

        List<RevenuePointDTO> points = reportService.revenueByDay(from, to);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Revenue by Day");

        for (RevenuePointDTO p : points) {
            s.getData().add(new XYChart.Data<>(p.getLabel(), p.getTotalRevenue()));
        }
        revenueLineChart.getData().add(s);
    }

    private void loadPaymentMethodChart(LocalDate from, LocalDate to) {
        paymentMethodBarChart.getData().clear();

        List<PaymentMethodRevenueDTO> rows = reportService.revenueByPaymentMethod(from, to);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        s.setName("Revenue by Payment Method");

        for (PaymentMethodRevenueDTO r : rows) {
            s.getData().add(new XYChart.Data<>(r.getMethod(), r.getTotal()));
        }
        paymentMethodBarChart.getData().add(s);
    }

    private void loadTopCourses(LocalDate from, LocalDate to) {
        topData.setAll(reportService.topCourses(from, to, 10));
    }

    // ====== EXPORT EXCEL/PDF (đúng tên cho FXML: exportExcel, exportPdf) ======

    @FXML
    private void exportExcel(ActionEvent e) {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null || from.isAfter(to)) { alert("From/To không hợp lệ."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất báo cáo doanh thu (Excel)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx"));
        fc.setInitialFileName("revenue_report_" + from.format(fileFmt) + "_" + to.format(fileFmt) + ".xlsx");

        File file = fc.showSaveDialog(getStage());
        if (file == null) return;

        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle moneyStyle = wb.createCellStyle();
            moneyStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Sheet 1: Revenue by Day
            Sheet sh1 = wb.createSheet("RevenueByDay");
            writeRevenuePointsSheet(sh1, "Date", reportService.revenueByDay(from, to), moneyStyle);

            // Sheet 2: Revenue by Method
            Sheet sh2 = wb.createSheet("RevenueByMethod");
            writeMethodSheet(sh2, reportService.revenueByPaymentMethod(from, to), moneyStyle);

            // Sheet 3: Top Courses
            Sheet sh3 = wb.createSheet("TopCourses");
            writeTopCoursesSheet(sh3, reportService.topCourses(from, to, 10), moneyStyle);

            // Sheet 4: Summary
            Sheet sh4 = wb.createSheet("Summary");
            writeSummarySheet(sh4, from, to, reportService.totalRefund(from, to), moneyStyle);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }

            alert("✅ Đã xuất Excel: " + file.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            alert("Xuất Excel lỗi: " + ex.getMessage());
        }
    }

    @FXML
    private void exportPdf(ActionEvent e) {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null || from.isAfter(to)) { alert("From/To không hợp lệ."); return; }

        FileChooser fc = new FileChooser();
        fc.setTitle("Xuất báo cáo doanh thu (PDF)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));
        fc.setInitialFileName("revenue_report_" + from.format(fileFmt) + "_" + to.format(fileFmt) + ".pdf");

        File file = fc.showSaveDialog(getStage());
        if (file == null) return;

        try {
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();

            doc.add(new Paragraph("BAO CAO DOANH THU"));
            doc.add(new Paragraph("From: " + from.format(ymd) + "   To: " + to.format(ymd)));
            doc.add(new Paragraph(" "));

            // Revenue by day
            doc.add(new Paragraph("1) Doanh thu theo ngay"));
            PdfPTable t1 = new PdfPTable(2);
            t1.setWidthPercentage(100);
            addHeader(t1, "Date");
            addHeader(t1, "Total");
            for (RevenuePointDTO p : reportService.revenueByDay(from, to)) {
                t1.addCell(p.getLabel());
                t1.addCell(formatVnd(p.getTotalRevenue()));
            }
            doc.add(t1);
            doc.add(new Paragraph(" "));

            // Revenue by method
            doc.add(new Paragraph("2) Doanh thu theo phuong thuc thanh toan"));
            PdfPTable t2 = new PdfPTable(2);
            t2.setWidthPercentage(100);
            addHeader(t2, "Method");
            addHeader(t2, "Total");
            for (PaymentMethodRevenueDTO r : reportService.revenueByPaymentMethod(from, to)) {
                t2.addCell(r.getMethod() == null ? "" : r.getMethod());
                t2.addCell(formatVnd(r.getTotal()));
            }
            doc.add(t2);
            doc.add(new Paragraph(" "));

            // Top courses
            doc.add(new Paragraph("3) Top khoa hoc"));
            PdfPTable t3 = new PdfPTable(3);
            t3.setWidthPercentage(100);
            addHeader(t3, "Course");
            addHeader(t3, "Enroll");
            addHeader(t3, "Revenue");
            for (TopCourseDTO c : reportService.topCourses(from, to, 10)) {
                t3.addCell(c.getCourseName());
                t3.addCell(String.valueOf(c.getEnrollCount()));
                t3.addCell(formatVnd(c.getRevenue()));
            }
            doc.add(t3);
            doc.add(new Paragraph(" "));

            BigDecimal refund = reportService.totalRefund(from, to);
            doc.add(new Paragraph("Total Refund: " + formatVnd(refund)));

            doc.close();
            alert("✅ Đã xuất PDF: " + file.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            alert("Xuất PDF lỗi: " + ex.getMessage());
        }
    }

    private void addHeader(PdfPTable t, String text) {
        PdfPCell cell = new PdfPCell(new Paragraph(text));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        t.addCell(cell);
    }

    // ===== Excel writers =====
    private void writeRevenuePointsSheet(Sheet sh, String labelCol, List<RevenuePointDTO> points, CellStyle moneyStyle) {
        org.apache.poi.ss.usermodel.Row h = sh.createRow(0);
        h.createCell(0).setCellValue(labelCol);
        h.createCell(1).setCellValue("Total");

        int row = 1;
        for (RevenuePointDTO p : points) {
            org.apache.poi.ss.usermodel.Row r = sh.createRow(row++);
            r.createCell(0).setCellValue(p.getLabel());
            var c1 = r.createCell(1);
            c1.setCellValue(p.getTotalRevenue().doubleValue());
            c1.setCellStyle(moneyStyle);
        }
        sh.autoSizeColumn(0);
        sh.autoSizeColumn(1);
    }

    private void writeMethodSheet(Sheet sh, List<PaymentMethodRevenueDTO> rows, CellStyle moneyStyle) {
        org.apache.poi.ss.usermodel.Row h = sh.createRow(0);
        h.createCell(0).setCellValue("Method");
        h.createCell(1).setCellValue("Total");

        int row = 1;
        for (PaymentMethodRevenueDTO r0 : rows) {
            org.apache.poi.ss.usermodel.Row r = sh.createRow(row++);
            r.createCell(0).setCellValue(r0.getMethod() == null ? "" : r0.getMethod());
            var c1 = r.createCell(1);
            c1.setCellValue(r0.getTotal().doubleValue());
            c1.setCellStyle(moneyStyle);
        }
        sh.autoSizeColumn(0);
        sh.autoSizeColumn(1);
    }

    private void writeTopCoursesSheet(Sheet sh, List<TopCourseDTO> rows, CellStyle moneyStyle) {
        org.apache.poi.ss.usermodel.Row h = sh.createRow(0);
        h.createCell(0).setCellValue("Course");
        h.createCell(1).setCellValue("Enroll");
        h.createCell(2).setCellValue("Revenue");

        int row = 1;
        for (TopCourseDTO c : rows) {
            org.apache.poi.ss.usermodel.Row r = sh.createRow(row++);
            r.createCell(0).setCellValue(c.getCourseName());
            r.createCell(1).setCellValue(c.getEnrollCount());
            var c2 = r.createCell(2);
            c2.setCellValue(c.getRevenue().doubleValue());
            c2.setCellStyle(moneyStyle);
        }
        sh.autoSizeColumn(0);
        sh.autoSizeColumn(1);
        sh.autoSizeColumn(2);
    }

    private void writeSummarySheet(Sheet sh, LocalDate from, LocalDate to, BigDecimal totalRefund, CellStyle moneyStyle) {
        org.apache.poi.ss.usermodel.Row r0 = sh.createRow(0);
        r0.createCell(0).setCellValue("From");
        r0.createCell(1).setCellValue(from.format(ymd));

        org.apache.poi.ss.usermodel.Row r1 = sh.createRow(1);
        r1.createCell(0).setCellValue("To");
        r1.createCell(1).setCellValue(to.format(ymd));

        org.apache.poi.ss.usermodel.Row r2 = sh.createRow(2);
        r2.createCell(0).setCellValue("Total Refund");
        var c1 = r2.createCell(1);
        c1.setCellValue(totalRefund.doubleValue());
        c1.setCellStyle(moneyStyle);

        sh.autoSizeColumn(0);
        sh.autoSizeColumn(1);
    }

    private String formatVnd(BigDecimal v) {
        if (v == null) return "0 VND";
        return vndFmt.format(v) + " VND";
    }

    private Stage getStage() {
        return (Stage) revenueLineChart.getScene().getWindow();
    }

    private void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    @FXML
    private void backToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/admin-dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("UCOP - Admin Dashboard");
            stage.centerOnScreen();
            stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
