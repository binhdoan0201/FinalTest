package com.ucop.edu.entity;

import jakarta.persistence.*;

public class Wallets {
	//======================================================
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	public int getId() {
		return id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	//======================================================
	@Column(name = "balance")
	private double balance;
	
	public double getBalance() {
		return balance;
	}
	
	public void setBalance(double balance) {
		this.balance = balance;
	}
	//======================================================
	@OneToOne()
	@JoinColumn(name = "account_id")
	private Account account;
	
	public Account getAccount() {
		return account;
	}
	
	public void setAccount(Account account) {
		this.account = account;
	}
	//======================================================
}
