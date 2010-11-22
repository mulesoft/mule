/*
 * $Id: SxcFilterTestCase.java 12264 2008-07-09 22:24:04Z dandiep $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.sxc;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;

import java.io.ByteArrayOutputStream;

import org.apache.commons.io.IOUtils;

public class HttpRoutingTestCase extends DynamicPortTestCase 
{
    int finished = 0;

    public void testBasicXPath() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(getClass().getResourceAsStream("/purchase-order.xml"), out);

        String address = "http://localhost:" + getPorts().get(0) + "/proxy";
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

    @Override
    protected String getConfigResources()
    {
        return "http-routing-conf.xml";
    }

    protected int getNumPortsToFind()
    {
        return 1;
    }

}
