package com.ucop.edu.entity;

import com.ucop.edu.entity.enums.WalletTxType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // DB: wallet_id NOT NULL
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallets wallet;

    // DB: account_id NOT NULL
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 30, nullable = false)
    private WalletTxType type;

    @Column(name = "amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "balance_after", precision = 18, scale = 2, nullable = false)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ✅ DB có order_id (nullable)
    @Column(name = "order_id")
    private Long orderId;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (amount == null) amount = BigDecimal.ZERO;
        if (balanceAfter == null) {
            balanceAfter = (wallet != null && wallet.getBalance() != null)
                    ? wallet.getBalance()
                    : BigDecimal.ZERO;
        }
    }

    // ===== getter/setter =====
    public Long getId() { return id; }

    public Wallets getWallet() { return wallet; }
    public void setWallet(Wallets wallet) { this.wallet = wallet; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public WalletTxType getType() { return type; }
    public void setType(WalletTxType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
}
