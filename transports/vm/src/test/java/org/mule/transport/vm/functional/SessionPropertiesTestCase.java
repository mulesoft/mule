/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;

public class SessionPropertiesTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "vm/session-properties.xml";
    }

    @Test
    public void testVmToVmSessionPropertiesTestCase() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> properties = Collections.emptyMap();
        MuleMessage response = client.send("vm://Flow1s1", "some message", properties, 1200000);
        assertNotNullAndNotExceptionResponse(response);
    }

    @Test
    public void testVm1ToVm2ThenVm1ToVm2SessionPropertiesTestCase() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> properties = Collections.emptyMap();
        MuleMessage response = client.send("vm://Flow1s2", "some message", properties, 1200000);
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
