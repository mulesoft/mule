/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;


import static org.junit.Assert.assertEquals;

import org.mule.api.MuleEvent;

import org.junit.Test;

public class DynamicRoundRobinTestCase extends AbstractDynamicRoundRobinTestCase
{

    @Test
    public void testDynamicRoundRobinWithDefaultIdentifier() throws Exception
    {
        DynamicRoundRobin dynamicRoundRobin = getDynamicRoundRobin(getDynamicRouteResolver());
        MuleEvent eventToProcessId1 = getEventWithId(ID_1);
        MuleEvent eventToProcessId2 = getEventWithId(ID_2);
        assertEquals(LETTER_A, dynamicRoundRobin.process(eventToProcessId1).getMessage().getPayloadAsString());
        assertEquals(LETTER_B, dynamicRoundRobin.process(eventToProcessId2).getMessage().getPayloadAsString());
        assertEquals(LETTER_C, dynamicRoundRobin.process(eventToProcessId1).getMessage().getPayloadAsString());
        assertEquals(LETTER_A, dynamicRoundRobin.process(eventToProcessId2).getMessage().getPayloadAsString());
    }

    @Test
    public void testDynamicRoundRobinWithIdentifier() throws Exception
    {
        DynamicRoundRobin dynamicRoundRobin = getDynamicRoundRobin(getIdentifiableDynamicRouteResolver());
        MuleEvent eventToProcessId1 = getEventWithId(ID_1);
        MuleEvent eventToProcessId2 = getEventWithId(ID_2);
        assertEquals(LETTER_A, dynamicRoundRobin.process(eventToProcessId1).getMessage().getPayloadAsString());
        assertEquals(LETTER_A, dynamicRoundRobin.process(eventToProcessId2).getMessage().getPayloadAsString());
        assertEquals(LETTER_B, dynamicRoundRobin.process(eventToProcessId1).getMessage().getPayloadAsString());
        assertEquals(LETTER_B, dynamicRoundRobin.process(eventToProcessId2).getMessage().getPayloadAsString());
    }

    private DynamicRoundRobin getDynamicRoundRobin(DynamicRouteResolver dynamicRouteResolver) throws Exception
    {
        DynamicRoundRobin dynamicRoundRobin = new DynamicRoundRobin();
        dynamicRoundRobin.setMuleContext(muleContext);
        dynamicRoundRobin.setDynamicRouteResolver(dynamicRouteResolver);
        dynamicRoundRobin.initialise();
        return dynamicRoundRobin;
    }

}
