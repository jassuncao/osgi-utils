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

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.felix.cm.PersistenceManager;
import org.osgi.annotation.bundle.Header;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.jdbc.DataSourceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author jassuncao
 *
 */
@Header(name = Constants.BUNDLE_ACTIVATOR, value = "${@class}")
public class Activator implements BundleActivator {
    /**
     * Notes: Can't use pax-jdbc to create a datasource because it depends on Configuration Admin
     */
    private static final Integer SERVICE_RANKING_VALUE = 5;

    private static final Object SQL_PM_NAME = "sql";
    private static final Object DELEGATED_PM_NAME = "delegated";

    private ServiceTracker<DataSourceFactory, ServiceRegistration<PersistenceManager>> dataSourceFactoryTracker;

    private DefaultLogHelper logHelper;

    private DelegatingPersistenceTracker delegatingPersistenceTracker;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        SystemPropertiesHelper properties = new SystemPropertiesHelper(bundleContext);
        int logLevel = properties.getLogLevel();

        logHelper = new DefaultLogHelper(logLevel, bundleContext);
        
        if(isNotEmpty(properties.getDriverName()) && isNotEmpty(properties.getJdbcUrl())) {
            logHelper.log(LogService.LOG_INFO, "Creating DatasourceServiceTracker");
            dataSourceFactoryTracker = new DatasourceServiceTracker(bundleContext, properties, logHelper);
            dataSourceFactoryTracker.open();        
        }
        
