/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import org.mule.api.MuleException;
import org.mule.tck.AbstractMuleTestCase;

public abstract class AbstractFlowConstuctTestCase extends AbstractMuleTestCase
{
    protected abstract AbstractFlowConstuct getFlowConstruct();

    public void testStart() throws MuleException
    {
        try
        {
            getFlowConstruct().start();
            fail("Exception expected: Cannot start an uninitialised service");
        }
        catch (Exception e)
        {
            // expected
        }

        getFlowConstruct().initialise();
        getFlowConstruct().start();

        try
        {
            getFlowConstruct().initialise();
            fail("Exception expected: Cannot initialise an already initialised service");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        getFlowConstruct().dispose();

    }

    public void testStop() throws MuleException
    {
        assertFalse(getFlowConstruct().isStarted());

        try
        {
            getFlowConstruct().stop();
            fail("Exception expected: Cannot stop an uninitialised service");
        }
        catch (IllegalStateException e)
        {
            // expected
        }

        getFlowConstruct().initialise();
        assertFalse(getFlowConstruct().isStarted());

        try
        {
            getFlowConstruct().stop();
            fail("Exception expected: Cannot stop a service that is not started");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        assertFalse(getFlowConstruct().isStarted());
        getFlowConstruct().start();
        assertTrue(getFlowConstruct().isStarted());
        getFlowConstruct().stop();
        assertFalse(getFlowConstruct().isStarted());
        try
        {
            getFlowConstruct().stop();
            fail("Exception expected: Cannot stop a service that is not started");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
        assertFalse(getFlowConstruct().isStarted());
        getFlowConstruct().dispose();

    }

    public void testDispose() throws MuleException
    {
        assertFalse(getFlowConstruct().isStarted());
        getFlowConstruct().dispose();

        try
        {
            getFlowConstruct().dispose();
            fail("Exception expected: Cannot dispose a service that is already disposed");
        }
        catch (IllegalStateException e)
        {
            // expected
        }

        try
        {
            getFlowConstruct().initialise();
            fail("Exception expected: Cannot invoke initialise (or any lifecycle) on an object once it is disposed");
        }
        catch (IllegalStateException e)
        {
            // expected
        }
    }

}
