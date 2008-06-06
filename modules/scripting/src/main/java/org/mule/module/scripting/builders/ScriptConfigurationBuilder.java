/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.builders;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.config.ConfigResource;
import org.mule.config.builders.AbstractResourceConfigurationBuilder;
import org.mule.config.builders.i18n.BuildersMessages;
import org.mule.module.scripting.component.Scriptable;

import javax.script.Bindings;

/** Configures Mule from one or more script files. */
public class ScriptConfigurationBuilder extends AbstractResourceConfigurationBuilder
{
    public static final String SCRIPT_ENGINE_NAME_PROPERTY = "org.mule.script.engine";

    private Scriptable scriptComponent = new Scriptable();

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
        scriptComponent.setScriptEngineName(scriptEngineName);
    }

    public ScriptConfigurationBuilder(String scriptEngineName, String[] configResources) throws MuleException
    {
        super(configResources);
        if (scriptEngineName == null)
        {
            // we can guess engine by file extension
            logger.warn(BuildersMessages.systemPropertyNotSet(SCRIPT_ENGINE_NAME_PROPERTY).getMessage());
        }
        scriptComponent.setScriptEngineName(scriptEngineName);
    }

    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        this.muleContext = muleContext;
            
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

    protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception 
    {
        // nothing to do
    }
    
    protected void populateBindings(Bindings bindings)
    {
        bindings.put("muleContext", muleContext);
    }

}
