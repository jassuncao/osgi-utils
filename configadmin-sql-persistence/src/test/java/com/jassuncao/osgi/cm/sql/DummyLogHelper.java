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

import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyLogHelper implements LogHelper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyLogHelper.class);

    @Override
    public void log(int level, String message, Throwable t) {
        switch (level) {
            case LogService.LOG_DEBUG:
                LOGGER.debug(message, t);
                break;
            case LogService.LOG_INFO:
                LOGGER.info(message, t);
                break;
            case LogService.LOG_WARNING:
                LOGGER.warn(message, t);
                break;
            default:
                LOGGER.error(message, t);
                break;
        }
    }

    @Override
    public void log(int level, String message) {
        log(level, message, null);
    }

    @Override
    public boolean isLogEnabled(int level) {
        return true;
    }

}
