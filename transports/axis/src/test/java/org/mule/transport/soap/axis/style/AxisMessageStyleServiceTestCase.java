/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.style;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.soap.axis.NamedParameter;
import org.mule.transport.soap.axis.SoapMethod;
import org.mule.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.junit.Rule;
import org.junit.Test;

public class AxisMessageStyleServiceTestCase extends FunctionalTestCase
{
    private static String expectedResult = "TEST RESPONSE";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    public String getConfigFile()
    {
        return "style/axis-mule-message-config.xml";
    }

    protected String getServiceEndpoint()
    {
        return "http://localhost:" + dynamicPort.getNumber() + "/ServiceEntryPoint";
    }

    @Test
    public void testDocumentWithNamespace() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "document"), "axis:" + getServiceEndpoint(), false,
            false, false);
    }

    @Test
    public void testDocumentWithQName() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "document"), "axis:" + getServiceEndpoint(), false,
            false, true);
    }

    @Test
    public void testDocumentWithAxisApi() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "document"), getServiceEndpoint(), true, false, false);
    }

    @Test
    public void testDocumentWithSoapMethod() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "document"), "axis:" + getServiceEndpoint(), false,
            true, false);
    }

    @Test
    public void testElementArrayWithSoapMethod() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "elementArray"), "axis:" + getServiceEndpoint(), false,
            true, false);
    }

    @Test
    public void testElementArrayWithNamesapce() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "elementArray"), "axis:" + getServiceEndpoint(), false,
            false, false);
    }

    @Test
    public void testElementArrayWithQName() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "elementArray"), "axis:" + getServiceEndpoint(), false,
            false, true);
    }

    @Test
    public void testElementArrayWithAxisApi() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "elementArray"), getServiceEndpoint(), true, false,
            false);
    }

    @Test
    public void testSoapBodyElementWithSoapMethod() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "soapBodyElement"), "axis:" + getServiceEndpoint(),
            false, true, false);
    }

    @Test
    public void testSoapBodyElementWithNamesapce() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "soapBodyElement"), "axis:" + getServiceEndpoint(),
            false, false, false);
    }

    @Test
    public void testSoapBodyElementWithQName() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "soapBodyElement"), "axis:" + getServiceEndpoint(),
            false, false, true);
    }

    @Test
    public void testSoapBodyElementWithAxisApi() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "soapBodyElement"), getServiceEndpoint(), true, false,
            false);
    }

    // TODO does work , complains about generated namespace...TestNS1
    // @Test
    //public void testSoapRequestResponseWithSoapMethod() throws Exception {
    // doSoapRequest(new QName("http://muleumo.org", "soapRequestResponse"), "axis:"
    // + getServiceEndpoint(), false, true, false);
    // }
    //
    // @Test
    //public void testSoapRequestResponseWithNamesapce() throws Exception {
    // doSoapRequest(new QName("http://muleumo.org", "soapRequestResponse"), "axis:"
    // + getServiceEndpoint(), false, false, false);
    // }
    //
    // @Test
    //public void testSoapRequestResponseWithQName() throws Exception {
    // doSoapRequest(new QName("http://muleumo.org", "soapRequestResponse"), "axis:"
    // + getServiceEndpoint(), false, false, true);
    // }

    @Test
    public void testSoapRequestResponseWithAxisApi() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "soapRequestResponse"), getServiceEndpoint(), true,
            false, false);
    }

    protected void doSoapRequest(QName method,
                                 String endpoint,
                                 boolean useAxisApi,
                                 boolean useSoapMethod,
                                 boolean useQNameMethod) throws Exception
    {
        if (useAxisApi)
        {
            Service service = new Service();
            Call call = (Call)service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName(method);
            String ret = (String)call.invoke(new Object[]{expectedResult});
            assertNotNull(ret);
            assertEquals(ret, expectedResult);
        }
        else
        {
            // Now try with the MuleClient
            MuleClient client = muleContext.getClient();

            Map<String, Object> props = new HashMap<String, Object>();
            if (useSoapMethod)
            {
                SoapMethod soapMethod = new SoapMethod(method);
                soapMethod.addNamedParameter(new QName(method.getNamespaceURI(), method.getLocalPart()),
                    NamedParameter.XSD_STRING, ParameterMode.IN);
                props.put(MuleProperties.MULE_METHOD_PROPERTY, soapMethod);
            }
            else if (useQNameMethod)
            {
                props.put(MuleProperties.MULE_METHOD_PROPERTY, method);
            }
            else
            {
                endpoint += "?method=" + method.getLocalPart();
                if (StringUtils.isNotBlank(method.getNamespaceURI()))
                {
                    props.put("methodNamespace", method.getNamespaceURI());
                }
            }

            MuleMessage result = client.send(endpoint, expectedResult, props);
            assertNotNull(result);
            assertEquals(expectedResult, result.getPayloadAsString());
        }
    }
}
