/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.config.MuleProperties;
import org.mule.extras.client.MuleClient;
import org.mule.providers.soap.NamedParameter;
import org.mule.providers.soap.SoapMethod;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Requires an external Axis server running in Tomcat with the Calculator.jws service
 * deployed to it.
 */
public class AxisExternalServerTest extends AbstractMuleTestCase
{

    public void testAxisServiceRPC() throws Exception
    {
        String URL = "axis:http://localhost:8080/axis/Calculator.jws?method=add";
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(URL, new Object[]{new Integer(4), new Integer(3)}, null);
        assertNotNull(result);

        assertEquals(result.getPayload(), new Integer(7));
    }

    public void testAxisServiceDocLitWrapped() throws Exception
    {
        String URL = "axis:http://localhost:8080/axis/Calculator.jws?method=add";
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("style", "wrapped");
        props.put("use", "literal");
        UMOMessage result = client.send(URL, new Object[]{new Integer(3), new Integer(3)}, props);
        assertNotNull(result);

        assertEquals(result.getPayload(), new Integer(6));
    }

    public void testAxisServiceDocLitWrappedWithNamedParams() throws Exception
    {
        String URL = "axis:http://localhost:8080/axis/Calculator.jws";
        MuleClient client = new MuleClient();

        SoapMethod method = new SoapMethod(new QName("http://muleumo.org/Calc", "add"));
        method.addNamedParameter(new QName("Number1"), NamedParameter.XSD_INT, "in");
        method.addNamedParameter(new QName("Number2"), NamedParameter.XSD_INT, "in");
        method.setReturnType(NamedParameter.XSD_INT);

        Map props = new HashMap();
        props.put("style", "wrapped");
        props.put("use", "literal");
        props.put(MuleProperties.MULE_METHOD_PROPERTY, method);
        UMOMessage result = client.send(URL, new Object[]{new Integer(3), new Integer(3)}, props);
        assertNotNull(result);

        assertEquals(result.getPayload(), new Integer(6));
    }

    public void testAxisServiceDocLitWrappedWithNamedParamsinXml() throws Exception
    {

        MuleClient client = new MuleClient(
            "axis-client-endpoint-config.xml");

        UMOMessage result = client.send("calculatorAddEndpoint",
            new Object[]{new Integer(3), new Integer(3)}, null);
        assertNotNull(result);

        assertEquals(result.getPayload(), new Integer(6));
    }

    // The service is not hosted as Doc/Lit, so Axis will not allow us
    // to send a Doc/Lit request style soap message
    // public void testAxisServiceDocLit() throws Exception
    // {
    // String URL = "axis:http://localhost:8080/axis/Calculator.jws";
    // MuleClient client = new MuleClient();
    // Map props = new HashMap();
    // props.put("style", "document");
    // props.put("use", "literal");
    //         
    // SoapMethod method = new SoapMethod(new
    // QName("http://localhost:8080/axis/Calculator.jws", "add"));
    // method.addNamedParameter(new QName("i1"), NamedParameter.XSD_INT, "in");
    // method.addNamedParameter(new QName("i2"), NamedParameter.XSD_INT, "in");
    // method.setReturnType(NamedParameter.XSD_INT);
    // props.put(MuleProperties.MULE_METHOD_PROPERTY, method);
    //         
    // UMOMessage result = client.send(URL, new Object[]{new Integer(3), new
    // Integer(3)}, props);
    // assertNotNull(result);
    //        
    // assertEquals(result.getPayload(), new Integer(6));
    // }

    // wsdl-axis is currently disabled due to the problems axis had with this
    // feature
    // public void testAxisServiceUsingWSDL() throws Exception
    // {
    // String URL =
    // "wsdl-axis:http://localhost:8080/axis/Calculator.jws?wsdl&method=add";
    // MuleClient client = new MuleClient();
    //
    // UMOMessage result = client.send(URL, new Object[]{new Integer(4), new
    // Integer(4)}, null);
    // assertNotNull(result);
    //
    // assertEquals(result.getPayload(), new Integer(8));
    // }

}

