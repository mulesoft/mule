/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.issues;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertNull;

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
        MuleClient client = new MuleClient(muleContext);
        String url = String.format("http://localhost:%1d/services", port1.getNumber());
        System.out.println(url);
        MuleMessage response = client.send(url, "test", null);
        assertNull(response.getExceptionPayload());
    }
}
