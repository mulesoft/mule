/*
 * $Id$
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
import org.mule.api.context.MuleContextBuilder;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.tck.AbstractMuleTestCase;

public class MuleContextLifecycleTestCase extends AbstractMuleTestCase
{

    private MuleContextBuilder ctxBuilder = new DefaultMuleContextBuilder();

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    public void testInitialise() throws MuleException
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        ctx.initialise();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        // Double initialisation does not fail.
        // MuleContext state stays the same
        ctx.initialise();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();
        // Attempt to initialise once started should fail!
        try
        {
            ctx.initialise();
            fail();
        }
        catch (Exception e)
        {
        }

        ctx.stop();
        // Attempt to initialise once stopped should fail!
        try
        {
            ctx.initialise();
            fail();
        }
        catch (Exception e)
        {
        }

        ctx.dispose();
        // Attempt to initialise once disposed should fail!
        try
        {
            ctx.initialise();
            fail();
        }
        catch (Exception e)
        {
        }
    }

    public void testStart() throws MuleException
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();

        // Attempt to start before initialise should fail!
        try
        {
            ctx.start();
            fail();
        }
        catch (Exception e)
        {
        }

        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertTrue(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        // Double start does not fail.
        // MuleContext state stays the same
        ctx.start();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertTrue(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        ctx.stop();
        ctx.start();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertTrue(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        ctx.dispose();
        // Attempt to start once disposed should fail!
        try
        {
            ctx.start();
            fail();
        }
        catch (Exception e)
        {
        }
    }

    public void testStop() throws MuleException
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();

        // Attempt to stop before initialise should fail!
        try
        {
            ctx.stop();
            fail();
        }
        catch (Exception e)
        {
        }

        ctx.initialise();
        ctx.stop();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();
        ctx.stop();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        ctx.start();
        ctx.stop();
        // Attempt to stop twice should fail!
        try
        {
            ctx.stop();
            fail();
        }
        catch (Exception e)
        {
        }

        ctx.dispose();
        // Attempt to start once disposed should fail!
        try
        {
            ctx.stop();
            fail();
        }
        catch (Exception e)
        {
        }
    }

    public void testDipose() throws MuleException
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();

        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();
        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();
        ctx.stop();
        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        ctx.dispose();
        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

    }
}
