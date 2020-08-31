/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.DefaultMuleConfiguration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FlowTraceManagerReinitializeTestCase {

    private MessageProcessingFlowTraceManager manager;

    private static boolean originalFlowTrace;

    @BeforeClass
    public static void beforeClass()
    {
        originalFlowTrace = DefaultMuleConfiguration.flowTrace;
        DefaultMuleConfiguration.flowTrace = true;
    }

    @AfterClass
    public static void afterClass()
    {
        DefaultMuleConfiguration.flowTrace = originalFlowTrace;
    }

    @Before
    public void before() throws InitialisationException
    {
        manager = new MessageProcessingFlowTraceManager();
        MuleContext context = mock(MuleContext.class);
        MuleConfiguration config = mock(MuleConfiguration.class);
        when(context.getConfiguration()).thenReturn(config);
        when(context.getNotificationManager()).thenReturn(new ServerNotificationManager());
        manager.setMuleContext(context);
        manager.initialise();
    }

    @Test
    public void reinitializeDoesntRemoveListeners() throws InitialisationException
    {
        manager = spy(manager);
        manager.initialise();
        verify(manager, never()).removeNotificationListeners();
    }
}
