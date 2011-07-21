/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.construct.FlowConstruct;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ExceptionStrategyConfigTestCase extends AbstractServiceAndFlowTestCase
{
    public ExceptionStrategyConfigTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/exceptions/exception-strategy-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/exceptions/exception-strategy-config-flow.xml"}
        });
    }      
    
    @Test
    public void testConfig() throws Exception
    {
        FlowConstruct service = muleContext.getRegistry().lookupFlowConstruct("testService1");
        assertNotNull(service);
        assertNotNull(service.getExceptionListener());
        assertTrue(service.getExceptionListener() instanceof DefaultMessagingExceptionStrategy);

        DefaultMessagingExceptionStrategy es = (DefaultMessagingExceptionStrategy)service.getExceptionListener();
        assertFalse(es.isEnableNotifications());
        assertNotNull(es.getCommitTxFilter());
        assertEquals("java.io.*", es.getCommitTxFilter().getPattern());

        assertNotNull(es.getRollbackTxFilter());
        assertEquals("org.mule.*, javax.*", es.getRollbackTxFilter().getPattern());
    }
}
