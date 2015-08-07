/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.functional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transformer.types.MimeTypes;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

public class CxfDataTypeTestCase extends FunctionalTestCase
{
    private static final String requestPayload =
        "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "           xmlns:hi=\"http://example.cxf.module.mule.org/\">\n" +
            "<soap:Body>\n" +
            "<hi:sayHi>\n" +
            "    <arg0>Hello</arg0>\n" +
            "</hi:sayHi>\n" +
            "</soap:Body>\n" +
            "</soap:Envelope>";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "cxf-datatype-conf.xml";
    }

    @Test
    public void testCxfService() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String,Object>)null, muleContext);
        MuleMessage received = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/hello", request, newOptions().method(POST.name()).disableStatusCodeValidation().build());
        Assert.assertThat(received.getPayloadAsString(), not(containsString("Fault")));
    }

    @Test
    public void testCxfClient() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleMessage received = muleContext.getClient().send("vm://helloClient", request);
        Assert.assertThat(received.getPayloadAsString(), not(containsString("Fault")));
    }

    @Test
    public void testCxfProxy() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(requestPayload, (Map<String,Object>)null, muleContext);
        MuleMessage received = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/hello-proxy", request, newOptions().method(POST.name()).disableStatusCodeValidation().build());
        Assert.assertThat(received.getPayloadAsString(), not(containsString("Fault")));
    }

    @Test
    public void testCxfSimpleService() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("Content-Type", "application/soap+xml");
        InputStream xml = getClass().getResourceAsStream("/direct/direct-request.xml");
        MuleMessage result = client.send("http://localhost:" + dynamicPort.getNumber() + "/echo", new DefaultMuleMessage(xml, props, muleContext), newOptions().method(POST.name()).disableStatusCodeValidation().build());
        Assert.assertThat(result.getPayloadAsString(), not(containsString("Fault")));
    }

    @Test
    public void testCxfSimpleClient() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage("hello", (Map<String,Object>)null, muleContext);
        MuleMessage received = muleContext.getClient().send("vm://helloClient", request);
        Assert.assertThat(received.getPayloadAsString(), not(containsString("Fault")));
    }

    public static class EnsureXmlDataType extends EnsureDataType
    {

        public EnsureXmlDataType()
        {
            super(MimeTypes.XML);
        }
    }

    public static class EnsureAnyDataType extends EnsureDataType
    {

        public EnsureAnyDataType()
        {
            super(MimeTypes.ANY);
        }
    }

    private static class EnsureDataType implements Callable
    {

        private final String mimeType;

        public EnsureDataType(String mimeType){
            this.mimeType = mimeType;
        }

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            if( ! eventContext.getMessage().getDataType().getMimeType().equals(mimeType) ){
                throw new RuntimeException();
            }
            return eventContext.getMessage().getPayload();
        }
    }

}
