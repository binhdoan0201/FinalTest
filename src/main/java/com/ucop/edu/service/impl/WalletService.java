package com.ucop.edu.service.impl;

import com.ucop.edu.entity.Account;
import com.ucop.edu.entity.WalletTransaction;
import com.ucop.edu.entity.Wallets;
import com.ucop.edu.entity.enums.WalletTxType;
import com.ucop.edu.repository.WalletRepository;
import com.ucop.edu.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletService {

    private final WalletRepository walletRepo = new WalletRepository();

    public void topup(Long accountId, BigDecimal amount) {
        if (accountId == null) throw new IllegalArgumentException("accountId null");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("Số tiền nạp không hợp lệ");

        Transaction tx = null;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            tx = s.beginTransaction();

            Account acc = s.get(Account.class, accountId);
            if (acc == null) throw new IllegalStateException("Không tìm thấy account id=" + accountId);

            Wallets w = walletRepo.getOrCreate(accountId, s);

            w.add(amount);          // w managed
            // s.merge(w);          // không bắt buộc, nhưng để rõ ràng cũng ok

            WalletTransaction t = new WalletTransaction();
            t.setWallet(w);
            t.setAccount(acc);
            t.setType(WalletTxType.TOPUP);
            t.setAmount(amount);
            t.setBalanceAfter(w.getBalance());
            t.setMessage("Nạp ví");
            t.setCreatedAt(LocalDateTime.now());

            s.persist(t);

            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e;
        }
    }
}
