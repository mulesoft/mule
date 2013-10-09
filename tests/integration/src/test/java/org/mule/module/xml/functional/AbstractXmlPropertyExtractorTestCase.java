/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractXmlPropertyExtractorTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/xml/property-extractor-test.xml"}

        });
    }

    public AbstractXmlPropertyExtractorTestCase(ConfigVariant variant, String configResources, boolean matchSingle)
    {
        super(variant, configResources);
        this.matchSingle = matchSingle;
    }

    private boolean matchSingle = true;

    protected abstract Object getMatchMessage() throws Exception;

    protected abstract Object getErrorMessage() throws Exception;

    @Test
    public void testMatch() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", getMatchMessage(), null);
        MuleMessage message = client.request("vm://match1", RECEIVE_TIMEOUT);

        assertNotNull(message);
        assertFalse(message.getPayload() instanceof NullPayload);
        if(!matchSingle)
        {
            message = client.request("vm://match2", RECEIVE_TIMEOUT);
            assertNotNull(message);
            assertFalse(message.getPayload() instanceof NullPayload);
        }
    }

    @Test
    public void testError() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in", getErrorMessage(), null);
        MuleMessage message = client.request("vm://error", RECEIVE_TIMEOUT);
        assertNotNull(message);
        assertFalse(message.getPayload() instanceof NullPayload);
    }
}
