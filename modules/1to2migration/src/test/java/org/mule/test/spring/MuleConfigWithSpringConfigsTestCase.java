/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.spring;

import org.mule.config.ConfigurationBuilder;
import org.mule.extras.spring.config.SpringConfigurationBuilder;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.umo.provider.UMOConnector;

/**
 * Tests a Mule config together with Spring configs in the same list of resources.
 */
public class MuleConfigWithSpringConfigsTestCase extends MultipleSpringContextsTestCase
{
    public String getConfigResources()
    {
        return "test-xml-mule-config.xml, test-application-context.xml, test-application-context-2.xml";
    }
    
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        return new SpringConfigurationBuilder();
    }

    public void testConnectorBean()
    {
        UMOConnector c = managementContext.getRegistry().lookupConnector("dummyConnector");
        assertNotNull(c);
        assertTrue(c instanceof TestConnector);
    }

}


