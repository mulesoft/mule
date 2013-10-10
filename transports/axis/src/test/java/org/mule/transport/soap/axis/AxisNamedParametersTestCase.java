/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AxisNamedParametersTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "axis-named-param-mule-config.xml";
    }

    @Test
    public void testNamedParameters() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        // The service itself will throw an exception if the parameters in the
        // request SOAP message are not named
        MuleMessage result = client.send("vm://mycomponent1", "Hello Named", null);
        assertEquals("Hello Named", result.getPayload());
    }

    @Test
    public void testNamedParametersViaClient() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
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
