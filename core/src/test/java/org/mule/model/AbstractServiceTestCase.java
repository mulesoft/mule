/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model;

import org.mule.api.MuleException;
import org.mule.api.service.Service;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class AbstractServiceTestCase extends AbstractMuleContextTestCase
{
    protected abstract Service getService();

    @Test
    public void testStart() throws MuleException
    {
        try
        {
            getService().start();
            fail("Exception expected: Cannot start an uninitialised service");
        }
        catch (Exception e)
        {
            // expected
        }

        getService().initialise();
        getService().start();

        try
        {
            getService().initialise();
            fail("Exception expected: Cannot initialise an already initialised service");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        getService().dispose();

    }

    @Test
    public void testPause() throws MuleException
    {
        assertFalse(getService().isStarted());
        assertFalse(getService().isPaused());
        assertFalse(getService().isStopped());

        getService().initialise();

        // Pausing a service that is not started does not throw an exception
        assertFalse(getService().isStarted());
        assertFalse(getService().isPaused());
        assertFalse(getService().isStopped());
        try
        {
            getService().resume();
            fail("cannot resume a service that is not paused");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        assertFalse(getService().isPaused());
        getService().start();
        assertTrue(getService().isStarted());
        assertFalse(getService().isPaused());
        assertFalse(getService().isStopped());
        getService().pause();
        assertTrue(getService().isPaused());
        assertFalse(getService().isStarted());
        assertFalse(getService().isStopped());
        try
        {
            getService().pause();
            fail("cannot pause a service that is already paused");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        assertTrue(getService().isPaused());
        getService().dispose();

    }

    @Test
    public void testResume() throws MuleException
    {
        assertFalse(getService().isStarted());
        assertFalse(getService().isPaused());

        getService().initialise();

        assertFalse(getService().isStarted());
        assertFalse(getService().isPaused());
        try
        {
            getService().resume();
            fail("cannot resume a service that is not paused");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        assertFalse(getService().isPaused());
        getService().start();
        assertTrue(getService().isStarted());
        assertFalse(getService().isPaused());
        try
        {
            getService().resume();
            fail("cannot resume a service that is not paused");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        assertFalse(getService().isPaused());
        getService().pause();
        assertTrue(getService().isPaused());
        getService().resume();
        assertFalse(getService().isPaused());
        // Resume is a meta phase, so after pause, we go back to started
        assertTrue(getService().isStarted());
        try
        {
            getService().resume();
            fail("cannot resume a service that is not paused");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        assertFalse(getService().isPaused());
        getService().dispose();

    }

    @Test
    public void testStop() throws MuleException
    {
        assertFalse(getService().isStarted());
        assertFalse(getService().isPaused());

        try
        {
            getService().stop();
            fail("Exception expected: Cannot stop an uninitialised service");
        }
        catch (IllegalStateException e)
        {
            // expected
        }

        try
        {
            getService().resume();
            fail("Exception expected: Cannot resume an uninitialised service");
        }
        catch (IllegalStateException e)
        {
            // expected
        }

        getService().initialise();
        assertFalse(getService().isStarted());

        // Can stop a service that is not started
        getService().stop();

        assertFalse(getService().isStarted());
        getService().start();
        assertTrue(getService().isStarted());
        getService().stop();
        assertFalse(getService().isStarted());
        try
        {
            getService().stop();
            fail("Exception expected: Cannot stop a service that is not started");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        assertFalse(getService().isStarted());
        getService().dispose();

    }

    @Test
    public void testDispose() throws MuleException
    {
        assertFalse(getService().isStarted());
        assertFalse(getService().isPaused());
        getService().dispose();

        try
        {
            getService().dispose();
            fail("Exception expected: Cannot dispose a service that is already disposed");
        }
        catch (IllegalStateException e)
        {
            // expected
        }

        try
        {
            getService().initialise();
            fail("Exception expected: Cannot invoke initialise (or any lifecycle) on an object once it is disposed");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
    }

}
