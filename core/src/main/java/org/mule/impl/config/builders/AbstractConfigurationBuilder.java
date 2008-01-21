/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.config.builders;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;

/**
 * A support class for {@link org.mule.api.config.ConfigurationBuilder} implementations
 * that handles the logic of creating config arrays and {@link java.util.Properties}
 * arguments
 * 
 * @see org.mule.api.config.ConfigurationBuilder
 */
public abstract class AbstractConfigurationBuilder implements ConfigurationBuilder
{

    private boolean configured = false;

    public void configure(MuleContext muleContext) throws ConfigurationException
    {
        try
        {
            doConfigure(muleContext);
            configured = true;
        }
        catch (Exception e)
        {
            throw new ConfigurationException(e);
        }
    }

    protected abstract void doConfigure(MuleContext muleContext) throws Exception;

    public boolean isConfigured()
    {
        return configured;
    }
}
