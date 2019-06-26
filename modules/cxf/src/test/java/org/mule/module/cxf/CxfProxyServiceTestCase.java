/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static java.lang.System.setProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CxfProxyServiceTestCase extends FunctionalTestCase
{

    public static final int CLIENT_TIMEOUT = 3600000;
    private static final String ENVELOPE_TAG_NAME = "soapenv:Envelope";
    private DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

    @Rule
    public DynamicPort listenerPort = new DynamicPort("listenerPort");

    @Override
    protected String getConfigFile()
    {
        return "cxf-proxy-service-tests-config.xml";
    }

    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        setProperty("wsdl.uri", "ClientAndServiceProxy.wsdl");
    }

    @Test
    public void cxfProxyServiceDoesNotRemoveEnvelopeWhenUsingSAAJInInterceptor() throws Exception
    {
        String responseBodyAsString = makeSOAPRequest();
        Document parsedResponse = parseXMLDocumentFromString(responseBodyAsString);
        assertThat(parsedResponse.getDocumentElement().getTagName(), is(ENVELOPE_TAG_NAME));
    }

    @Test
    public void test() throws Exception
    {
        String soapRequestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:test=\"http://test.Pablo.name/\">"
                                 + "<soapenv:Header/>"
                                 + "<soapenv:Body>"
                                 + "<test:Hi/>"
                                 + "</soapenv:Body>"
                                 + "</soapenv:Envelope>";

        HttpClient client = new HttpClient();
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setSoTimeout(CLIENT_TIMEOUT);
        client.setParams(clientParams);

        PostMethod soapRequestPostMethod = new PostMethod("http://localhost:" + listenerPort.getNumber() + "/perro");
        StringRequestEntity soapPayload = new StringRequestEntity(soapRequestBody, "application/xml", "UTF-8");
        soapRequestPostMethod.setRequestEntity(soapPayload);

        client.executeMethod(soapRequestPostMethod);
        String response = soapRequestPostMethod.getResponseBodyAsString();
        Document parsedResponse = parseXMLDocumentFromString(response);
        assertThat(parsedResponse.getDocumentElement().getTagName(), is(ENVELOPE_TAG_NAME));
    }

    private String makeSOAPRequest() throws IOException
    {
        String soapRequestBody = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:test=\"http://test.Pablo.name/\">"
                                 + "<soapenv:Header/>"
                                 + "<soapenv:Body>"
                                 + "<test:Hi/>"
                                 + "</soapenv:Body>"
                                 + "</soapenv:Envelope>";

        HttpClient client = new HttpClient();
        HttpClientParams clientParams = new HttpClientParams();
        clientParams.setSoTimeout(CLIENT_TIMEOUT);
        client.setParams(clientParams);

        PostMethod soapRequestPostMethod = new PostMethod("http://localhost:" + listenerPort.getNumber() + "/proxy");
        StringRequestEntity soapPayload = new StringRequestEntity(soapRequestBody, "application/xml", "UTF-8");
        soapRequestPostMethod.setRequestEntity(soapPayload);

        client.executeMethod(soapRequestPostMethod);
        return soapRequestPostMethod.getResponseBodyAsString();
    }

    private Document parseXMLDocumentFromString(String responseBodyAsString) throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(responseBodyAsString));
        return documentBuilder.parse(inputSource);
    }

}
