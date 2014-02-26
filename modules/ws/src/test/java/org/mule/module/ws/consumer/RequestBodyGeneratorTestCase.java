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

import org.junit.Test;

@SmallTest
public class RequestBodyGeneratorTestCase extends AbstractMuleTestCase
{
    private static final String EXPECTED_BODY_PATTERN = "<ns:%s xmlns:ns=\"http://consumer.ws.module.mule.org/\" />";

    @Test
    public void noRequestBodyForOperationWithParameter() throws Exception
    {
        String requestBody = generateRequestBody("TestParams.wsdl", "TestParamsService", "TestParamsPort", "echo");
        assertNull(requestBody);
    }

    @Test
    public void noRequestBodyForOperationWithParameterSimpleType() throws Exception
    {
        String requestBody = generateRequestBody("TestParams.wsdl", "TestParamsService", "TestParamsPort", "echoSimpleType");
        assertNull(requestBody);
    }

    @Test
    public void requestBodyGeneratedForOperationWithNoParameters() throws Exception
    {
        String requestBody = generateRequestBody("TestParams.wsdl", "TestParamsService", "TestParamsPort", "noParams");
        assertEquals(String.format(EXPECTED_BODY_PATTERN, "noParams"), requestBody);
    }

    @Test
    public void requestBodyGeneratedForOperationWithHeadersAndNoParameters() throws Exception
    {
        String requestBody = generateRequestBody("TestParams.wsdl", "TestParamsService", "TestParamsPort", "noParamsWithHeader");
        assertEquals(String.format(EXPECTED_BODY_PATTERN, "noParamsWithHeader"), requestBody);
    }

    @Test
    public void noRequestBodyForOperationWithNoParametersInInvalidWsdl() throws Exception
    {
        // Assert that if a WSDL has an invalid definition of types (for example because of a missing schema), we don't
        // create any request body (because we are unable to get the type for the XML element).
        String requestBody = generateRequestBody("TestParamsInvalid.wsdl", "TestParamsService", "TestParamsPort", "noParams");
        assertNull(requestBody);
    }

    private String generateRequestBody(String wsdlLocation, String serviceName, String portName, String operationName) throws Exception
    {
        URL url = IOUtils.getResourceAsUrl(wsdlLocation, getClass());

        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        Definition wsdlDefinition = wsdlReader.readWSDL(url.toString());
        Service service = wsdlDefinition.getService(new QName(wsdlDefinition.getTargetNamespace(), serviceName));
        Port port = service.getPort(portName);
        Binding binding = port.getBinding();

        RequestBodyGenerator requestBodyGenerator = new RequestBodyGenerator(wsdlDefinition);
        BindingOperation bindingOperation = binding.getBindingOperation(operationName, null, null);

        return requestBodyGenerator.generateRequestBody(bindingOperation);
    }

}
