/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import static org.mule.tck.functional.FlowAssert.verify;

import org.junit.Test;

/**
 * Tests the correct propagation of the correlation id property within the JMS transport. This test is related to MULE-6577.
 */
public class JmsCorrelationIdPropagationTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "integration/jms-correlation-id-propagation.xml";
    }

    @Test
    public void testMuleCorrelationIdPropagation() throws Exception
    {
        runFlow("withMuleCorrelationId");
        verifyPropagation();
    }

    @Test
    public void testCustomCorrelationIdPropagation() throws Exception
    {
        runFlow("withCustomCorrelationId");
        verifyPropagation();
    }

    @Test
    public void testNoCorrelationIdPropagation() throws Exception
    {
        runFlow("withNoCorrelationId");
        verifyPropagation();
    }

    protected void verifyPropagation() throws Exception
    {
        verify("withCorrelationIdBridge");
        verify("withCorrelationIdOut");
    }

}
