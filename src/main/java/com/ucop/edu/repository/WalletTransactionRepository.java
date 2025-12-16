package com.ucop.edu.repository;

import com.ucop.edu.entity.WalletTransaction;
import org.hibernate.Session;

import java.util.List;

public class WalletTransactionRepository {

    public List<WalletTransaction> findByStudent(Long studentId, Session s) {
        return s.createQuery(
                "select tx from WalletTransaction tx " +
                "join fetch tx.wallet w " +
                "join fetch w.account a " +
                "where a.id = :sid " +
                "order by tx.id desc",
                WalletTransaction.class
        ).setParameter("sid", studentId)
         .list();
    }
}
