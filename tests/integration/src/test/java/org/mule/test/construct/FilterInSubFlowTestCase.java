/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;

import org.junit.Test;

public class FilterInSubFlowTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/sub-flow-with-filter-config.xml";
    }

    @Test
    public void filterOnFlow() throws Exception
    {
        MuleEvent response = flowRunner("flowWithFilter").withPayload("TEST").run();
        assertEquals(null, response);
    }

    @Test
    public void filterOnSubFlow() throws Exception
    {
        MuleEvent response = flowRunner("flowWithSubFlowWithFilter").withPayload("TEST").run();
        assertEquals(null, response);
    }
}
