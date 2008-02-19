/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.tck.FunctionalTestCase;
import org.mule.api.MuleException;

public abstract class AbstractInterceptorTestCase extends FunctionalTestCase
{

    public static final String MESSAGE = "boo";

    public void assertMessageIntercepted() throws MuleException, InterruptedException
    {
        FunctionalTestAdvice advice = (FunctionalTestAdvice) muleContext.getRegistry().lookupObject("advice");
        assertNotNull("Cannot find advice", advice);
        String message = advice.getMessage(RECEIVE_TIMEOUT);
        assertEquals("Bad message", MESSAGE, message);
    }

}