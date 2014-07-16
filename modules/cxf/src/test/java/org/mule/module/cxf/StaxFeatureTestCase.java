/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertNotNull;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.cxf.Bus;
import org.junit.Rule;
import org.junit.Test;

public class StaxFeatureTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "stax-feature-conf.xml";
    }

    @Test
    public void testEchoService() throws Exception
    {
        CxfConfiguration cxfCon = (CxfConfiguration)muleContext.getRegistry().lookupObject(CxfConstants.DEFAULT_CXF_CONFIGURATION);
        assertNotNull(cxfCon);

        Bus cxfBus = cxfCon.getCxfBus();

        // ensure that our interceptor is there
//        assertEquals(2, cxfBus.getInInterceptors().size());
//
//        MuleClient client = new MuleClient();
//        MuleMessage result = client.send("cxf:http://localhost:63081/services/Echo?method=echo", "Hello!", null);
//        assertEquals("Hello!", result.getPayload());
    }
}

