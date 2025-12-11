package com.ucop.edu.service.impl;

import com.ucop.edu.entity.Account;
import com.ucop.edu.entity.AuditLog;
import com.ucop.edu.repository.AccountRepository;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public class AccountService {

    private final AccountRepository accountRepo = new AccountRepository();

    public List<Account> getAllAccounts() {
        return accountRepo.findAll();
    }

    public void createAccount(Account account, String createdBy) {
        // Lưu tài khoản
        accountRepo.save(account);

        // Ghi audit log – KHÔNG DÙNG BUILDER
        AuditLog log = new AuditLog();
        log.setUsername(createdBy);
        log.setAction("CREATE_USER");
        log.setDescription("Tạo tài khoản: " + account.getUsername());
        log.setCreatedAt(LocalDateTime.now());

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            session.save(log);
            tx.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toggleLock(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            Account acc = session.get(Account.class, id);
            if (acc != null) {
                acc.setEnabled(!acc.isEnabled());
                session.update(acc);
                tx.commit();
            }
        }
    }
}