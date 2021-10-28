/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.issues;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.client.HttpRequestOptions;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;


/**
 * This test sends a message to a CXF proxy client with wsse:Security.
 * The flow finish with a mulexml:dom-to-xml-transformer. It should transform the payload and return an empty body.
 */
public class CxfProxyClientWithXmlTransformerTestCase extends FunctionalTestCase {

    private static final String SOAP_REQUEST_WITH_SECURITY =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ver=\"http://www.murex.com/versionplanning-v1/]\"> " +
                    "<soapenv:Header>" +
                    " <username>username</username> " +
                    "<wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"> " +
                    "<saml2:Assertion ID=\"_415D4BF34ABCB6F5F714242477649121\" IssueInstant=\"2016-05-31T09:58:44.916Z\" Version=\"2.0\" xsi:type=\"saml2:AssertionType\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"> " +
                    "<saml2:Issuer>www.example.com</saml2:Issuer> " +
                    "<saml2:Subject> " +
                    "<saml2:NameID>uid=joe,ou=people,ou=saml-demo,o=example.com</saml2:NameID> " +
                    "<saml2:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:sender-vouches\"/> " +
                    "</saml2:Subject> " +
                    "</saml2:Assertion> " +
                    "</wsse:Security> " +
                    "</soapenv:Header> " +
                    "<soapenv:Body></soapenv:Body>" +
                    "</soapenv:Envelope>";

    private static final String SOAP_RESPONSE_WITH_SECURITY =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                    "<soap:Body><soapenv:Body xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"/>" +
                    "</soap:Body>" +
                    "</soap:Envelope>";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port");

    private static final HttpRequestOptions HTTP_REQUEST_OPTIONS = newOptions().method(POST.name()).disableStatusCodeValidation().build();

    @Test
    public void testProxyClientWithXmlTransformer() throws Exception
    {
        MuleMessage msg = getTestMuleMessage(SOAP_REQUEST_WITH_SECURITY);
        MuleMessage muleMessage = muleContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/test-xml", msg, HTTP_REQUEST_OPTIONS);
        assertThat(muleMessage.getPayloadAsString(), equalTo(SOAP_RESPONSE_WITH_SECURITY));
    }

    @Override
    protected String getConfigFile()
    {
        return "issues/proxy-service-with-xml-transformer.xml";
    }
}
