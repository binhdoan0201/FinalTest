package com.ucop.edu.controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;

public class CategoryCourseViewController {

    @FXML private Tab tabCourse;

    // phải khớp với fx:id="courseView" trong fx:include
    @FXML private CourseManagementController courseViewController;

    @FXML
    private void onCourseTabSelected(Event e) {
        // Cách an toàn: lấy Tab từ event (tránh NPE nếu tabCourse chưa inject)
        Tab t = (Tab) e.getSource();
        if (!t.isSelected()) return;

        if (courseViewController != null) {
            courseViewController.refreshCategories();
        }
    }
}
