/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
