/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.tck.AbstractMuleTestCase;

public abstract class AbstractServiceTestCase extends AbstractMuleTestCase
{

    protected Service service;

    public void testStart() throws MuleException
    {
        try
        {
            service.start();
            fail("Exception expected: Cannot start an uninitialised service");
        }
        catch (Exception e)
        {
            // expected
        }

        service.initialise();
        service.start();

        try
        {
            service.initialise();
            fail("Exception expected: Cannot initialise an already initialised service");
        }
        catch (InitialisationException e)
        {
            // expected
        }
        service.dispose();

    }

    public void testPause() throws MuleException
    {
        assertFalse(service.isStarted());
        assertFalse(service.isPaused());

        service.initialise();

        // Pausing a service that is not started does not throw an exception
        assertFalse(service.isStarted());
        assertFalse(service.isPaused());
        service.resume();
        assertFalse(service.isPaused());
        service.start();
        assertTrue(service.isStarted());
        assertFalse(service.isPaused());
        service.pause();
        assertTrue(service.isPaused());
        service.pause();
        assertTrue(service.isPaused());
        service.dispose();

    }

    public void testResume() throws MuleException
    {
        assertFalse(service.isStarted());
        assertFalse(service.isPaused());

        service.initialise();

        assertFalse(service.isStarted());
        assertFalse(service.isPaused());
        service.resume();
        assertFalse(service.isPaused());
        service.start();
        assertTrue(service.isStarted());
        assertFalse(service.isPaused());
        service.resume();
        assertFalse(service.isPaused());
        service.pause();
        assertTrue(service.isPaused());
        service.resume();
        assertFalse(service.isPaused());
        service.resume();
        assertFalse(service.isPaused());
        service.dispose();

    }

    public void testStop() throws MuleException
    {
        assertFalse(service.isStarted());
        assertFalse(service.isPaused());
        service.stop();

        try
        {
            service.resume();
            fail("Exception expected: Cannot stop an uninitialised service");
        }
        catch (MuleException e)
        {
            // expected
        }

        service.initialise();
        assertFalse(service.isStarted());

        service.stop();
        assertFalse(service.isStarted());
        service.start();
        assertTrue(service.isStarted());
        service.stop();
        assertFalse(service.isStarted());
        service.stop();
        assertFalse(service.isStarted());
        service.dispose();

    }

    public void testDispose() throws MuleException
    {
        assertFalse(service.isStarted());
        assertFalse(service.isPaused());
        service.dispose();

        service.initialise();
        assertFalse(service.isStarted());

        service.dispose();
        assertFalse(service.isStarted());
        service.dispose();
    }

}
