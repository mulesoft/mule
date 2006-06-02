/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.config.builders;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.components.script.jsr223.Scriptable;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.MuleProperties;
import org.mule.config.ReaderResource;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.manager.UMOManager;

import javax.script.CompiledScript;
import javax.script.Namespace;

import java.io.IOException;

/**
 * Will configure a MuleManager from one or more script files
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ScriptConfigurationBuilder extends Scriptable implements ConfigurationBuilder {

    public static final String SCRIPT_ENGINE_NAME_PROPERTY = "org.mule.script.engine";
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());
    protected UMOManager manager = null;
    protected QuickConfigurationBuilder builder = null;
    protected boolean initialised = false;

    public ScriptConfigurationBuilder() {
        builder = new QuickConfigurationBuilder(false);
        manager = MuleManager.getInstance();
        String scriptName = System.getProperty(SCRIPT_ENGINE_NAME_PROPERTY);
        if(scriptName==null) {
            throw new NullPointerException(new Message(
                    Messages.SYSTEM_PROPERTY_X_NOT_SET, SCRIPT_ENGINE_NAME_PROPERTY).getMessage());
        } else {
            this.setScriptEngineName(scriptName);
        }
    }

    public ScriptConfigurationBuilder(String scriptEngineName) {
        builder = new QuickConfigurationBuilder(false);
        manager = MuleManager.getInstance();
        this.setScriptEngineName(scriptEngineName);
    }


    /**
     * Will configure a UMOManager based on the configuration file(s) provided.
     *
     * @param configResources a comma separated list of configuration files to
     *                        load, this should be accessible on the classpath or filesystem
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManager configure(String configResources) throws ConfigurationException {
//        if(!initialised) {
//            try {
//                initialise();
//                initialised = true;
//            } catch (InitialisationException e) {
//                throw new ConfigurationException(e);
//            }
//        }
        try {
            return configure(ReaderResource.parseResources( configResources));
        } catch (IOException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Will configure a UMOManager based on the configurations made available
     * through Readers
     *
     * @param configResources an array of Readers
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManager configure(ReaderResource[] configResources) throws ConfigurationException {
        try {

            for (int i = 0; i < configResources.length; i++) {
                ReaderResource configResource = configResources[i];
                setScriptFile(configResource.getDescription());
                initialise();
                Namespace ns = getScriptEngine().createNamespace();
                populateNamespace(ns);
                CompiledScript script = compileScript(configResource.getReader());
                script.eval(ns);
            }

            if(System.getProperty(MuleProperties.MULE_START_AFTER_CONFIG_SYSTEM_PROPERTY, "true").equalsIgnoreCase("true")) {
                if(!manager.isStarted()) {
                    manager.start();
                }
            }

        } catch (Exception e) {
            throw new ConfigurationException(e);
        }
        return manager;
    }

    protected void populateNamespace(Namespace namespace) {
        namespace.put("manager", manager);
        namespace.put("builder", builder);

    }

    public boolean isConfigured() {
        return manager!=null;
    }
}
