package com.ucop.edu.repository;

import com.ucop.edu.entity.Account;
import com.ucop.edu.entity.Wallets;
import org.hibernate.Session;

import java.math.BigDecimal;

public class WalletRepository {

    public Wallets getOrCreate(Long accountId, Session s) {
        Wallets w = s.createQuery(
                "select w from Wallets w join fetch w.account a where a.id = :aid",
                Wallets.class
        ).setParameter("aid", accountId).uniqueResult();

        if (w == null) {
            Account a = s.get(Account.class, accountId);
            if (a == null) throw new IllegalStateException("Account không tồn tại id=" + accountId);

            w = new Wallets();
            w.setAccount(a);
            w.setBalance(BigDecimal.ZERO);
            s.persist(w); // sau dòng này w là managed
        }
        return w;
    }
}
