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
package org.mule.test.integration.providers.soap.glue;

import org.mule.test.integration.providers.soap.AbstractSoapUrlEndpointFunctionalTestCase;
import org.mule.providers.http.HttpConnector;
import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOMessage;

import java.util.Map;
import java.util.HashMap;

/**
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GlueConnectorHttpFunctionalTestCase extends AbstractSoapUrlEndpointFunctionalTestCase
{
    protected String getSoapProvider() {
        return "glue";
    }

    protected String getTransportProtocol() {
        return "http";
    }

    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/glue/glue-http-mule-config.xml";
    }

    protected String getWsdlEndpoint() {
        return "http://127.0.0.1:38008/mule/mycomponent.wsdl";        
    }

    //I've had to overload this method because depending on the network adapter being used the WSDL location url will change
    //between the loopback address and machine ip
    public void testLocationUrlInWSDL() throws Exception {

        Map props = new HashMap();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(getWsdlEndpoint(), null, props);
        assertNotNull(result);
        System.out.println(result.getPayloadAsString());

        String location = getWsdlEndpoint();
        location = location.substring(0, location.length() - 5);
        if(location.endsWith("/")) {
            location = location.substring(0, location.length() - 1);
        }
        if(result.getPayloadAsString().indexOf("location=\"" + location) == -1) {
            assertTrue(result.getPayloadAsString().indexOf("location='") > -1);
        } else {
            assertTrue(result.getPayloadAsString().indexOf("location=\"") > -1);
        }
        System.out.println(result.getPayloadAsString());
    }
}
