/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    //Note that the space after the comma denotes that the classes path itself should be scanned.  I don't know what this is
    //required when scanning for resources
    private static final String DEFAULT_BASEPATH = "org.mule, ";

    public void setMuleContext(MuleContext context)
    {
        //Don't like this but without it the user must explicitly configure this builder at start up
        String scanPackages = System.getProperty(SCAN_PACKAGES_PROPERTY, DEFAULT_BASEPATH);
        String[] paths = scanPackages.split(",");
        IBeanHolderConfigurationBuilder builder = new IBeanHolderConfigurationBuilder(paths);
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
