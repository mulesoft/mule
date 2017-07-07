/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static com.google.common.net.MediaType.PLAIN_TEXT_UTF_8;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.httpclient.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * This is a simple test where it is verified that the SSLSession is propagated and the properties defined in a JAX-WS
 * service are respected so that the WS-Service-Policy assertions are honored.
 */
public class HttpSecurityPolicyTestCase extends AbstractHttpSecurityTestCase
{
    private static String SOAP_REQUEST_OPEN =
            "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tes=\"http://testmodels.cxf.module.mule.org/\">";


    private static String SOAP_ENV_HEADER_OPEN = "<soapenv:Header>";

    private static String SOAP_ENV_HEADER_CLOSE = "</soapenv:Header>";
    String msgEchoOperation1 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:new=\"http://new.webservice.namespace\">"
                               + "<soapenv:Header/>"
                               + "  <soapenv:Body>"
                               + "    <new:parameter1>hello world</new:parameter1>"
                               + "  </soapenv:Body>"
                               + "</soapenv:Envelope>";


    private static String SOAP_BODY = "<soapenv:Body>"
                                      + "<tes:echo>"
                                      + "<text>echo</text>"
                                      + "</tes:echo>"
                                      + "</soapenv:Body>";

    private static String SOAP_REQUEST_CLOSE = "</soapenv:Envelope>";


    private static String SOAP_PROPERTY_USERNAME_TOKEN =
            "<wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">"
                                                         + "<wsse:UsernameToken wsu:Id=\"UsernameToken-8424E70C4CDF89B5E514992807079293\">"
                                                         + "<wsse:Username>fabiang</wsse:Username>"
                                                         + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">password</wsse:Password>"
                                                         + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">nFovTkBez8DceekL08FsQw==</wsse:Nonce>"
                                                         + "</wsse:UsernameToken>"
                                                         + "</wsse:Security>";

    private static String soapPropertyUsernameWrongPasswordToken =
            "<wsse:Security soapenv:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">"
                                                                   + "<wsse:UsernameToken wsu:Id=\"UsernameToken-8424E70C4CDF89B5E514992807079293\">"
                                                                   + "<wsse:Username>fabiang</wsse:Username>"
                                                                   + "<wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">wrongPassword</wsse:Password>"
                                                                   + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">nFovTkBez8DceekL08FsQw==</wsse:Nonce>"
                                                                   + "</wsse:UsernameToken>"
                                                                   + "</wsse:Security>";

    private static String SOAP_RESPONSE_AUTHENTICATED =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                                                        "<SOAP-ENV:Header xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\"/>" +
                                                        "<soap:Body><ns2:echoResponse xmlns:ns2=\"http://testmodels.cxf.module.mule.org/\">" +
                                                        "<text>echo</text>" +
                                                        "</ns2:echoResponse>" +
                                                        "</soap:Body>" +
                                                        "</soap:Envelope>";

    private static String SOAP_RESPONSE_NOT_AUTHENTICATED =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><soap:Fault><faultcode xmlns:ns1=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">ns1:FailedAuthentication</faultcode><faultstring>The security token could not be authenticated or authorized</faultstring></soap:Fault></soap:Body></soap:Envelope>";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");


    public HttpSecurityPolicyTestCase(ConfigVariant variant, String configResources)

    {
        super(variant, configResources);
    }

    @Test
    public void testSSLSessionPropagationWithCorrectPassword() throws Exception
    {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod("https://localhost:" + dynamicPort2.getNumber() + "/echo");
        sendRequest(true, client, method, SOAP_PROPERTY_USERNAME_TOKEN);
        assertThat(client.executeMethod(method), equalTo(SC_OK));
        assertThat(method.getResponseBodyAsString(), equalTo(SOAP_RESPONSE_AUTHENTICATED));
    }

    @Test
    public void testSSLSessionPropagationWithIncorrectPassword() throws Exception
    {
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod("https://localhost:" + dynamicPort2.getNumber() + "/echo");
        sendRequest(false, client, method, soapPropertyUsernameWrongPasswordToken);
        assertThat(client.executeMethod(method), equalTo(SC_INTERNAL_SERVER_ERROR));
        assertThat(method.getResponseBodyAsString(), equalTo(SOAP_RESPONSE_NOT_AUTHENTICATED));
    }

    private void sendRequest(boolean correctPassword, HttpClient client, PostMethod method, String passwordSecurityPart) throws Exception
    {
        StringRequestEntity requestEntity = new StringRequestEntity(getSoapRequest(correctPassword, passwordSecurityPart), PLAIN_TEXT_UTF_8.toString(), UTF_8.name());
        method.setRequestEntity(requestEntity);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return asList(new Object[][] {
                                      {ConfigVariant.FLOW, "jax-ws-security-policy.xml"}
        });
    }

    private static String getSoapRequest(boolean correctPassword, String passwordSecurityPart)
    {
        StringBuffer request = new StringBuffer();
        request.append(SOAP_REQUEST_OPEN);
        request.append(SOAP_ENV_HEADER_OPEN);
        request.append(passwordSecurityPart);
        request.append(SOAP_ENV_HEADER_CLOSE);
        request.append(SOAP_BODY);
        request.append(SOAP_REQUEST_CLOSE);

        return request.toString();
    }
}
