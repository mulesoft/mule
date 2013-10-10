/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.cxf.Bus;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class StaxFeatureTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
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

