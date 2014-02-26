/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.IOUtils;

import java.net.URL;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class RequestBodyGeneratorTestCase extends AbstractMuleTestCase
{

    private static final String EXPECTED_BODY_PATTERN = "<ns:%s xmlns:ns=\"http://consumer.ws.module.mule.org/\" />";

    private Definition wsdlDefinition;
    private Service service;
    private Port port;
    private Binding binding;
    private RequestBodyGenerator requestBodyGenerator;

    @Before
    public void setup() throws Exception
    {
        URL url = IOUtils.getResourceAsUrl("TestParams.wsdl", getClass());
        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        wsdlDefinition = wsdlReader.readWSDL(url.toString());
        service = wsdlDefinition.getService(new QName(wsdlDefinition.getTargetNamespace(), "TestParamsService"));
        port = service.getPort("TestParamsPort");
        binding = port.getBinding();
        requestBodyGenerator = new RequestBodyGenerator(wsdlDefinition);
    }

    @Test
    public void noRequestBodyForOperationWithParameter()
    {
        assertNoRequestBody("echo");
    }

    @Test
    public void noRequestBodyForOperationWithParameterSimpleType()
    {
        assertNoRequestBody("echoSimpleType");
    }

    @Test
    public void requestBodyGeneratedForOperationWithNoParameters()
    {
        assertRequestBody("noParams");
    }

    @Test
    public void requestBodyGeneratedForOperationWithHeadersAndNoParameters()
    {
        assertRequestBody("noParamsWithHeader");
    }


    private void assertNoRequestBody(String operationName)
    {
        BindingOperation operation = binding.getBindingOperation(operationName, null, null);
        String requestBody = requestBodyGenerator.generateRequestBody(operation);
        assertNull(requestBody);
    }

    private void assertRequestBody(String operationName)
    {
        BindingOperation operation = binding.getBindingOperation(operationName, null, null);
        String requestBody = requestBodyGenerator.generateRequestBody(operation);
        assertEquals(String.format(EXPECTED_BODY_PATTERN, operationName), requestBody);
    }

}
