/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.junit.Rule;
import org.junit.Test;

public class AxisNamedParametersTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "axis-named-param-mule-config.xml";
    }

    @Test
    public void testNamedParameters() throws Exception
    {
        MuleClient client = muleContext.getClient();
        // The service itself will throw an exception if the parameters in the
        // request SOAP message are not named
        MuleMessage result = client.send("vm://mycomponent1", "Hello Named", null);
        assertEquals("Hello Named", result.getPayload());
    }

    @Test
    public void testNamedParametersViaClient() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        // create the soap method passing in the method name and return type
        SoapMethod soapMethod = new SoapMethod(new QName("echo"), NamedParameter.XSD_STRING);
        // add one or more parameters
        soapMethod.addNamedParameter(new QName("value"), NamedParameter.XSD_STRING, ParameterMode.IN);
        // set the soap method as a property and pass the properties
        // when making the call
        props.put(MuleProperties.MULE_SOAP_METHOD, soapMethod);

        MuleMessage result = client.send("axis:http://localhost:" + dynamicPort.getNumber() + "/mule/mycomponent2?method=echo",
            "Hello Named", props);
        assertEquals("Hello Named", result.getPayload());
    }
}
