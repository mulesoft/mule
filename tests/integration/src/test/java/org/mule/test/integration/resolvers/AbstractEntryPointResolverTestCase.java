/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import static org.junit.Assert.assertEquals;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.functional.junit4.FunctionalTestCase;

import java.util.Map;

public abstract class AbstractEntryPointResolverTestCase extends FunctionalTestCase
{

    protected void doTest(String flowName, Object payload, String result) throws Exception
    {
        doTest(flowName, payload, result, null);
    }

    protected void doTest(String flowName, Object payload, String result, Map properties) throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(payload, properties, null, null);
        MuleMessage response = flowRunner(flowName).withPayload(message).run().getMessage();
        assertEquals(result, getPayloadAsString(response));
    }
}
