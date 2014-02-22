/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.components.script;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class GroovyMessageBuilderTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "groovy-messagebuilder-config-flow.xml";
    }

    @Test
    public void testFunctionBehaviour() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage m = client.send("groovy1.endpoint", "Test:", null);
        assertNotNull(m);
        assertEquals("Test: A B Received", m.getPayloadAsString());
    }
}
