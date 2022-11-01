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
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @author jassuncao
 *
 */
public class DefaultLogHelper implements LogHelper {
    
    private static final String LOG_SERVICE_NAME = "org.osgi.service.log.LogService";

    private final int logLevel;

    @SuppressWarnings("rawtypes")
    private final ServiceTracker logTracker;

    public DefaultLogHelper(int logLevel, BundleContext bundleContext) {
        this.logLevel = logLevel;
        logTracker = new ServiceTracker<>(bundleContext, LOG_SERVICE_NAME, null);
        logTracker.open();
    }
    
    public void stop() {
        if(logTracker!=null) {
            logTracker.close();
        }
    }

    @Override
    public void log(int level, String message) {

        log(level, message, null);
    }

    @Override
    public void log(int level, String message, Throwable t) {
        // log using the LogService if available
        Object log = logTracker.getService();
        if (log != null) {
            ((LogService) log).log(level, message, t);
            return;
        }

        // Otherwise, only log if more serious than the configured level
        if (isLogEnabled(level)) {
            String code;
            switch (level) {
                case LogService.LOG_INFO:
                    code = "*INFO *";
                    break;

                case LogService.LOG_WARNING:
                    code = "*WARN *";
                    break;

                case LogService.LOG_ERROR:
                    code = "*ERROR*";
                    break;

                case LogService.LOG_DEBUG:
                default:
                    code = "*DEBUG*";
            }

            System.err.println(code + " " + message);
            if (t != null) {
                t.printStackTrace(System.err);
            }
        }
    }

    @Override
    public boolean isLogEnabled(int level) {
        return level <= logLevel;
    }

}
