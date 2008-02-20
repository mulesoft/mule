/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.AbstractMuleTestCase;

public abstract class AbstractObjectFactoryTestCase extends AbstractMuleTestCase
{
    protected ObjectFactory factory;

    // @Override
    public void doSetUp()
    {
        factory = getObjectFactory();
    }

    public final void testInitialise() throws Exception
    {
        try
        {
            factory.getOrCreate();
            fail("expected InitialisationException");
        }
        catch (InitialisationException iex)
        {
            // OK
        }

        try
        {
            factory.initialise();
        }
        catch (InitialisationException iex)
        {
            fail(iex.getDetailedMessage());
        }

        assertNotNull(factory.getOrCreate());
    }

    // @Override
    public final void testDispose() throws Exception
    {
        factory.initialise();
        factory.dispose();

        try
        {
            factory.getOrCreate();
            fail("expected InitialisationException");
        }
        catch (InitialisationException iex)
        {
            // OK
        }

    }

    public abstract ObjectFactory getObjectFactory();

    public abstract void testGetObjectClass() throws Exception;

    public abstract void testGet() throws Exception;

}
