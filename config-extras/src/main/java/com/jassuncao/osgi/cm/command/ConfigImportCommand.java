package com.jassuncao.osgi.cm.command;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.cm.json.ConfigurationReader;
import org.apache.felix.cm.json.ConfigurationResource;
import org.apache.felix.cm.json.Configurations;
import org.apache.karaf.config.core.ConfigRepository;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.FileCompleter;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;

@Command(scope = "config", name = "import", description = "Imports configurations from a backup file")
@Service
public class ConfigImportCommand implements Action {

    @Reference
    Session session;
    
    //@Reference
    //ConfigurationAdmin configurationAdmin;
    
    @Reference
    ConfigRepository configRepository;
    
    @Completion(FileCompleter.class)
    @Argument(index = 0, name = "inputFile", description = "File from where configurations should be imported", required = true, multiValued = false)
    String inputFile;
    
    @Argument(index = 1, name = "filter", description = "Filters the configurations to import. A query in LDAP syntax. Example: \"(service.pid=org.apache.karaf.log)\"", required = false, multiValued = false)
    String filter;
    
    @Option(name = "-n", aliases="--no-clobber",  description = "Do not overwrite existing configurations", required = false, multiValued = false)
    boolean noClobber = false;
    
    @Option(name = "-d", aliases="--dry-run",  description = "Don't perform any change", required = false, multiValued = false)
    boolean dryRun = false;
    
    @Override
    public Object execute() throws Exception {

        Filter configFilter = null;
        if (filter != null) {
            if (!filter.matches("\\s*\\(.*\\)\\s*$")) {
                filter = "(service.pid=" + filter + ")";
            }
            configFilter = FrameworkUtil.createFilter(filter);
        }
        
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(inputFile))){
            ConfigurationReader configReader = Configurations.buildReader().verifyAsBundleResource(false).build(reader);
            ConfigurationResource resource = configReader.readConfigurationResource();
            Map<String, Hashtable<String, Object>> configurationsToImport = resource.getConfigurations();
            for (Entry<String, Hashtable<String, Object>> entry : configurationsToImport.entrySet()) {
                String pid = entry.getKey();
                if (configFilter == null || configFilter.match(configurationsToImport.get(pid))) {
                    final Hashtable<String, Object> properties = entry.getValue();
                    boolean exists = configRepository.exists(pid);
                    if(exists) {
                        updateExistingConfiguration(pid, properties);
                    }
                    else  {
                        createNewConfiguration(pid, properties);
                    }
                }
            }
        }
        return null;
    }
    
    private void createNewConfiguration(String pid, final Hashtable<String, Object> properties) throws IOException {
        String factoryPid = (String) properties.get(ConfigurationAdmin.SERVICE_FACTORYPID);
        if(factoryPid != null) {
            if(!dryRun) {
                String name = pid.substring(factoryPid.length()+1);
                properties.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
                properties.remove(Constants.SERVICE_PID);
                configRepository.createFactoryConfiguration(factoryPid, name, properties);                
                session.getConsole().println("Created factory configuration " + factoryPid);
            }
            else {
                session.getConsole().println("Would create factory configuration " + factoryPid);
            }
        } else {
            if(!dryRun) {
                properties.remove(Constants.SERVICE_PID);
                configRepository.update(pid, properties);
                session.getConsole().println("Created configuration " + pid);
            }
            else {
                session.getConsole().println("Would create configuration " + pid);
            }
        }
    }
    
    private void updateExistingConfiguration(String pid, final Hashtable<String, Object> properties) throws IOException {
        if (noClobber) {
            session.getConsole().println("Configuration " + pid + " already exists and no clobber option is set. Skipping.");
            return;
        }
        if(!dryRun) {
            properties.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
            properties.remove(Constants.SERVICE_PID);
            configRepository.update(pid, properties);
            session.getConsole().println("Replaced configuration " + pid);
        } 
        else {
            session.getConsole().println("Would replace configuration " + pid);
        }
    }
    
    static boolean isMatch(String text, String pattern) {
        if (pattern == null) {
            return text == null;
        }
        if (text == null) {
            return false;
        }
        int textIdx = 0, patternIdx = 0, match = 0, starIdx = -1;       
        while (textIdx < text.length()){
            // advancing both indexes when (both characters match) or ('?' found in pattern)
            if (patternIdx < pattern.length()  && (pattern.charAt(patternIdx) == '?' || text.charAt(textIdx) == pattern.charAt(patternIdx))){
                textIdx++;
                patternIdx++;
            }
            // * found, only advancing pattern index
            else if (patternIdx < pattern.length() && pattern.charAt(patternIdx) == '*'){
                starIdx = patternIdx;
                match = textIdx;
                patternIdx++;
            }
            // last pattern index was *, advancing text index
            else if (starIdx != -1){
                patternIdx = starIdx + 1;
                match++;
                textIdx = match;
            }
            //current pattern pointer is not star, last patter pointer was not *
            //characters do not match
            else return false;
        }

        //check for remaining characters in pattern
        while (patternIdx < pattern.length() && pattern.charAt(patternIdx) == '*')
            patternIdx++;

        return patternIdx == pattern.length();
    }
    
    public static void main(String[] args) {

        String text = "aabbbccc";
        String pattern = "a";
        System.out.println(isMatch(text, pattern));
        System.out.println(isMatch(text, "aabbbccc"));
        System.out.println(isMatch(text, "aabbbccc"));
    }

}
