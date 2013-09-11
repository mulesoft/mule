/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class InOutOutOnlyMessageCopyMule3007TestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/issues/inout-outonly-message-copy-mule3007-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/issues/inout-outonly-message-copy-mule3007-test-flow.xml"}
        });
    }

    public InOutOutOnlyMessageCopyMule3007TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testStreamMessage() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        String url = String.format("http://localhost:%1d/services", port1.getNumber());
        MuleMessage response = client.send(url, "test", null);
        assertNull(response.getExceptionPayload());
    }
}
