/*
 * Copyright (C) 2022 Joao Assuncao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jassuncao.osgi.cm.sql;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.felix.cm.PersistenceManager;
import org.osgi.service.log.LogService;

/**
 * @author jassuncao
 *
 */
public class DatabasePersistenceManager implements PersistenceManager {


    private static final String ERROR_CLOSING_CONNECTION = "Error closing connection";

    private static final String ERROR_SAVING_PROPERTIES_TO_DATABASE = "Error saving properties to database";

    private static final String ERROR_DELETING_PROPERTIES_FROM_DATABASE = "Error deleting properties from database";

    private static final String ERROR_LOADING_PROPERTIES_FROM_DATABASE = "Error loading properties from database";

    private final DataSource datasource;

    private final LogHelper logHelper;

    private final String qualifiedTable;
    
    private final String tableName;

    private final String schema;

    public DatabasePersistenceManager(DataSource datasource, LogHelper logHelper, String tableName, String schema) {
        this.datasource = datasource;
        this.logHelper = logHelper;
        this.tableName = tableName;
        this.schema = schema;
        this.qualifiedTable = schema == null || schema.trim().isEmpty() ? tableName : schema + "." + tableName;
    }

    public void init() {
        Connection connection = null;
        try {
            connection = datasource.getConnection();
            boolean exists = tableExistsGuess(connection);
            if (!exists) {
                createTable(connection);
            }
        } catch (SQLException ex) {
            logHelper.log(LogService.LOG_WARNING, "Failed to create config table. Probably it alread exists", ex);
        } finally {
            closeConnection(connection);
        }
    }

