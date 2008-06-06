/*
 * $Id: DefaultMuleContext.java 11517 2008-03-31 21:34:19Z dirk.olmes $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.tck.AbstractMuleTestCase;

public class DefaultMuleContextTestCase extends AbstractMuleTestCase
{

    public void testDisposal() throws MuleException, InterruptedException
    {
        int threadsBeforeStart = Thread.activeCount();
        MuleContext ctx = new DefaultMuleContextFactory().createMuleContext();
        ctx.start();
        assertTrue(Thread.activeCount() > threadsBeforeStart);
        ctx.stop();
        ctx.dispose();
        // Check that workManager ("MuleServer") thread no longer exists.
        assertTrue(Thread.activeCount() == threadsBeforeStart);
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isStarted());
    }

    // @Override
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    // @Override
    protected void disposeManager()
    {
    }

}
