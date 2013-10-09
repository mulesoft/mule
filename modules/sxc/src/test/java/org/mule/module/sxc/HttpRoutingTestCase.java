/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.sxc;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class HttpRoutingTestCase extends FunctionalTestCase
{
    private int finished = 0;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-routing-conf.xml";
    }

    @Test
    public void testBasicXPath() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getResourceAsStream("/purchase-order.xml"), out);

        String address = "http://localhost:" + dynamicPort.getNumber() + "/proxy";
        MuleMessage res = client.send(address, out.toByteArray(), null);
        System.out.println(res.getPayloadAsString());
        assertTrue(res.getPayloadAsString().contains("purchaseOrder"));
        assertTrue(res.getPayloadAsString().contains("Alice"));

        out = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getResourceAsStream("/namespace-purchase-order.xml"), out);

        res = client.send(address, out.toByteArray(), null);
        System.out.println(res.getPayloadAsString());
        assertTrue(res.getPayloadAsString().contains("purchaseOrder"));
        assertTrue(res.getPayloadAsString().contains("Alice"));
    }

}