    private boolean tableExistsGuess(Connection connection) {
        boolean res = false;
        logHelper.log(LogService.LOG_DEBUG, "Checking if config table (" + qualifiedTable + ") exists", null);
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try(ResultSet tables = metaData.getTables(null, schema, tableName, null)){
                res = tables.next();
            }
        } catch (SQLException ex) {
            logHelper.log(LogService.LOG_DEBUG, "Error while checking if table exists", ex);
        }
        logHelper.log(LogService.LOG_DEBUG, "Table exists = " + res, null);
        return res;
    }

    private void createTable(Connection connection) {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            String createStmt = "CREATE TABLE " + qualifiedTable
                    + " (pid VARCHAR NOT NULL, prop_name VARCHAR NOT NULL, prop_type VARCHAR NOT NULL, prop_value VARCHAR, CONSTRAINT pk_"+tableName+" PRIMARY KEY (pid, prop_name))";
            statement.executeUpdate(createStmt);
        } catch (SQLException ex) {
            logHelper.log(LogService.LOG_WARNING, "Failed to create config table. Probably it already exists", ex);
        } finally {
            closeStatement(statement);
        }
    }

    @Override
    public boolean exists(String pid) {
        logHelper.log(LogService.LOG_DEBUG, "Checking for a configuration for PID " + pid, null);
        Connection connection = null;
        PreparedStatement countStatement = null;
        boolean res = false;
        try {
            connection = datasource.getConnection();
            countStatement = connection
                    .prepareStatement("SELECT EXISTS(SELECT 1 FROM " + qualifiedTable + " WHERE pid = ?)");
            countStatement.setString(1, pid);
            ResultSet rs = countStatement.executeQuery();
            if (rs.next()) {
                res = rs.getBoolean(1);
            }
        } catch (SQLException ex) {
            logHelper.log(LogService.LOG_ERROR, ERROR_LOADING_PROPERTIES_FROM_DATABASE, ex);
        } finally {
            closeStatement(countStatement);
            closeConnection(connection);
        }
        return res;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Dictionary load(String pid) throws IOException {
        logHelper.log(LogService.LOG_DEBUG, "Loading configuration for PID " + pid, null);
        Connection connection = null;
        Dictionary<String, Object> properties;
        try {
            connection = datasource.getConnection();
            properties = loadProperties(connection, pid);
        } catch (SQLException ex) {
            logHelper.log(LogService.LOG_ERROR, ERROR_LOADING_PROPERTIES_FROM_DATABASE, ex);
            throw new IOException(ERROR_LOADING_PROPERTIES_FROM_DATABASE, ex);
        } finally {
            closeConnection(connection);
        }
        return properties;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.felix.cm.PersistenceManager#getDictionaries()
     */
    @SuppressWarnings("rawtypes")
    @Override
    public Enumeration getDictionaries() throws IOException {
        Connection connection = null;
        Map<String, Dictionary<String, Object>> dictionaries;
        try {
            connection = datasource.getConnection();
            dictionaries = loadAllProperties(connection);
        } catch (SQLException ex) {
            logHelper.log(LogService.LOG_ERROR, ERROR_LOADING_PROPERTIES_FROM_DATABASE, ex);
            throw new IOException(ERROR_LOADING_PROPERTIES_FROM_DATABASE, ex);
        } finally {
            closeConnection(connection);
        }
        return new DictionariesEnumeration(dictionaries.values());
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    @Override
    public void store(String pid, Dictionary properties) throws IOException {
        logHelper.log(LogService.LOG_DEBUG, "Storing configuration for PID " + pid, null);
        Connection connection = null;
        try {
            connection = datasource.getConnection();
            storeProperties(connection, pid, properties);
        } catch (SQLException ex) {
            logHelper.log(LogService.LOG_ERROR, ERROR_SAVING_PROPERTIES_TO_DATABASE, ex);
            throw new IOException(ERROR_SAVING_PROPERTIES_TO_DATABASE, ex);
        }
        finally {
            closeConnection(connection);
        }
    }

    @Override
    public void delete(String pid) throws IOException {
        logHelper.log(LogService.LOG_DEBUG, "Deleting configuration for PID " + pid, null);
        Connection connection = null;
        try {
            connection = datasource.getConnection();
            Dictionary<String, Object> properties = new Hashtable<>(0); // NOSONAR
            storeProperties(connection, pid, properties);
        } catch (SQLException ex) {
            logHelper.log(LogService.LOG_ERROR, ERROR_DELETING_PROPERTIES_FROM_DATABASE, ex);
            throw new IOException(ERROR_DELETING_PROPERTIES_FROM_DATABASE, ex);
        } finally {
            closeConnection(connection);
        }
    }

    private void storeProperties(Connection connection, String pid, Dictionary<String, Object> properties)
            throws SQLException {

        PreparedStatement deleteStatement = null;
        PreparedStatement insertPropertyStatement = null;
        boolean autoCommit = connection.getAutoCommit();
        int previousLevel = connection.getTransactionIsolation();
        try {
            connection.setAutoCommit(false);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            deleteStatement = connection.prepareStatement("DELETE FROM " + qualifiedTable + " WHERE pid = ?");
            deleteStatement.setString(1, pid);
            deleteStatement.executeUpdate();

            insertPropertyStatement = connection
                    .prepareStatement("INSERT INTO " + qualifiedTable + " (pid, prop_name, prop_type, prop_value) VALUES (?, ?, ?, ?)");
            insertPropertyStatement.setString(1, pid);

            for (Enumeration<?> ce = properties.keys(); ce.hasMoreElements();) {
                String name = (String) ce.nextElement();
                Object value = properties.get(name);
                String typeCode = PropertyConverter.getCodeForType(value);
                String valueString = PropertyConverter.convertToString(value);
                if (typeCode != null && valueString != null) {
                    insertPropertyStatement.setString(2, name);
                    insertPropertyStatement.setString(3, typeCode);
                    insertPropertyStatement.setString(4, valueString);
                    insertPropertyStatement.executeUpdate();
                } else {
                    logHelper.log(LogService.LOG_WARNING, "Failed to convert property " + name, null);
                }
            }
            connection.commit();

        } catch (SQLException ex) {
            logHelper.log(LogService.LOG_ERROR, ERROR_SAVING_PROPERTIES_TO_DATABASE, ex);
            try {
                connection.rollback();
            } catch (SQLException e) {
                logHelper.log(LogService.LOG_WARNING, "Failed to rollback transaction", e);
            }
        } finally {
            closeStatement(deleteStatement);
            closeStatement(insertPropertyStatement);
            connection.setAutoCommit(autoCommit);
            connection.setTransactionIsolation(previousLevel);
        }
    }

    public Dictionary<String, Object> loadProperties(Connection connection, String pid) throws SQLException {
        PreparedStatement selectStatement = null;
        Hashtable<String, Object> dictionary = new Hashtable<>(); // NOSONAR
        int previousLevel = connection.getTransactionIsolation();
        try {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            selectStatement = connection
                    .prepareStatement("SELECT prop_name, prop_type, prop_value FROM " + qualifiedTable + " WHERE pid = ?");
            selectStatement.setString(1, pid);
            ResultSet rs = selectStatement.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String type = rs.getString(2);
                String valueAsString = rs.getString(3);
                Object value = PropertyConverter.convertFromString(type, valueAsString);
                dictionary.put(name, value);
            }
        } finally {
            closeStatement(selectStatement);
            connection.setTransactionIsolation(previousLevel);
        }
        return dictionary;
    }

    public Map<String, Dictionary<String, Object>> loadAllProperties(Connection connection) throws SQLException {
        PreparedStatement selectStatement = null;
        Map<String, Dictionary<String, Object>> dictionaries = new HashMap<>();
        int previousLevel = connection.getTransactionIsolation();
        try {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            selectStatement = connection.prepareStatement("SELECT pid, prop_name, prop_type, prop_value FROM " + qualifiedTable);
            ResultSet rs = selectStatement.executeQuery();
            while (rs.next()) {
                String pid = rs.getString(1);
                String name = rs.getString(2);
                String type = rs.getString(3);
                String valueAsString = rs.getString(4);
                Object value = PropertyConverter.convertFromString(type, valueAsString);
                Dictionary<String, Object> dictionary = dictionaries.get(pid);
                if (dictionary == null) {
                    dictionary = new Hashtable<>();// NOSONAR
                    dictionaries.put(pid, dictionary);
                }
                dictionary.put(name, value);
            }
        } finally {
            closeStatement(selectStatement);
            connection.setTransactionIsolation(previousLevel);
        }
        return dictionaries;
    }

    private static class DictionariesEnumeration implements Enumeration<Dictionary<String, Object>> {

        private final Iterator<Dictionary<String, Object>> iterator;

        public DictionariesEnumeration(Collection<Dictionary<String, Object>> dictionaries) {
            this.iterator = dictionaries.iterator();
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public Dictionary<String, Object> nextElement() {
            return iterator.next();
        }
    }

    private void closeConnection(Connection connection) {

        if (connection != null) {
            try {
                connection.close();
            }
            catch (SQLException ex) {
                logHelper.log(LogService.LOG_DEBUG, ERROR_CLOSING_CONNECTION, ex);
            }
        }
    }

    private void closeStatement(Statement statement) {
        try {
            if (statement != null) {
                statement.close();
            }
        }
        catch (SQLException ex) { // NOSONAR
            logHelper.log(LogService.LOG_DEBUG, "Error closing Prepared Statement", ex);
        }
    }

}
