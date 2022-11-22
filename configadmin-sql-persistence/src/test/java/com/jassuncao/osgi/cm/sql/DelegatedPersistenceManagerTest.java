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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.felix.cm.PersistenceManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;


public class DelegatedPersistenceManagerTest {
   
    private static final String COM_JASSUNCAO_PID = "com.jassuncao.pid";
    private static final String COM_ACME_PID = "com.acme.pid";

    @Rule 
    public MockitoRule rule = MockitoJUnit.rule().silent();
   
    @Mock
    PersistenceManager primaryManager;
    
    @Mock
    PersistenceManager secondaryManager;
    
    DelegatedPersistenceManager underTest;
    
    
    @Before
    public void setUp() throws Exception {
        Pattern primaryPids = Pattern.compile("^com\\.jassuncao\\..*");
        underTest = new DelegatedPersistenceManager(primaryManager, secondaryManager, primaryPids);
    }
    
    @Test
    public void testStore() throws IOException {
        Dictionary<String,Object> properties1 = new Hashtable<>();
        Dictionary<String,Object> properties2 = new Hashtable<>();
        underTest.store(COM_JASSUNCAO_PID, properties1);
        underTest.store(COM_ACME_PID, properties2);
        verify(primaryManager).store(COM_JASSUNCAO_PID, properties1);
        verify(secondaryManager).store(COM_ACME_PID, properties2);
    }
    
    @Test
    public void testLoad() throws IOException {
        Dictionary<String,Object> properties1 = new Hashtable<>();
        Dictionary<String,Object> properties2 = new Hashtable<>();
        when(primaryManager.exists(COM_JASSUNCAO_PID)).thenReturn(Boolean.TRUE);
        when(secondaryManager.exists(COM_ACME_PID)).thenReturn(Boolean.TRUE);
        
        when(primaryManager.load(COM_JASSUNCAO_PID)).thenReturn(properties1);
        when(secondaryManager.load(COM_ACME_PID)).thenReturn(properties2);
        assertEquals(properties1, underTest.load(COM_JASSUNCAO_PID));
        assertEquals(properties2, underTest.load(COM_ACME_PID));
    }
    
    @Test
    public void testGetDictionaries() throws IOException {
        when(primaryManager.getDictionaries()).thenReturn(toEnumerator(Collections.singleton("pid1")));
        when(secondaryManager.getDictionaries()).thenReturn(toEnumerator(Collections.singleton("pid2")));
        Enumeration<Object> dictionaries = underTest.getDictionaries();
        assertEquals("pid1", dictionaries.nextElement());
        assertEquals("pid2", dictionaries.nextElement());
        assertFalse(dictionaries.hasMoreElements());
    }
    
    private <E> Enumeration<E> toEnumerator(Collection<E> col){
        Iterator<E> it = col.iterator();
        return new Enumeration<E>() {

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public E nextElement() {
                return it.next();
            }
        };
    }

}
