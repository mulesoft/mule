/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ProxyRPCBindingTestCase extends FunctionalTestCase
{
    @Rule
    public final DynamicPort httpPortProxy = new DynamicPort("port1");

    @Rule
    public final DynamicPort httpPortService = new DynamicPort("port2");

    private String getAllRequest;
    private String getAllResponse;


    @Override
    protected String getConfigFile()
    {
        return "proxy-rpc-binding-conf.xml";
    }

    @Before
    public void doSetUp() throws Exception
    {
        getAllRequest = IOUtils.getResourceAsString("artistregistry-get-all-request.xml", getClass());
        getAllResponse = IOUtils.getResourceAsString("artistregistry-get-all-response.xml", getClass());
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void proxyRPCBodyPayload() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPortProxy.getNumber() + "/body", getAllRequest, null);
        assertTrue(XMLUnit.compareXML(getAllResponse, response.getPayloadAsString()).identical());
    }

    @Test
    public void proxyRPCBodyEnvelope() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPortProxy.getNumber() + "/envelope", getAllRequest, null);
        assertTrue(XMLUnit.compareXML(getAllResponse, response.getPayloadAsString()).identical());
    }

}
