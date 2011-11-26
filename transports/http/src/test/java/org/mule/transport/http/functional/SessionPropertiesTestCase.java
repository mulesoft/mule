/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SessionPropertiesTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    @Rule
    public DynamicPort dynamicPort4 = new DynamicPort("port4");

    @Rule
    public DynamicPort dynamicPort5 = new DynamicPort("port5");

    @Rule
    public DynamicPort dynamicPort6 = new DynamicPort("port6");

    @Override
    protected String getConfigResources() {
        return "session-properties.xml";
    }

    @Test
    public void testHttp1ToHttp2ToHttp3SessionPropertiesTestCase() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort1.getNumber() + "/Flow1s1", "some message", Collections.emptyMap());
        assertNotNullAndNotExceptionResponse(response);
    }

    @Test
    public void testHttp1ToHttp2ThenHttp1ToHttp3SessionPropertiesTestCase() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort4.getNumber() + "/Flow1s2", "some message", Collections.emptyMap());
        assertNotNullAndNotExceptionResponse(response);
    }

    private void assertNotNullAndNotExceptionResponse(MuleMessage response)
    {
        assertNotNull(response);
        if (response.getExceptionPayload() != null)
        {
            fail(response.getExceptionPayload().getException().getCause().toString());
        }
    }

}
