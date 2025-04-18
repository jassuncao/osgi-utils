package com.jassuncao.osgi.cm.command;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.cm.json.ConfigurationResource;
import org.apache.felix.cm.json.ConfigurationWriter;
import org.apache.felix.cm.json.Configurations;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@Command(scope = "config", name = "export", description = "Exports configurations to a file")
@Service
public class ConfigExportCommand implements Action {
    
    @Reference
    Session session;
    
    @Reference
    ConfigurationAdmin configurationAdmin;

    @Argument(index = 0, name = "exportFile", description = "Name for the file where configurations are exported. Example, configs.json", required = true, multiValued = false)
    String exportFile;
    
    @Argument(index = 1, name = "filter", description = "Filter in LDAP syntax. Example: \"(service.pid=org.apache.karaf.log)\"", required = false, multiValued = false)
    String filter;
    
    @Option(name = "-f", aliases="--force",  description = "Overwrite an existing file", required = false, multiValued = false)
    boolean overwrite = false;
    
    @Override
    public Object execute() throws Exception {
        //If the filter doesn't seem to be an LDAP filter (no parentheses), we create a filter for the service.pid property
        if (filter != null &&  !filter.matches("\\s*\\(.*\\)\\s*$")) {
            filter = "(service.pid=" + filter + ")";
        }        
        
        Configuration[] configs = configurationAdmin.listConfigurations(filter);
        if(configs == null) {
            session.getConsole().println("No configurations found for filter: " + filter);
            return -1;
        }
        Path targetPath = Paths.get(exportFile);
        if (Files.exists(targetPath) && !overwrite) {
            session.getConsole().println("Output file already exists: " + exportFile);
            return -1;
        }
        
        OpenOption options = overwrite ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE_NEW;
        try(BufferedWriter writer = Files.newBufferedWriter(targetPath, options)){
            ConfigurationWriter configWriter = Configurations.buildWriter().build(writer);
            ConfigurationResource resource = new ConfigurationResource();
            resource.getProperties().put(":configurator:resource-version" , 1);
            resource.getProperties().put(":configurator:version" , "1.0");
            resource.getProperties().put(":configurator:symbolic-name" , "n/a"); 
            resource.getProperties().put(":export:date" , Instant.now().toString());    
            for (Configuration cfg : configs) {
                Hashtable<String, Object> copy = new Hashtable<>();
                Dictionary<String, Object> properties = cfg.getProperties();
                for (Enumeration<String> keys = properties.keys(); keys.hasMoreElements();) {
                    String key = keys.nextElement();
                    if(!key.equals("felix.fileinstall.filename")) {
                        copy.put(key, properties.get(key));
                    }
                }
                resource.getConfigurations().put(cfg.getPid(), copy);
            }
            configWriter.writeConfigurationResource(resource);
        }
        return null;
    }
    
}
