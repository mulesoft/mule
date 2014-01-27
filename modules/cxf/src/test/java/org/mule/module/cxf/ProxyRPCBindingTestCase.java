/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;

public class ProxyRPCBindingTestCase extends FunctionalTestCase
{
    private static final String SAMPLE_REQUEST = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:sam=\"http://example.cxf.module.mule.org/\">\n" +
                                                 "   <soapenv:Header/>\n" +
                                                 "   <soapenv:Body>\n" +
                                                 "      <sam:getAll>\n" +
                                                 "         <pageSize>10</pageSize>\n" +
                                                 "         <pageNumber>1</pageNumber>\n" +
                                                 "      </sam:getAll>\n" +
                                                 "   </soapenv:Body>\n" +
                                                 "</soapenv:Envelope>";

    private static final String SAMPLE_RESPONSE = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                                                  "\t<soap:Body>\n" +
                                                  "\t\t<ns1:getAllResponse xmlns:ns1=\"http://example.cxf.module.mule.org/\">\n" +
                                                  "\t\t\t<return>\n" +
                                                  "\t\t\t\t<item>\n" +
                                                  "\t\t\t\t\t<artType>AUTHOR</artType>\n" +
                                                  "\t\t\t\t\t<famousWorks>Hamlet</famousWorks>\n" +
                                                  "\t\t\t\t\t<firstName>William</firstName>\n" +
                                                  "\t\t\t\t\t<lastName>Shakespeare</lastName>\n" +
                                                  "\t\t\t\t</item>\n" +
                                                  "\t\t\t\t<item>\n" +
                                                  "\t\t\t\t\t<artType>ACTOR</artType>\n" +
                                                  "\t\t\t\t\t<famousWorks>Mission Impossible</famousWorks>\n" +
                                                  "\t\t\t\t\t<firstName>Tom</firstName>\n" +
                                                  "\t\t\t\t\t<lastName>Cruise</lastName>\n" +
                                                  "\t\t\t\t</item>\n" +
                                                  "\t\t\t</return>\n" +
                                                  "\t\t</ns1:getAllResponse>\n" +
                                                  "\t</soap:Body>\n" +
                                                  "</soap:Envelope>";


    @Rule
    public final DynamicPort httpPortProxy = new DynamicPort("port1");

    @Rule
    public final DynamicPort httpPortService = new DynamicPort("port2");


    @Override
    protected String getConfigFile()
    {
        return "proxy-rpc-binding-conf.xml";
    }

    @Test
    public void proxyRPCBodyPayload() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPortProxy.getNumber() + "/body", SAMPLE_REQUEST, null);
        assertEquals(StringUtils.deleteWhitespace(SAMPLE_RESPONSE), StringUtils.deleteWhitespace(response.getPayloadAsString()));
    }

    @Test
    public void proxyRPCBodyEnvelope() throws Exception
    {
        MuleMessage response = muleContext.getClient().send("http://localhost:" + httpPortProxy.getNumber() + "/envelope", SAMPLE_REQUEST, null);
        assertEquals(StringUtils.deleteWhitespace(SAMPLE_RESPONSE), StringUtils.deleteWhitespace(response.getPayloadAsString()));
    }

}
