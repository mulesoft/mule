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
package org.mule.samples.echo;

import org.mule.extras.client.MuleClient;
import org.mule.providers.NullPayload;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;
import org.custommonkey.xmlunit.XMLUnit;

import java.util.Map;
import java.util.HashMap;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisEchoTestCase extends FunctionalTestCase {

    public static final String ECHO_RESPONSE_SOAP = "<?xml version='1.0' encoding='UTF-8'?>" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">" +
            "<soap:Body>" +
            "<echoResponse xmlns=\"http://www.muleumo.org\">" +
            "<out xmlns=\"http://www.muleumo.org\">hello</out>" +
            "</echoResponse>" +
            "</soap:Body>" +
            "</soap:Envelope>";

    protected String getConfigResources() {
        return "echo-axis-config.xml";
    }

    protected String getProtocol() {
        return "axis";
    }

    public void testPostEcho() throws Exception {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("http://localhost:8081/services/EchoUMO?method=echo", "hello", null);
        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        XMLUnit.compareXML(result.getPayloadAsString(), ECHO_RESPONSE_SOAP);
    }

    public void testGetEcho() throws Exception {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("http.method", "GET");
        UMOMessage result = client.send("http://localhost:8081/services/EchoUMO?method=echo", "hello", props);
        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        XMLUnit.compareXML(result.getPayloadAsString(), ECHO_RESPONSE_SOAP);
    }

    public void testSoapPostEcho() throws Exception {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(getProtocol() + ":http://localhost:8082/services/EchoUMO?method=echo", "hello", null);
        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals("hello", result.getPayload());
    }
}
