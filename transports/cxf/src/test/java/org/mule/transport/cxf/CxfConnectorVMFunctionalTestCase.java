/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.providers.soap.AbstractSoapResourceEndpointFunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

public class CxfConnectorVMFunctionalTestCase extends AbstractSoapResourceEndpointFunctionalTestCase
{

    public void testWSDL() throws Throwable
    {
        MuleClient client = new MuleClient();
        
        Map<String,Object> props = new HashMap<String, Object>();
        props.put("http.method", "GET");
        
        MuleMessage response = client.send("http://localhost:63081/test?wsdl", "", props);
        
        assertTrue(response.getPayloadAsString().indexOf("http://localhost:63081/test") != -1);
        
        // this doesn't work as now the endpoint is registered on a different url...
        testRequestResponse();
    }

    public String getConfigResources()
    {
        return getTransportProtocol() + "-mule-config.xml";
    }

    protected String getTransportProtocol()
    {
        return "vm";
    }

    protected String getSoapProvider()
    {
        return "cxf";
    }

}
