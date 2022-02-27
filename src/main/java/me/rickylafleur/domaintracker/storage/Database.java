package me.rickylafleur.domaintracker.storage;

import java.sql.SQLException;

public interface Database {

    void connect() throws SQLException;
    void disconnect();
    void createTable();
    boolean isConnected();

}