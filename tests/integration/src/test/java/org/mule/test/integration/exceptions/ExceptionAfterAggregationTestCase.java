/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.functional.junit4.runners.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class ExceptionAfterAggregationTestCase extends AbstractIntegrationTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private final String configResources;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"exception-after-aggregation-test-config-simple.xml"},
        });
    }

    public ExceptionAfterAggregationTestCase(String configResources)
    {
        super();
        this.configResources = configResources;
    }

    @Override
    protected String getConfigResources()
    {
        return configResources;
    }

    @Test
    public void testReceiveCorrectExceptionAfterAggregation() throws Exception
    {
        expectedException.expectMessage("Ad hoc message exception");
        flowRunner("main").withPayload(getTestMuleMessage("some data")).run();
    }
}
