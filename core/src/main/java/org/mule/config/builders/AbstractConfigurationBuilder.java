/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.builders;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.LifecycleManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A support class for {@link org.mule.api.config.ConfigurationBuilder} implementations
 * that handles the logic of creating config arrays and {@link java.util.Properties}
 * arguments
 *
 * @see org.mule.api.config.ConfigurationBuilder
 */
public abstract class AbstractConfigurationBuilder implements ConfigurationBuilder
{
    protected transient final Log logger = LogFactory.getLog(getClass());

    protected boolean configured = false;

    /**
     * Will configure a MuleContext object based on the builders configuration settings.
     * This method will delegate the actual processing to {@link #doConfigure(org.mule.api.MuleContext)}
     *
     * @param muleContext The current {@link org.mule.api.MuleContext}
     * @throws ConfigurationException if the configuration fails i.e. an object cannot be created or
     * initialised properly
     */
    public void configure(MuleContext muleContext) throws ConfigurationException
    {
        try
        {
            doConfigure(muleContext);
            applyLifecycle(muleContext.getLifecycleManager());
            configured = true;
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
    }

    /**
     * Will configure a MuleContext based on the configuration provided.  The configuration will be set on the
     * {@link org.mule.api.config.ConfigurationBuilder} implementation as bean properties before this method
     * has been called.
     *
     * @param muleContext The current {@link org.mule.api.MuleContext}
     * @throws ConfigurationException if the configuration fails i.e. an object cannot be created or
     * initialised properly
     */
    protected abstract void doConfigure(MuleContext muleContext) throws Exception;

    /**
     * Allows a configuration builder to check and customise the lifecycle of objects in the registry
     * being used.  The ONLY time a user should implement this method is if the underlying container for
     * the Registry is an IoC container had manages it's own lifecycle.  If this is the case the lifecycle
     * manager can be used to call the next lifecycle method on all the objects.  For example for the Spring
     * Registry only Initialise and Dispose phase is handled by Spring. The Start and Stop phases are handled
     * by Mule by calling-
     * <code>
     * // If the MuleContext is started, start all objects in the new Registry.
     *  if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME))
     *  {
     *      lifecycleManager.applyPhase(registry.lookupObjects(Object.class), Startable.PHASE_NAME);
     *  }
     * </code>
     * @param lifecycleManager the lifecycleManager for the current context
     * @throws Exception if anything goes wrong.  Usually this is an exeption bubbled up from calling
     * a lifecycle method on an object in the registry
     */
    protected void applyLifecycle(LifecycleManager lifecycleManager) throws Exception
    {
        //by default do nothing
    }

    /**
     * Has this builder been configured already
     * @return true if the {@link #configure(org.mule.api.MuleContext)} method has been called
     */
    public boolean isConfigured()
    {
        return configured;
    }
}
