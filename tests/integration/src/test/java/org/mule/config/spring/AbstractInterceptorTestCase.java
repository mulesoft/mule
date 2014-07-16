/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleException;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public abstract class AbstractInterceptorTestCase extends AbstractServiceAndFlowTestCase
{
    public static final String MESSAGE = "boo";

    public AbstractInterceptorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    public void assertMessageIntercepted() throws MuleException, InterruptedException
    {
        FunctionalTestAdvice advice = (FunctionalTestAdvice) muleContext.getRegistry().lookupObject("advice");
        assertNotNull("Cannot find advice", advice);
        String message = advice.getMessage(RECEIVE_TIMEOUT);
        assertEquals("Bad message", MESSAGE, message);
    }
}
