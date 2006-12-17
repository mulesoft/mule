/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring.config;

import org.mule.MuleManager;
import org.mule.impl.model.seda.SedaModel;
import org.mule.config.ConfigurationBuilder;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.manager.UMOManager;

public class SpringNamespaceConfigBuilderTestCase extends FunctionalTestCase
{

    public String getConfigResources()
    {
        return "test-mule2-app-context.xml";
    }

    public ConfigurationBuilder getBuilder()
    {
        return new SpringConfigurationBuilder();
    }

    public void testComponentResolverConfig() throws Exception
    {
        // test container init
        UMOManager manager = MuleManager.getInstance();
        assertTrue(manager.getModel() instanceof SedaModel);
    }

}