/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.builders;

import org.mule.MuleServer;
import org.mule.RegistryContext;
import org.mule.components.script.jsr223.Scriptable;
import org.mule.config.ConfigurationException;
import org.mule.config.MuleProperties;
import org.mule.config.builders.i18n.BuildersMessages;
import org.mule.config.spring.MuleApplicationContext;
import org.mule.registry.Registry;
import org.mule.registry.RegistrationException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;

import java.util.Properties;

import javax.script.Bindings;

/** Configures a MuleManager from one or more script files. */
public class ScriptConfigurationBuilder extends MuleXmlConfigurationBuilder
{
    public static final String SCRIPT_ENGINE_NAME_PROPERTY = "org.mule.script.engine";

    private Scriptable scriptComponent = new Scriptable();

    protected UMOManagementContext managementContext = null;
    protected boolean initialised = false;

    public ScriptConfigurationBuilder() throws UMOException
    {
        String scriptName = System.getProperty(SCRIPT_ENGINE_NAME_PROPERTY);
        if (scriptName == null)
        {
            throw new IllegalArgumentException(
                    BuildersMessages.systemPropertyNotSet(SCRIPT_ENGINE_NAME_PROPERTY).getMessage());
        }
        else
        {
            scriptComponent.setScriptEngineName(scriptName);
        }
    }

    public ScriptConfigurationBuilder(String scriptEngineName) throws UMOException
    {
        this(scriptEngineName, true);
    }

    public ScriptConfigurationBuilder(String scriptEngineName, boolean createDefaultRegistry) throws UMOException
    {
        //Create a Registry by default if we do not have one
//        if(RegistryContext.getRegistry()==null && createDefaultRegistry)
//        {
//            // TODO MULE-2161
//            TransientRegistry registry = TransientRegistry.createNew();
//            RegistryContext.setRegistry(registry);
//            managementContext = MuleServer.getManagementContext();
//        }
        scriptComponent.setScriptEngineName(scriptEngineName);
    }


    /**
     * Will configure a UMOManager based on the configurations made available through
     * Readers
     *
     * @param configResources an array of Readers
     * @return A configured UMOManager
     * @throws org.mule.config.ConfigurationException
     *
     */
    public UMOManagementContext configure(String[] configResources, Properties startupProperties)
            throws ConfigurationException
    {
        try
        {
            MuleApplicationContext context = new MuleApplicationContext(new String[]{getDefaultConfigResource()});
            managementContext = context.getManagementContext();
            MuleServer.setManagementContext(managementContext);

            // Load startup properties.
            Registry registry = RegistryContext.getOrCreateRegistry();
            try
            {
                registry.registerObjects(startupProperties);
            }
            catch (RegistrationException e)
            {
                throw new ConfigurationException(e);
            }

            for (int i = 0; i < configResources.length; i++)
            {
                String configResource = configResources[i];
                scriptComponent.setScriptFile(configResource);
                scriptComponent.initialise();
                Bindings ns = scriptComponent.getScriptEngine().createBindings();
                populateBindings(ns);
                scriptComponent.runScript(ns);
            }

            if (!managementContext.isInitialised() && !managementContext.isInitialising())
            {
                managementContext.initialise();
            }

            if (System.getProperty(MuleProperties.MULE_START_AFTER_CONFIG_SYSTEM_PROPERTY, "true")
                    .equalsIgnoreCase("true") && isStartContext())
            {
                if (!managementContext.isStarted())
                {
                    managementContext.start();
                }
            }

        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
        return managementContext;
    }

    protected void populateBindings(Bindings bindings)
    {
        bindings.put("managementContext", managementContext);
    }

}
