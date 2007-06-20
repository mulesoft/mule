/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis.style;

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.providers.soap.NamedParameter;
import org.mule.providers.soap.SoapMethod;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.mule.util.StringUtils;

public class AxisMessageStyleServiceWithoutNamespaceTestCase extends FunctionalTestCase
{
    private static String expectedResult = "TEST RESPONSE";

    public AxisMessageStyleServiceWithoutNamespaceTestCase()
    {
        setDisposeManagerPerSuite(true);
    }

    public String getConfigResources()
    {
        return "style/axis-mule-message-config-without-namespace.xml";
    }

    protected static String getServiceEndpoint()
    {
        return "http://localhost:8088/ServiceEntryPoint";
    }

    public void testDocumentWithNamesapce() throws Exception
    {
        doSoapRequest(new QName("document"), "axis:" + getServiceEndpoint(), false, false, false);
    }

    public void testDocumentWithQName() throws Exception
    {
        doSoapRequest(new QName("document"), "axis:" + getServiceEndpoint(), false, false, true);
    }

    public void testDocumentWithAxisApi() throws Exception
    {
        doSoapRequest(new QName("document"), getServiceEndpoint(), true, false, false);
    }

    public void testDocumentWithSoapMethod() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "document"), "axis:" + getServiceEndpoint(), false,
            true, false);
    }

    public void testElementArrayWithSoapMethod() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "elementArray"), "axis:" + getServiceEndpoint(), false,
            true, false);
    }

    public void testElementArrayWithNamesapce() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "elementArray"), "axis:" + getServiceEndpoint(), false,
            false, false);
    }

    public void testElementArrayWithQName() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "elementArray"), "axis:" + getServiceEndpoint(), false,
            false, true);
    }

    public void testElementArrayWithAxisApi() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "elementArray"), getServiceEndpoint(), true, false,
            false);
    }

    public void testSoapBodyElementWithSoapMethod() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "soapBodyElement"), "axis:" + getServiceEndpoint(),
            false, true, false);
    }

    public void testSoapBodyElementWithNamesapce() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "soapBodyElement"), "axis:" + getServiceEndpoint(),
            false, false, false);
    }

    public void testSoapBodyElementWithQName() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "soapBodyElement"), "axis:" + getServiceEndpoint(),
            false, false, true);
    }

    public void testSoapBodyElementWithAxisApi() throws Exception
    {
        doSoapRequest(new QName("http://muleumo.org", "soapBodyElement"), getServiceEndpoint(), true, false,
            false);
    }

    // Todo does work , complains about generated namespace...TestNS1
    // public void testSoapRequestResponseWithSoapMethod() throws Exception {
    // doSoapRequest(new QName("http://muleumo.org", "soapRequestResponse"), "axis:"
    // + getServiceEndpoint(), false, true, false);
    // }
    //
    // public void testSoapRequestResponseWithNamesapce() throws Exception {
    // doSoapRequest(new QName("http://muleumo.org", "soapRequestResponse"), "axis:"
    // + getServiceEndpoint(), false, false, false);
    // }
    //
    // public void testSoapRequestResponseWithQName() throws Exception {
    // doSoapRequest(new QName("http://muleumo.org", "soapRequestResponse"), "axis:"
    // + getServiceEndpoint(), false, false, true);
    // }

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
            MuleClient client = new MuleClient();
            Map props = new HashMap();
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

            UMOMessage result = client.send(endpoint, expectedResult, props);
            assertNotNull(result);
            assertEquals(expectedResult, result.getPayloadAsString());
        }
    }

}
