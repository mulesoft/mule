/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.config;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextAware;

/**
 * Responsible for discovering and loading available iBeans into the registry.
 */
public class IBeansLoader implements MuleContextAware
{
    public static final String SCAN_PACKAGES_PROPERTY = "org.mule.scan";
    public void setMuleContext(MuleContext context)
    {
        //Don't like this but without it the user must explicitly configure this builder at start up
        String scanPackages = System.getProperty(SCAN_PACKAGES_PROPERTY, "");
        IBeanHolderConfigurationBuilder builder = new IBeanHolderConfigurationBuilder(scanPackages);
        try
        {
            builder.configure(context);
        }
        catch (ConfigurationException e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}
