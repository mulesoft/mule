/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.mule.extras.client.MuleClient;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleClientWsdlSoapExternalTestCase extends AbstractMuleTestCase {
    public static final String WSDL_URL = "http://www.dataaccess.com/webservicesserver/conversions.wso?WSDL";
    public static final String METHOD = "NumberToWords";
    public static final String INPUT = "24";
    public static final String OUTPUT = "twenty four";

    public void testXFireWsdlRequestResponse() throws Throwable {
        if (isOffline("org.mule.test.integration.client.MuleClientXFireExternalTestCase.testRequestResponse()")) {
            return;
        }

        String url = "wsdl-xfire:" + WSDL_URL + "&method=" + METHOD;
        MuleClient client = null;
        try {
            client = new MuleClient();
        } catch (UMOException e) {
            e.printStackTrace();
        }
        UMOMessage result = client.send(url, INPUT, null);
        assertNotNull(result);
        assertEquals(OUTPUT, result.getPayload());
    }

    //This doesn't work as Axis WSDL parser doesn't grab the param names from the schema for some reason...

//    public void testAxisWsdlRequestResponseAuto() throws Throwable {
//        if (isOffline("org.mule.test.integration.client.MuleClientXFireExternalTestCase.testAxisWsdlRequestResponse()")) return;
//        Map properties = new HashMap();
//        //properties.put(AxisConnector.SOAP_ACTION_PROPERTY, "${methodNamespace}#${method}");
//        //properties.put(AxisConnector.METHOD_NAMESPACE_PROPERTY, "urn:xmethods-delayed-quotes");
//
//        properties.put("style", "document");
//        properties.put("use", "literal");
//        String url = "wsdl-axis:" + WSDL_URL + "&method=" + METHOD;
//        MuleClient client = null;
//        client = new MuleClient();
//        UMOMessage result = client.send(url, INPUT, properties);
//        assertNotNull(result);
//        assertEquals(OUTPUT, result.getPayload());
//    }


    public void testDiscoveryWsdlRequestResponse() throws Throwable {
        if(isOffline("org.mule.test.integration.client.MuleClientXFireExternalTestCase.testDiscoveryWsdlRequestResponse()")) {
            return;
        }

        String url = "wsdl:" + WSDL_URL + "&method=" + METHOD;
        MuleClient client = null;
        client = new MuleClient();
        UMOMessage result = client.send(url, INPUT, null);
        assertNotNull(result);
        assertEquals(OUTPUT, result.getPayload());
    }

}
