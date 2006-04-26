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
package org.mule.test.integration.providers.soap.xfire;

import org.mule.extras.client.MuleClient;
import org.mule.providers.http.HttpConnector;
import org.mule.test.integration.providers.soap.AbstractSoapUrlEndpointFunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.Map;


/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XfireConnectorHttpFunctionalTestCase extends AbstractSoapUrlEndpointFunctionalTestCase
{
    protected String getTransportProtocol() {
        return "http";
    }

    protected String getSoapProvider() {
        return "xfire";
    }

    public String getConfigResources() {
        return "org/mule/test/integration/providers/soap/xfire/xfire-" + getTransportProtocol() + "-mule-config.xml";
    }

    public void testLocationUrlInWSDL() throws Exception {
        Map props = new HashMap();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(getWsdlEndpoint(), null, props);
        assertNotNull(result);
        System.out.println(result.getPayloadAsString());
        assertTrue(result.getPayloadAsString().indexOf("wsdl:definitions") > -1);
    }
}
