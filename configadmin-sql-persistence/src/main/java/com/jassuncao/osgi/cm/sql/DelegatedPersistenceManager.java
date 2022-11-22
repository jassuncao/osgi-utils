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
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.apache.felix.cm.PersistenceManager;

public class DelegatedPersistenceManager implements PersistenceManager {
    
    private final PersistenceManager primaryManager;
    private final PersistenceManager secondaryManager;
    private final Pattern primaryPids;

    public DelegatedPersistenceManager(PersistenceManager primaryManager, PersistenceManager secondaryManager, Pattern primaryPids) {
        this.primaryManager = primaryManager;
        this.secondaryManager = secondaryManager;
        this.primaryPids = primaryPids;
    }

    @Override
    public void delete(String pid) throws IOException {
        this.primaryManager.delete(pid);
        this.secondaryManager.delete(pid);
    }

    @Override
    public boolean exists(String pid) {
        return this.primaryManager.exists(pid) || this.secondaryManager.exists(pid);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enumeration<Object> getDictionaries() throws IOException {
        Enumeration<Object> primary = this.primaryManager.getDictionaries();
        Enumeration<Object> secondary = this.secondaryManager.getDictionaries();
        return new DualEnumerationWrapper(primary, secondary);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Dictionary<String,Object> load(String pid) throws IOException {
        if(this.primaryManager.exists(pid)) {
            return this.primaryManager.load(pid);
        }
        return this.secondaryManager.load(pid);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void store(String pid, Dictionary properties) throws IOException {
        if(primaryPids.matcher(pid).matches()) {
            primaryManager.store(pid, properties);
        }
        else {
            secondaryManager.store(pid, properties); 
        }
    }
    
    private static class DualEnumerationWrapper implements Enumeration<Object>  {
        
        private Enumeration<Object> primaryProperties;
        private Enumeration<Object> secondaryProperties;
        
        DualEnumerationWrapper(Enumeration<Object> primaryProperties, Enumeration<Object> secondaryProperties){
            this.primaryProperties = primaryProperties;
            this.secondaryProperties = secondaryProperties;
        }

        @Override
        public synchronized boolean hasMoreElements() {
            if(primaryProperties!=null) {
                if(primaryProperties.hasMoreElements()) {
                    return true;
                }
                primaryProperties = null;
            }
            if(secondaryProperties!=null) {
                if(secondaryProperties.hasMoreElements()) {
                    return true;
                }
                secondaryProperties = null;
            }
            return false;
        }

        @Override
        public synchronized Object nextElement() {
            if(primaryProperties!=null && primaryProperties.hasMoreElements()) {
                return primaryProperties.nextElement();
            }
            if(secondaryProperties!=null && secondaryProperties.hasMoreElements()) {
                return secondaryProperties.nextElement();
            }
            throw new NoSuchElementException();
        }
        
    }

}
