/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ForeachUntilSuccessfulTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "for-each-until-config.xml";
    }

    @Test
    public void flowVariablesSyncArePropagated() throws Exception
    {
        runAndAssert("flowVarSync");
    }

    @Test
    public void flowVariablesAsyncArePropagated() throws Exception
    {
        runAndAssert("flowVarAsync");
    }

    private void runAndAssert(String flowName) throws Exception
    {
        MuleEvent event = runFlow(flowName, newArrayList(1, 2, 3));
        assertThat((Integer) event.getFlowVariable("count"), is(6));
        assertThat((Integer) event.getFlowVariable("current"), is(3));
    }
}
