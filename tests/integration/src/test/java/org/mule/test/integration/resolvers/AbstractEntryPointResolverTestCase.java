/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Map;

public abstract class AbstractEntryPointResolverTestCase extends AbstractIntegrationTestCase
{

    protected void doTest(String flowName, Object payload, String result) throws Exception
    {
        doTest(flowName, payload, result, emptyMap());
    }

    protected void doTest(String flowName, Object payload, String result, Map properties) throws Exception
    {
        MuleMessage response = flowRunner(flowName).withPayload(payload).withInboundProperties(properties).run().getMessage();
        assertEquals(result, getPayloadAsString(response));
    }
}
