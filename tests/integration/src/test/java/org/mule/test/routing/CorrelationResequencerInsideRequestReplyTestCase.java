/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.List;
import java.util.Map;


public class CorrelationResequencerInsideRequestReplyTestCase extends FunctionalTestCase
{

    @Parameters
    protected String getConfigFile()
    {
        return "correlation-resequencer-inside-request-reply-flow.xml";
    }

    @Test
    public void testResequencer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        List<String> payload = asList("a", "b", "c", "d", "e", "f");
        MuleMessage request = new DefaultMuleMessage(payload, (Map<String,Object>)null, muleContext);
        MuleMessage message = client.send("vm://splitter", request);

        assertThat((List<String>) message.getPayload(), equalTo(payload));
    }
}
