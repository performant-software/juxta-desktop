package edu.virginia.speclab.juxta.author.model.template;

import java.util.HashMap;

import edu.virginia.speclab.exceptions.FatalException;
import edu.virginia.speclab.exceptions.ReportedException;

/**
 * Singleton manager for juxta parse templates.
 * 
 * @author loufoster
 *
 */
public final class TemplateConfigManager {

    // List of the possible config types
    public enum ConfigType {
        SESSION( "Current Templates" ), 
        MASTER( "Archived Templates" );
        
        private final String name;
        ConfigType(final String name) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return this.name;
        }
    }
    
    private static final TemplateConfigManager instance = new TemplateConfigManager();
    private HashMap<ConfigType, TemplateConfig> configMap;

    private static final String MASTER_CONFIG_PATH = "config/templates.xml";
    
    /**
     * Get the singleton instace of the TemplateManager
     * @return
     */
    public static TemplateConfigManager getInstance() {
        return TemplateConfigManager.instance;
    }
    
    /**
     * Disallow public construction of a template manager
     */
    private TemplateConfigManager() {  
        this.configMap = new HashMap<TemplateConfigManager.ConfigType, TemplateConfig>();
    }
    
    /**
     * Load the master template config. This is the central template configuration
     * that is not tied to any particular session, and can be applied to any.
     * 
     * @throws ReportedException 
     */
    public void loadMasterConfig() throws FatalException {
        TemplateConfig cfg = TemplateConfig.fromFile( MASTER_CONFIG_PATH );
        this.configMap.put(ConfigType.MASTER, cfg);
    }
    
    /**
     * Set the template configuration for the active session
     * @param cfg
     */
    public void setSessionConfig ( final TemplateConfig cfg ) {
        this.configMap.put(ConfigType.SESSION, cfg);
    }
    
    /**
     * Get the specified type of parse template configuration
     * @return
     */
    public TemplateConfig getConfig( ConfigType type) {
        return this.configMap.get(type);
    }
    
    /**
     * Get a template by GUID from the specified config type
     * 
     * @param type
     * @param templateName
     * @return
     */
    public ParseTemplate getTemplate(ConfigType type, String guid) {
        return getConfig(type).get(guid);
    }

    /**
     * Copy <code>srcTemplate</code> into the configuration specifed by<code>to</code>.
     * If the destination config already contains a template with a matching name,
     * it will be overwritten.
     * 
     * @param srcTemplate The template to copy
     * @param to The destination config type
     */
    public void copyTemplate(ParseTemplate srcTemplate, ConfigType to) {
        TemplateConfig toCfg = getConfig(to);
        toCfg.add(srcTemplate);
    }
}
