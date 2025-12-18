package com.ucop.edu.service.impl;

import com.ucop.edu.entity.Order;
import com.ucop.edu.repository.DuyetDonStaffRepository;

import java.util.List;

public class DuyetDonStaffService {

    private final DuyetDonStaffRepository repo = new DuyetDonStaffRepository();

    public List<Order> getPendingOrders() {
        return repo.findByStatusWithStudent("PENDING");
    }

    public void approveOrder(Long orderId) {
        boolean ok = repo.updateStatus(orderId, "SUCCESS");
        if (!ok) {
            throw new RuntimeException("Không tìm thấy Order ID = " + orderId);
        }
    }

    public List<Order> getPaidOrders() {
        return repo.findByStatusWithStudent("PAID");
    }
    
    
    public int approveAllPending() {
        return repo.updateAllPendingToSuccess();
    }
    
    public void refundPaidOrder(Long orderId) {
        boolean ok = repo.refundPaidOrderToWallet(orderId);
        if (!ok) throw new RuntimeException("Không tìm thấy Order ID = " + orderId);
    }

    public int refundAllPaid() {
        return repo.refundAllPaidToWallet();
    }
}