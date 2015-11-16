/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

public abstract class AbstractXmlPropertyExtractorTestCase extends FunctionalTestCase
{

    public AbstractXmlPropertyExtractorTestCase(boolean matchSingle)
    {
        this.matchSingle = matchSingle;
    }

    @Override
    protected String getConfigFile()
    {
        return  "org/mule/module/xml/property-extractor-test.xml";
    }

    private boolean matchSingle = true;

    protected abstract Object getMatchMessage() throws Exception;

    protected abstract Object getErrorMessage() throws Exception;

    @Test
    public void testMatch() throws Exception
    {
        MuleClient client = muleContext.getClient();
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
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", getErrorMessage(), null);
        MuleMessage message = client.request("vm://error", RECEIVE_TIMEOUT);
        assertNotNull(message);
        assertFalse(message.getPayload() instanceof NullPayload);
    }
}
