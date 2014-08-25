/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.log4j2;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.message.MessageFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MuleLoggerContextTestCase extends AbstractMuleTestCase
{

    private static final String DEFAULT_CONTEXT_NAME = "Default";

    @Mock
    private ContextSelector contextSelector;

    @Mock
    private MessageFactory messageFactory;


    @Test
    public void dispatchingLogger()
    {
        MuleLoggerContext ctx = getDefaultContext();
        assertThat(ctx.newInstance(ctx, "", messageFactory), instanceOf(DispatchingLogger.class));
    }

    private MuleLoggerContext getDefaultContext()
    {
        return new MuleLoggerContext(DEFAULT_CONTEXT_NAME, null, Thread.currentThread().getContextClassLoader(), contextSelector, true);
    }
}
