/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.exception.CatchMessagingExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;

public class DefaultExceptionStrategyUsingReferenceTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/default-exception-strategy-using-reference.xml";
    }

    @Test
    public void testGlobalExceptionStrategyUsingReference()
    {
        FlowConstruct flowNoExceptionStrategy = muleContext.getRegistry().lookupFlowConstruct("flowNoExceptionStrategy");
        MessagingExceptionHandler flowNoExceptionStrategyExceptionListener = flowNoExceptionStrategy.getExceptionListener();
        assertThat(flowNoExceptionStrategyExceptionListener, instanceOf(CatchMessagingExceptionStrategy.class));
    }

}
