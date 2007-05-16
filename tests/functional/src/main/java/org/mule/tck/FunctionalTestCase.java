/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.config.ConfigurationBuilder;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.manager.DefaultWorkListener;
import org.mule.util.ClassUtils;
import org.mule.RegistryContext;

import javax.resource.spi.work.WorkEvent;

/**
 * Is a base tast case for tests that initialise Mule using a configuration file. The
 * default configuration builder used is the MuleXmlConfigurationBuilder. This you
 * need to have the mule-modules-builders module/jar on your classpath. If you want
 * to use a different builder, just overload the <code>getBuilder()</code> method
 * of this class to return the type of builder you want to use with your test. Note
 * you can overload the <code>getBuilder()</code> to return an initialised instance
 * of the QuickConfiguratonBuilder, this allows the developer to programmatically
 * build a Mule instance and roves the need for additional config files for the test.
 */
public abstract class FunctionalTestCase extends AbstractMuleTestCase
{

    public static final String DEFAULT_BUILDER_CLASS = "org.mule.config.builders.MuleXmlConfigurationBuilder";

    protected UMOManagementContext createManagementContext() throws Exception
    {
        // Should we set up te manager for every method?
        UMOManagementContext context;
        if (getTestInfo().isDisposeManagerPerSuite() && managementContext!=null)
        {
            context = managementContext;
        }
        else
        {
            ConfigurationBuilder builder = getBuilder();
            context = builder.configure(getConfigResources(), null);
            RegistryContext.getConfiguration().setDefaultWorkListener(new TestingWorkListener());
        }
        return context;
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {

        try
        {
            Class builderClass = ClassUtils.loadClass(DEFAULT_BUILDER_CLASS, getClass());
            return (ConfigurationBuilder)builderClass.newInstance();
        }
        catch (ClassNotFoundException e)
        {
            throw new ClassNotFoundException(
                "The builder "
                                + DEFAULT_BUILDER_CLASS
                                + " is not on your classpath and "
                                + "the getBuilder() method of this class has not been overloaded to return adifferent builder. Please "
                                + "check your functional test.", e);
        }

    }

    protected abstract String getConfigResources();

    public class TestingWorkListener extends DefaultWorkListener
    {
        protected void handleWorkException(WorkEvent event, String type)
        {
            super.handleWorkException(event, type);
            if (event.getException() != null)
            {
                Throwable t = event.getException().getCause();
                if (t != null)
                {

                    if (t instanceof Error)
                    {
                        throw (Error)t;
                    }
                    else if (t instanceof RuntimeException)
                    {
                        throw (RuntimeException)t;
                    }
                }

            }
        }
    }

}
