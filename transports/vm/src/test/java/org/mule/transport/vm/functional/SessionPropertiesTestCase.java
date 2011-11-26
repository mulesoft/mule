/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.vm.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SessionPropertiesTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "vm/session-properties.xml";
    }

    @Test
    public void testVmToVmSessionPropertiesTestCase() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://Flow1s1", "some message", Collections.emptyMap(), 1200000);
        assertNotNullAndNotExceptionResponse(response);
    }

    @Test
    public void testVm1ToVm2ThenVm1ToVm2SessionPropertiesTestCase() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://Flow1s2", "some message", Collections.emptyMap(), 1200000);
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
