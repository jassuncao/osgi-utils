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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.osgi.framework.BundleContext;

/**
 * @author jassuncao
 *
 */
public class SystemPropertiesHelper {
    
    private static final String PREFIX = "com.jassuncao.osgi.cm.sql.";

    public static final String PROPERTY_DRIVER_NAME = PREFIX+"driver.name";

    public static final String PROPERTY_JDBC_URL = PREFIX+"url";

    public static final String PROPERTY_JDBC_USER = PREFIX+"user";

    public static final String PROPERTY_JDBC_PASSWORD = PREFIX+"password"; 

    public static final String PROPERTY_TABLE = PREFIX+"table";
    
    public static final String PROPERTY_SCHEMA = PREFIX+"schema";
    
    public static final String PROPERTY_LOG_LEVEL = PREFIX+"loglevel";
    
    public static final String PROPERTY_PAX_CONFIG = PREFIX+"paxconfig";
    
    private final BundleContext bundleContext;
    private final Properties paxJdbcProperties = new Properties();
    
    SystemPropertiesHelper(BundleContext bundleContext) throws IOException{
        this.bundleContext = bundleContext;
        String paxCfgFile = this.bundleContext.getProperty(PROPERTY_PAX_CONFIG);
        if(paxCfgFile!=null && !paxCfgFile.isEmpty()) {
            try(FileInputStream inStream = new FileInputStream(paxCfgFile)){
                paxJdbcProperties.load(inStream);
            }
        }
    }
    
    public int getLogLevel() {
        return getIntValue(PROPERTY_LOG_LEVEL, 2);
    }

    public String getDriverName() {
        return getOwnValueOrPax(PROPERTY_DRIVER_NAME,"osgi.jdbc.driver.name", null);
    }

    public String getJdbcUrl() {
        return getOwnValueOrPax(PROPERTY_JDBC_URL, "url", null);
    }

    public String getJdbcUser() {
        return getOwnValueOrPax(PROPERTY_JDBC_USER, "user", null);
    }

    public String getJdbcPassword() {
        return getOwnValueOrPax(PROPERTY_JDBC_PASSWORD,"password", null);
    }

    public String getTableName() {
        return getOwnValue(PROPERTY_TABLE, "osgi_config");
    }

    public String getSchema() {
        return getOwnValue(PROPERTY_SCHEMA, null);
    }
    
    public String getPaxJdbcConfig() {
        return getOwnValue(PROPERTY_PAX_CONFIG, null);
    }
    
    private String getOwnValueOrPax(String key, String paxFallbackKey, String defaultValue) {
        String value = bundleContext.getProperty(key);
        if(value==null) {
            value = paxJdbcProperties.getProperty(paxFallbackKey, defaultValue);
        }
        return value;
    }

    private String getOwnValue(String key, String defaultValue) {
        String value = bundleContext.getProperty(key);
        return value !=null ? value : defaultValue;
    }
    
    private int getIntValue(String key, int defaultValue) {
        String value = bundleContext.getProperty(key);
        return value !=null ? Integer.parseInt(value) : defaultValue;
    }
    
}
