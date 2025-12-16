package com.ucop.edu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
public class Wallets {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", unique = true, nullable = false)
    private Account account;

    @Column(name = "balance", precision = 18, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (balance == null) balance = BigDecimal.ZERO;
    }

    public void add(BigDecimal amount) {
        if (amount == null) return;
        if (balance == null) balance = BigDecimal.ZERO;
        balance = balance.add(amount);
    }

    public void sub(BigDecimal amount) {
        if (amount == null) return;
        if (balance == null) balance = BigDecimal.ZERO;
        balance = balance.subtract(amount);
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = (balance == null ? BigDecimal.ZERO : balance); }

    // (không bắt buộc) chỉ để tương thích nếu code cũ có gọi setId()
    public void setId(Long id) { this.id = id; }
}
