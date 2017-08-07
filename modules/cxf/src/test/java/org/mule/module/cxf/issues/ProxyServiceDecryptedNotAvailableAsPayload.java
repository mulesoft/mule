/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.issues;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.api.security.tls.TlsConfiguration.DISABLE_SYSTEM_PROPERTIES_MAPPING_PROPERTY;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.api.MuleMessage;
import org.mule.module.cxf.wssec.ClientPasswordCallback;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * This tests sends a message to a CXF proxy client which encrypts the payload. This is routed throughout a CXF proxy
 * server which decrypts the payload and resends it back to the original flow. Originally (MULE-13246) proxy-server did
 * not provide the flow with an XLMStreamReader of the SOAP message, as documented, in case the envelope payload
 * attribute was set.
 */
public class ProxyServiceDecryptedNotAvailableAsPayload extends FunctionalTestCase
{

    @ClassRule
    public static SystemProperty disablePropertiesMapping = new SystemProperty("com.sun.net.ssl.checkRevocation", "false");

    private static final String SOAP_REQUEST =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                               "<soap:Body><test xmlns=\"http://foo\"> foo </test></soap:Body>" +
                                               "</soap:Envelope>";

    private static final String SOAP_RESPONSE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><test xmlns=\"http://foo\"> foo </test>";
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    @Before
    public void doSetUp() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");
    }
    
    @Test
    public void testDecryptedPayloadAvailable() throws Exception
    {
        MuleMessage msg = getTestMuleMessage(SOAP_REQUEST);
        MuleMessage muleMessage = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/client", msg, HTTP_REQUEST_OPTIONS);
        assertThat(muleMessage.getPayloadAsString(), equalTo(SOAP_RESPONSE));
    }

    @Override
    protected String getConfigFile()
    {
        return "issues/proxy-service-serving-wsdl-decrypted-no-available.xml";
    }

}
