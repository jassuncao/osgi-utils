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

    public static final String PROPERTY_DATASOURCE_NAME = PREFIX+"dataSourceName";

    public static final String PROPERTY_TABLE = PREFIX+"table";
    
    public static final String PROPERTY_SCHEMA = PREFIX+"schema";
    
    public static final String PROPERTY_LOG_LEVEL = PREFIX+"loglevel";
    
    private final BundleContext bundleContext;
    
    SystemPropertiesHelper(BundleContext bundleContext){
        this.bundleContext = bundleContext;
    }
    
    public int getLogLevel() {
        return getIntValue(PROPERTY_LOG_LEVEL, 2);
    }

    public String getDriverName() {
        return getValue(PROPERTY_DRIVER_NAME, null);
    }

    public String getJdbcUrl() {
        return getValue(PROPERTY_JDBC_URL, null);
    }

    public String getJdbcUser() {
        return getValue(PROPERTY_JDBC_USER, null);
    }

    public String getJdbcPassword() {
        return getValue(PROPERTY_JDBC_PASSWORD, null);
    }

    public String getDataSourceName() {
        return getValue(PROPERTY_DATASOURCE_NAME, "cm_sql");
    }
    
    public String getTableName() {
        return getValue(PROPERTY_TABLE, "osgi_config");
    }

    public String getSchema() {
        return getValue(PROPERTY_SCHEMA, null);
    }

    private String getValue(String key, String defaultValue) {
        String value = bundleContext.getProperty(key);
        return value !=null ? value : defaultValue;
    }
    
    private int getIntValue(String key, int defaultValue) {
        String value = bundleContext.getProperty(key);
        return value !=null ? Integer.parseInt(value) : defaultValue;
    }
    
}
