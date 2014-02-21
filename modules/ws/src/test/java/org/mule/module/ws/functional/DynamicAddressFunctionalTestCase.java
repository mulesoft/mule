/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.functional;


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
    public void returnsExpectedResponseWhenValidPathIsProvided() throws Exception
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("path", "services/Test");
        assertValidResponse("vm://in", properties);
    }

    @Test
    public void failsWhenInvalidPathIsProvided() throws Exception
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("path", "invalid");
        assertSoapFault("vm://in", ECHO_REQUEST, properties, "Client");
    }

    @Test
    public void failsWhenNoPathIsDefined() throws Exception
    {
        assertSoapFault("vm://in", ECHO_REQUEST, "Client");
    }
}
