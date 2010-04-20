/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations.example;


import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.util.Properties;

public class HelloComponentFunctionalTestCase extends FunctionalTestCase
{
    @Override
    protected Properties getStartUpProperties()
    {
        //these will be made available at start up
        Properties p = new Properties();
        try
        {
            p.load(IOUtils.getResourceAsStream("hello-annotation-test.properties", getClass()));
            return p;
        }
        catch (IOException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    @Override
    protected String getConfigResources()
    {
        return "hello-annotation-service.xml";
    }

    public void testHelloComponent() throws Exception
    {
        MuleClient client = new MuleClient();

        MuleMessage message = client.send("helloEndpoint", "Ross", null);

        assertNotNull(message);
        assertEquals("Good day to you Ross", message.getPayloadAsString());
    }
}

