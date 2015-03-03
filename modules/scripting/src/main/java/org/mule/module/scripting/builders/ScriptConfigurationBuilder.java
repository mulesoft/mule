/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.builders;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.config.ConfigResource;
import org.mule.config.builders.AbstractResourceConfigurationBuilder;
import org.mule.config.builders.i18n.BuildersMessages;
import org.mule.module.scripting.component.Scriptable;
import org.mule.registry.TransientRegistry;

import javax.script.Bindings;

/**
 * Configures Mule from one or more script files.
 *
 * @deprecated This class is deprecated and will be removed in Mule 4.0
 */
@Deprecated
public class ScriptConfigurationBuilder extends AbstractResourceConfigurationBuilder
{
    public static final String SCRIPT_ENGINE_NAME_PROPERTY = "org.mule.script.engine";

    private Scriptable scriptComponent;

    private String scriptEngineName;

    protected MuleContext muleContext = null;

    public ScriptConfigurationBuilder(String configResource) throws MuleException
    {
        this(System.getProperty(SCRIPT_ENGINE_NAME_PROPERTY), configResource);
    }

    public ScriptConfigurationBuilder(String[] configResources) throws MuleException
    {
        this(System.getProperty(SCRIPT_ENGINE_NAME_PROPERTY), configResources);
    }

    public ScriptConfigurationBuilder(String scriptEngineName, String configResource) throws MuleException
    {
        super(configResource);
        if (scriptEngineName == null)
        {
            // we can guess engine by file extension
            logger.warn(BuildersMessages.systemPropertyNotSet(SCRIPT_ENGINE_NAME_PROPERTY).getMessage());
        }
        this.scriptEngineName = scriptEngineName;
    }

    public ScriptConfigurationBuilder(String scriptEngineName, String[] configResources) throws MuleException
    {
        super(configResources);
        if (scriptEngineName == null)
        {
            // we can guess engine by file extension
            logger.warn(BuildersMessages.systemPropertyNotSet(SCRIPT_ENGINE_NAME_PROPERTY).getMessage());
        }
        this.scriptEngineName = scriptEngineName;
    }

    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        this.muleContext = muleContext;
        TransientRegistry registry = new TransientRegistry(muleContext);
        registry.initialise();
        muleContext.addRegistry(registry);
        //registry.registerObject(MuleProperties.OBJECT_MULE_SIMPLE_REGISTRY_BOOTSTRAP, new SimpleRegistryBootstrap());

        scriptComponent = new Scriptable(muleContext);
        scriptComponent.setScriptEngineName(scriptEngineName);
            
        for (int i = 0; i < configResources.length; i++)
        {
            ConfigResource configResource = configResources[i];
            scriptComponent.setScriptFile(configResource.getResourceName());
            scriptComponent.initialise();
            // Set up initial script variables.
            Bindings bindings = scriptComponent.getScriptEngine().createBindings();
            scriptComponent.populateDefaultBindings(bindings);
            scriptComponent.runScript(bindings);
        }
    }

    protected void populateBindings(Bindings bindings)
    {
        bindings.put("muleContext", muleContext);
    }

}
