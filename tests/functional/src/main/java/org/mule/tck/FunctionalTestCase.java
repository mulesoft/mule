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

import org.mule.RegistryContext;
import org.mule.config.ConfigurationBuilder;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.impl.model.MuleProxy;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.mule.TestMuleProxy;
import org.mule.tck.testmodels.mule.TestSedaComponent;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.model.UMOModel;

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
    /** Expected response after the test message has passed through the FunctionalTestComponent. */
    public static final String TEST_MESSAGE_RESPONSE = FunctionalTestComponent.received(TEST_MESSAGE);
    
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
            context = builder.configure(getConfigResources());
            RegistryContext.getConfiguration().setDefaultWorkListener(new TestingWorkListener());
        }
        return context;
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        // TODO MULE-1988
        builder.setStartContext(isStartContext());
        return builder;
    }

    protected abstract String getConfigResources();
    
    protected FunctionalTestComponent lookupTestComponent(String modelName, String componentName) throws UMOException
    {    
        // TODO MULE-1995 Simplify this lookup
        UMOModel m = managementContext.getRegistry().lookupModel(modelName);
        assertNotNull("Model " + m + " not found", m);
        UMOComponent c = m.getComponent(componentName);
        assertNotNull("Component " + c + " not found", c);
        assertTrue("Component should be a TestSedaComponent", c instanceof TestSedaComponent);
        MuleProxy proxy = ((TestSedaComponent) c).getProxy();
        assertNotNull("Component " + c + " does not have a proxy", proxy);
        assertTrue("Proxy should be a TestMuleProxy", proxy instanceof TestMuleProxy);
        Object component = ((TestMuleProxy) proxy).getComponent();
        assertNotNull("No component for proxy", component);
        assertTrue("Component should be a FunctionalTestComponent", component instanceof FunctionalTestComponent);
        return (FunctionalTestComponent) component;
    }
}
