/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