        if(isNotEmpty(properties.getDelegatedPrimary()) && isNotEmpty(properties.getDelegatedSecondary()) && isNotEmpty(properties.getDelegatedPrimaryPids())) {
            logHelper.log(LogService.LOG_INFO, "Creating DelegatingPersistenceTracker");
            
            delegatingPersistenceTracker = new DelegatingPersistenceTracker(bundleContext, properties, logHelper);
            delegatingPersistenceTracker.open();
        }
        
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (dataSourceFactoryTracker != null) {
            dataSourceFactoryTracker.close();
        }
        if(delegatingPersistenceTracker!=null) {
            delegatingPersistenceTracker.close();
        }
        if (logHelper != null) {
            logHelper.stop();
        }
    }

    private static class DatasourceServiceTracker extends ServiceTracker<DataSourceFactory, ServiceRegistration<PersistenceManager>> {

        private final SystemPropertiesHelper propertiesHelper;
        private final LogHelper logHelper;

        public DatasourceServiceTracker(BundleContext context, SystemPropertiesHelper propertiesHelper, LogHelper logHelper) {
            super(context, DataSourceFactory.class, null);
            this.propertiesHelper = propertiesHelper;
            this.logHelper = logHelper;
        }

        @Override
        public ServiceRegistration<PersistenceManager> addingService(ServiceReference<DataSourceFactory> reference) {
            String refDriverName = (String) reference.getProperty(DataSourceFactory.OSGI_JDBC_DRIVER_NAME);
            if (!propertiesHelper.getDriverName().equals(refDriverName)) {
                logHelper.log(LogService.LOG_DEBUG, "Ignoring DataSourceFactory for driver " + refDriverName, null);
                return null;
            }
            DataSourceFactory dataSourceFactory = context.getService(reference);
            DataSource dataSource;
            try {
                dataSource = createDataSource(dataSourceFactory);
                logHelper.log(LogService.LOG_INFO, "Creating service DatabasePersistenceManager", null);
                String dbSchema = propertiesHelper.getSchema();
                String tableName = propertiesHelper.getTableName();
                DatabasePersistenceManager persistenceManager = new DatabasePersistenceManager(dataSource, logHelper,
                        tableName, dbSchema);
                persistenceManager.init();
                Hashtable<String, Object> props = new Hashtable<>();
                props.put(Constants.SERVICE_DESCRIPTION, "Configuration Persistence Manager - Database");
                props.put(Constants.SERVICE_RANKING, SERVICE_RANKING_VALUE);
                props.put(PersistenceManager.PROPERTY_NAME, SQL_PM_NAME);
                logHelper.log(LogService.LOG_INFO, "Registering service DatabasePersistenceManager", null);
                return context.registerService(PersistenceManager.class, persistenceManager, props);
            } catch (SQLException ex) {
                logHelper.log(LogService.LOG_ERROR, "Failed to create DataSource ", ex);
            }
            return null;
        }

        @Override
        public void removedService(ServiceReference<DataSourceFactory> reference, ServiceRegistration<PersistenceManager> service) {
            if (service != null) {
                service.unregister();
                context.ungetService(reference);
            }
        }

        private DataSource createDataSource(DataSourceFactory dataSourceFactory) throws SQLException {
            Properties props = new Properties();
            props.put(DataSourceFactory.JDBC_URL, propertiesHelper.getJdbcUrl());
            props.put(DataSourceFactory.JDBC_USER, propertiesHelper.getJdbcUser());
            props.put(DataSourceFactory.JDBC_PASSWORD, propertiesHelper.getJdbcPassword());

            StringBuilder builder = new StringBuilder();
            props.forEach((k, v) -> builder.append(k).append("=").append(v).append('\n'));
            logHelper.log(LogService.LOG_DEBUG,
                    "Creating datasource for connection properties:\n" + builder, null);

            return dataSourceFactory.createDataSource(props);
        }

    }
    
    private static class DelegatingPersistenceTracker extends ServiceTracker<PersistenceManager, PersistenceManager> {

        private final String primaryName;
        private final String secondaryName;
        private final LogHelper logHelper;
        private final Pattern primaryPids;
        private PersistenceManager primaryManager;
        private PersistenceManager secondaryManager;
        private ServiceRegistration<PersistenceManager> registration;

        public DelegatingPersistenceTracker(BundleContext context,  SystemPropertiesHelper propertiesHelper, LogHelper logHelper) {
            super(context, PersistenceManager.class, null);
            this.primaryName = propertiesHelper.getDelegatedPrimary();
            this.secondaryName = propertiesHelper.getDelegatedSecondary();
            this.primaryPids = Pattern.compile(propertiesHelper.getDelegatedPrimaryPids());
            logHelper.log(LogService.LOG_DEBUG, "Primary PersistenceManager "+primaryName+" will hold PIDs matching: "+propertiesHelper.getDelegatedPrimaryPids(), null);
            this.logHelper = logHelper;
        }
        
        @Override
        public PersistenceManager addingService(ServiceReference<PersistenceManager> reference) {
            final String managerName = (String) reference.getProperty(PersistenceManager.PROPERTY_NAME);
            final PersistenceManager service;
            if (primaryManager == null && primaryName.equals(managerName)) {
                logHelper.log(LogService.LOG_DEBUG, "Primary "+primaryName+" PersistenceManager added", null);
                primaryManager = service = context.getService(reference);
            }
            else if (secondaryManager == null && secondaryName.equals(managerName)) {
                logHelper.log(LogService.LOG_DEBUG, "Secondary "+primaryName+" PersistenceManager added", null);
                secondaryManager = service = context.getService(reference);
            }
            else {
                return null;
            }
            
            if(primaryManager!=null && secondaryManager!=null) {
                DelegatedPersistenceManager delegated = new DelegatedPersistenceManager(primaryManager, secondaryManager, primaryPids);
                Hashtable<String, Object> props = new Hashtable<>();
                props.put(Constants.SERVICE_DESCRIPTION, "Configuration Persistence Manager - Delegated");
                props.put(Constants.SERVICE_RANKING, SERVICE_RANKING_VALUE+1);
                props.put(PersistenceManager.PROPERTY_NAME, DELEGATED_PM_NAME);
                logHelper.log(LogService.LOG_INFO, "Registering service DelegatedPersistenceManager", null);
                this.registration = context.registerService(PersistenceManager.class, delegated, props);
            }
            return service;
        }
        
        @Override
        public void removedService(ServiceReference<PersistenceManager> reference, PersistenceManager service) {
            if(this.registration!=null) {
                this.registration.unregister();
                this.registration = null;
            }
            if(service == primaryManager) {
                primaryManager = null;
            }
            else if(service == secondaryManager) {
                secondaryManager = null;
            }
            context.ungetService(reference);
        }
        
    }
    
    private static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    private static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
