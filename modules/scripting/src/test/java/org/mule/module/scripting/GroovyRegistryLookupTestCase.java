/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class GroovyRegistryLookupTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "groovy-registry-lookup-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        muleContext.getRegistry().registerObject("hello", new Hello());
    }

    @Test
    public void testBindingCallout() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://test", "", null);
        assertNotNull(response);
        assertEquals("hello", response.getPayloadAsString());
    }

    public static class Hello
    {
        public String sayHello()
        {
            return "hello";
        }
    }
}
