/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("ArtifactClassloaderTestRunner groovy classCastExceptionerror")
public class InOptionalOutOutOnlyAsyncRouterTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/pattern_In-Optional-Out_Out-Only-Async-Router-flow.xml";
    }

    @Test
    public void testExchange() throws Exception
    {
        FlowRunner baseRunner = flowRunner("In-Out_Out-Only-Async-Service").withPayload("some data");
        MuleEvent event = baseRunner.run();
        assertNull(event);

        baseRunner.reset();
        MuleMessage result = baseRunner.withInboundProperty("foo", "bar")
                                       .run()
                                       .getMessage();

        assertNotNull(result);
        assertEquals("got it!", getPayloadAsString(result));
    }
}
