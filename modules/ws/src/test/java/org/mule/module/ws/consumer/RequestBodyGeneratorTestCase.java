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
import java.util.Arrays;
import java.util.Collection;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class RequestBodyGeneratorTestCase extends AbstractMuleTestCase
{
    private static final String EXPECTED_BODY_PATTERN = "<ns:%s xmlns:ns=\"http://consumer.ws.module.mule.org/\" />";

    private static final String SERVICE_NAME = "TestParamsService";
    private static final String VALID_WSDL_FILE = "TestParams.wsdl";
    private static final String INVALID_WSDL_FILE = "TestParamsInvalid.wsdl";
    private static final String IMPORTED_TYPES_WSDL_FILE = "TestParamsImportedTypes.wsdl";

    private String port;

    public RequestBodyGeneratorTestCase(String port)
    {
        this.port = port;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[] {"TestParamsSoapPort"},
                             new Object[] {"TestParamsSoap12Port"});
    }

    @Test
    public void noRequestBodyForOperationWithParameter() throws Exception
    {
        String requestBody = generateRequestBody(VALID_WSDL_FILE, SERVICE_NAME, port, "echo");
        assertNull(requestBody);
    }

    @Test
    public void noRequestBodyForOperationWithParameterSimpleType() throws Exception
    {
        String requestBody = generateRequestBody(VALID_WSDL_FILE, SERVICE_NAME, port, "echoSimpleType");
        assertNull(requestBody);
    }

    @Test
    public void requestBodyGeneratedForOperationWithNoParameters() throws Exception
    {
        String requestBody = generateRequestBody(VALID_WSDL_FILE, SERVICE_NAME, port, "noParams");
        assertEquals(String.format(EXPECTED_BODY_PATTERN, "noParams"), requestBody);
    }

    @Test
    public void requestBodyGeneratedForOperationWithHeadersAndNoParameters() throws Exception
    {
        String requestBody = generateRequestBody(VALID_WSDL_FILE, SERVICE_NAME, port, "noParamsWithHeader");
        assertEquals(String.format(EXPECTED_BODY_PATTERN, "noParamsWithHeader"), requestBody);
    }

    @Test
    public void noRequestBodyForOperationWithNoParametersInInvalidWsdl() throws Exception
    {
        // Assert that if a WSDL has an invalid definition of types (for example because of a missing schema), we don't
        // create any request body (because we are unable to get the type for the XML element).
        String requestBody = generateRequestBody(INVALID_WSDL_FILE, SERVICE_NAME, port, "noParams");
        assertNull(requestBody);
    }

    @Test
    public void requestBodyGeneratedForOperationWithNoParametersImportedTypes() throws Exception
    {
        // This WSDL imports types from another WSDL with a different namespace. If this fails, the request body
        // generator will assume the operation requires parameters and it will return null. If the imports are resolved
        // correctly, then it will detect that the operation doesn't require parameters and a body will be generated.
        String requestBody = generateRequestBody(IMPORTED_TYPES_WSDL_FILE, SERVICE_NAME, port, "noParams");
        assertEquals(String.format(EXPECTED_BODY_PATTERN, "noParams"), requestBody);
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
