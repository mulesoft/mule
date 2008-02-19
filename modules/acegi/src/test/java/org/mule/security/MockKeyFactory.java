/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.util.object.ObjectFactory;


/**
 * Empty mock for tests
 */
public class MockKeyFactory extends Named implements ObjectFactory
{
    public Object getOrCreate() throws Exception
    {
        return null;
    }

    public Object lookup(String id) throws Exception
    {
        return null;
    }

    public void release(Object object) throws Exception
    {
        // nothing to do
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        return LifecycleTransitionResult.OK;
    }
    
    public void dispose()
    {
        // nothing to do
    }

    public Class getObjectClass() throws Exception
    {
        return null;
    }
}
