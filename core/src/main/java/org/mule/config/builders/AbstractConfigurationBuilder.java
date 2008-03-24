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

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.config.i18n.CoreMessages;

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

    private boolean configured = false;

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
            logger.error(CoreMessages.configurationBuilderError(this), e);
            throw new ConfigurationException(e);
        }
    }

    protected abstract void doConfigure(MuleContext muleContext) throws Exception;

    protected abstract void applyLifecycle(LifecycleManager lifecycleManager) throws Exception;
    
    public boolean isConfigured()
    {
        return configured;
    }
}
