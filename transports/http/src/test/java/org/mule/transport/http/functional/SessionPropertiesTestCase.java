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
import org.mule.tck.DynamicPortTestCase;
import org.mule.tck.FunctionalTestCase;

import java.util.Collections;

public class SessionPropertiesTestCase extends DynamicPortTestCase
{

    @Override
    protected String getConfigResources() {
        return "session-properties.xml";
    }

    public void testHttp1ToHttp2ToHttp3SessionPropertiesTestCase() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + getPorts().get(0) + "/Flow1s1", "some message", Collections.emptyMap());
        assertNotNullAndNotExceptionResponse(response);
    }

    public void testHttp1ToHttp2ThenHttp1ToHttp3SessionPropertiesTestCase() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("http://localhost:" + getPorts().get(3) + "/Flow1s2", "some message", Collections.emptyMap());
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


    @Override
    protected int getNumPortsToFind()
    {
        return 6;
    }
}
