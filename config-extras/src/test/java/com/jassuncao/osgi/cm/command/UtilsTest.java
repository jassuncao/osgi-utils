package com.jassuncao.osgi.cm.command;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UtilsTest {

    @Test
    public void verifyWildcardMatcher() {
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.apache.karaf.*"));
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.apache.karaf.log"));
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.apache.karaf.???"));
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.apache.karaf.*"));
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.apache*"));
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.*.karaf.???"));
        
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.*"));            
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "*"));
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "*.*"));
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "*.*.*"));
        assertTrue(ConfigImportCommand.isMatch("org.apache.karaf.log", "*.*.*.*"));
        
        assertFalse(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.apache.karaf"));
        assertFalse(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.apache.karaf.log.*"));
        assertFalse(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.apache.karaf.*.*"));
        assertFalse(ConfigImportCommand.isMatch("org.apache.karaf.log", "org.apache.karaf.log.*"));
        
        assertFalse(ConfigImportCommand.isMatch("org.apache.karaf.log", null));
        assertFalse(ConfigImportCommand.isMatch("org.apache.karaf.log", "b"));
        
        
    }
}
