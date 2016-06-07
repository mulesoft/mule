/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.ws.functional;


import org.mule.runtime.core.DefaultMuleMessage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class DynamicAddressFunctionalTestCase extends AbstractWSConsumerFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "dynamic-address-config.xml";
    }

    @Test
    public void returnsExpectedResponseWhenValidPathIsProvidedInboundProperty() throws Exception
    {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("pathInboundProperty", "services/Test");
        assertValidResponse("clientInboundProperty", new DefaultMuleMessage(ECHO_REQUEST, properties, null, null, muleContext));
    }

    @Test
    public void returnsExpectedResponseWhenValidPathIsProvidedOutboundProperty() throws Exception
    {
        assertValidResponse("clientOutboundProperty", new HashMap<>());
    }

    @Test
    public void returnsExpectedResponseWhenValidPathIsProvidedFlowVar() throws Exception
    {
        assertValidResponse("clientFlowVar", new HashMap<>());
    }

    @Test
    public void returnsExpectedResponseWhenValidPathIsProvidedSessionVar() throws Exception
    {
        assertValidResponse("clientSessionVar", new HashMap<>());
    }

    @Test
    public void failsWhenInvalidPathIsProvided() throws Exception
    {
        Map<String, Serializable> properties = new HashMap<>();
        properties.put("clientInboundProperty", "invalid");
        assertSoapFault("clientInboundProperty", ECHO_REQUEST, properties, "Client");
    }

    @Test
    public void failsWhenNoPathIsDefined() throws Exception
    {
        assertSoapFault("clientInboundProperty", ECHO_REQUEST, "Client");
    }
}
