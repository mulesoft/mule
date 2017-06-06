/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.processor.AbstractFilteringMessageProcessor.FILTERS_STOP_ALL_FLOW_CALLERS;
import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class MessageFilterFunctionalTestCase extends FunctionalTestCase
{

    @Rule
    public SystemProperty filterStopAllFlowCallersSystemProperty = new SystemProperty(FILTERS_STOP_ALL_FLOW_CALLERS, "true");

    private static boolean componentWasCalled = false;

    @Override
    protected String getConfigFile()
    {
        return "message-filter-config.xml";
    }

    @Test
    public void testFlowCallerStopsAfterUnacceptedEvent() throws Exception
    {
        runFlow("MainFlow");
        assertThat(componentWasCalled, is(false));
    }

    public static class TestJavaComponent implements Callable {

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            componentWasCalled = true;
            return eventContext.getMessage();
        }
    }
}
