/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import static org.junit.Assert.assertTrue;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.util.IOUtils;

/**
 * With these tests we verify that the correct operation
 * is used when there are more than one operation in the
 * same binding.
 *
 */
public class ProxyBindingTestCase extends FunctionalTestCase
{
    @Rule
    public final DynamicPort httpPortProxy = new DynamicPort("port1");

    @Rule
    public final DynamicPort httpPortService = new DynamicPort("port2");

    public static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(org.mule.module.http.api.HttpConstants.Methods.POST.name()).build();

    private String getAllRequest;
    private String addArtistRequest;
    private String getAllResponse;
    private String addArtistResponse;

    @Override
    protected String getConfigFile()
    {
        return "proxy-service-one-way-flow.xml";
    }

    @Before
    public void doSetUp() throws Exception
    {
        getAllRequest = IOUtils.getResourceAsString("artistregistry-get-all-request.xml", getClass());
        addArtistRequest = IOUtils.getResourceAsString("artistregistry-add-artist-request.xml", getClass());
        getAllResponse = IOUtils.getResourceAsString("artistregistry-get-all-response.xml", getClass());
        addArtistResponse = IOUtils.getResourceAsString("artistregistry-add-artist-response.xml", getClass());
        XMLUnit.setIgnoreWhitespace(true);
    }
   
    @Test
    public void proxyGetAllRequest() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPortProxy.getNumber() + "/body", getTestMuleMessage(getAllRequest), HTTP_REQUEST_OPTIONS);
        assertTrue(XMLUnit.compareXML(getAllResponse, response.getPayloadAsString()).identical());
    }
    
    @Test
    public void proxyAddArtistRequest() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPortProxy.getNumber() + "/body", getTestMuleMessage(addArtistRequest), HTTP_REQUEST_OPTIONS);
        assertTrue(XMLUnit.compareXML(addArtistResponse, response.getPayloadAsString()).identical());
    }
}
