/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.model.resolvers.EntryPointNotFoundException;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class MethodEntryPointsTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/resolvers/method-entrypoints-config-flow.xml";
    }

    @Test
    public void testTooManySatisfiableMethods() throws Exception
    {
        try
        {
            flowRunner("Service").withPayload("hello").run().getMessage();
        }
        catch (Exception e)
        {
            assertThat(e.getCause(), instanceOf(EntryPointNotFoundException.class));
            assertThat(e.getMessage(), containsString("Found too many possible methods on object"));
        }
    }

    @Test
    public void testBadMethodName() throws Exception
    {
        try
        {
            flowRunner("Service").withPayload("hello").withInboundProperty("method", "foo").run().getMessage();
        }
        catch (Exception e)
        {
            assertThat(e.getCause(), instanceOf(EntryPointNotFoundException.class));
        }
    }

    @Test
    public void testValidCallToReverse() throws Exception
    {
        MuleMessage message = flowRunner("Service").withPayload("hello").withInboundProperty("method", "reverseString").run().getMessage();
        assertNotNull(message);
        assertEquals("olleh", getPayloadAsString(message));
    }

    @Test
    public void testValidCallToUpperCase() throws Exception
    {
        MuleMessage message = flowRunner("Service").withPayload("hello").withInboundProperty("method", "upperCaseString").run().getMessage();
        assertNotNull(message);
        assertEquals("HELLO", getPayloadAsString(message));
    }
}
