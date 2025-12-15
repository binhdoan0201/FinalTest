package com.ucop.edu.repository;

import com.ucop.edu.entity.Account;
import com.ucop.edu.entity.Wallets;
import org.hibernate.Session;

import java.math.BigDecimal;

public class WalletRepository {

    public Wallets findByAccountId(Long accountId, Session s) {
        return s.createQuery(
                "select w from Wallet w where w.account.id = :aid",
                Wallets.class
        ).setParameter("aid", accountId).uniqueResult();
    }

    public Wallets getOrCreate(Long accountId, Session s) {
        Wallets w = findByAccountId(accountId, s);
        if (w != null) return w;

        Account acc = s.get(Account.class, accountId);

        Wallets nw = new Wallets();
        nw.setAccount(acc);
        nw.setBalance(BigDecimal.ZERO);

        s.persist(nw);
        return nw;
    }
}
