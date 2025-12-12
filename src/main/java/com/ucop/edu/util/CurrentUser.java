package com.ucop.edu.util;

import com.ucop.edu.entity.Account;

public class CurrentUser {
    private static Account currentAccount;

    public static Account getCurrentAccount() {
        return currentAccount;
    }

    public static void setCurrentAccount(Account account) {
        currentAccount = account;
    }

    public static void clear() {
        currentAccount = null;
    }

    public static String getUsername() {
        return currentAccount != null ? currentAccount.getUsername() : null;
    }

    public static String getRole() {
        return currentAccount != null ? currentAccount.getRole() : null;
    }
}