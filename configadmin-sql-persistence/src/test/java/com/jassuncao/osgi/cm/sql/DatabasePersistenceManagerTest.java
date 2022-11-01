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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import javax.sql.DataSource;

import org.dbunit.DBTestCase;
import org.dbunit.PropertiesBasedJdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2DataTypeFactory;
import org.h2.tools.RunScript;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/**
 * @author jassuncao
 *
 */
public class DatabasePersistenceManagerTest extends DBTestCase {

    private static final String JDBC_DRIVER = org.h2.Driver.class.getName();

    private static final String JDBC_URL = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1";

    private static final String JDBC_USER = "sa";

    private static final String JDBC_PASSWORD = "";

    private DatabasePersistenceManager underTest;
    
    static {
        try {
            RunScript.execute(JDBC_URL, JDBC_USER, JDBC_PASSWORD, "classpath:/schema.sql", StandardCharsets.UTF_8, false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public DatabasePersistenceManagerTest() {
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_DRIVER_CLASS, JDBC_DRIVER);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_CONNECTION_URL, JDBC_URL);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_USERNAME, JDBC_USER);
        System.setProperty(PropertiesBasedJdbcDatabaseTester.DBUNIT_PASSWORD, JDBC_PASSWORD);
    }

    
    DataSource datasource;
   

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        datasource = mock(DataSource.class);
        when(datasource.getConnection()).then((Answer<Connection>) invocation -> getConnection().getConnection());
        LogHelper logHelper = new DummyLogHelper();
        underTest = new DatabasePersistenceManager(datasource, logHelper, "osgi_config", null);
    }
    

    @Override
    protected void setUpDatabaseConfig(DatabaseConfig config) {
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new H2DataTypeFactory());
        config.setProperty(DatabaseConfig.PROPERTY_ESCAPE_PATTERN, "\"?\"");
    }

    @Override
    protected IDataSet getDataSet() throws Exception {
        return new FlatXmlDataSetBuilder()
                .build(DatabasePersistenceManagerTest.class.getResourceAsStream("/dataset.xml"));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testExists() {
        assertTrue(underTest.exists("pid1"));
    }

    @Test
    public void testLoad() throws IOException {
        Dictionary<?, ?> dict = underTest.load("pid1");
        assertEquals(3, dict.size());
    }

    @Test
    public void testGetDictionaries() throws IOException {

        Enumeration<?> dictionaries = underTest.getDictionaries();
        assertTrue(dictionaries.hasMoreElements());
        Dictionary<?, ?> dict = (Dictionary<?, ?>) dictionaries.nextElement();
        assertEquals(3, dict.size());
    }

    @Test
    public void testStore() throws IOException {
        Properties properties = new Properties();
        properties.put("key1", "x");
        properties.put("key2", "y");
        underTest.store("pid2", properties);

        Dictionary<?, ?> dict = underTest.load("pid2");
        assertEquals(2, dict.size());
    }
    
    @Test
    public void testConcurrentReadWrite() throws Exception {
        Semaphore beforeCommitSemaphore = new Semaphore(0);
        Semaphore releaseCommit = new  Semaphore(0);
        
        when(datasource.getConnection()).thenAnswer((Answer<Connection>) invocation -> {
            Connection connection = getConnection().getConnection();
            if(!Thread.currentThread().getName().equals("STORE")) {
                return connection;

            }
            Connection mockConnection = mock(Connection.class, AdditionalAnswers.delegatesTo(connection));
            Mockito.doAnswer((Answer<Void>) invocation1 -> {
                beforeCommitSemaphore.release();
                releaseCommit.acquire();
                connection.commit();
                return null;
            }).when(mockConnection).commit();
            return mockConnection;
        });
        Thread storeThread = new Thread("STORE") {
            @Override
            public void run() {
                Properties properties = new Properties();
                properties.put("key1", "x");
                properties.put("key2", "y");
                try {
                    underTest.store("pid1", properties);
                }
                catch (IOException ex) {
                    //
                }
            }
        };
        storeThread.start();
       
        beforeCommitSemaphore.acquire();
        Dictionary<?, ?> dict = underTest.load("pid1");
        releaseCommit.release();
        assertEquals(3, dict.size());
    }

    @Test
    public void testDelete() throws IOException {
        underTest.delete("pid1");
        assertFalse(underTest.exists("pid1"));
    }
}
