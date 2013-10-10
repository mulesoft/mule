/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNull;

public class InOutOutOnlyMessageCopyMule3007TestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/issues/inout-outonly-message-copy-mule3007-test.xml";
    }

    @Test
    public void testStreamMessage() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        String url = String.format("http://localhost:%1d/services", dynamicPort.getNumber());
        System.out.println(url);
        MuleMessage response = client.send(url, "test", null);
        assertNull(response.getExceptionPayload());
    }
}


