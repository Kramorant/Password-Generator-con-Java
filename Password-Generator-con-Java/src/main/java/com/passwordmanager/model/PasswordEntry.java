package com.passwordmanager.model;

public class PasswordEntry {

    private int id;
    private String title;
    private String username;
    private String encrypted;
    private String iv;
    private String createdAt;

    public PasswordEntry(int id, String title, String username,
                         String encrypted, String iv, String createdAt) {
        this.id        = id;
        this.title     = title;
        this.username  = username;
        this.encrypted = encrypted;
        this.iv        = iv;
        this.createdAt = createdAt;
    }

    // Constructor sin ID para nuevas entradas (el ID lo asigna SQLite)
    public PasswordEntry(String title, String username, String encrypted, String iv) {
        this(0, title, username, encrypted, iv, null);
    }

    public int getId()            { return id; }
    public String getTitle()      { return title; }
    public String getUsername()   { return username; }
    public String getEncrypted()  { return encrypted; }
    public String getIv()         { return iv; }
    public String getCreatedAt()  { return createdAt; }

    public void setTitle(String title)         { this.title = title; }
    public void setUsername(String username)   { this.username = username; }
    public void setEncrypted(String encrypted) { this.encrypted = encrypted; }
    public void setIv(String iv)               { this.iv = iv; }
}