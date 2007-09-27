/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.umo.UMOEventContext;
import org.mule.umo.model.UMOEntryPointResolver;
import org.mule.umo.model.UMOEntryPointResolverSet;

/**
 * <code>TestEntryPointResolver</code> is a test EntryPointResolver that doesn't provide
 * any functionality. It is usually used to test confguration options
 */
public class TestEntryPointResolverSet implements UMOEntryPointResolverSet
{

    /**
     * Will add a resolver to the list of resolvers to invoke on a compoent.
     * Implementations must maintain an ordered list of resolvers
     *
     * @param resolver the resolver to add
     */
    public void addEntryPointResolver(UMOEntryPointResolver resolver)
    {

    }

    public Object invoke(Object component, UMOEventContext context) throws Exception
    {
        return null;
    }

    /**
     * Removes a resolver from the list
     *
     * @param resolver the resolver to remove
     * @return true if the resolver was found and removed from the list
     */
    public boolean removeEntryPointResolver(UMOEntryPointResolver resolver)
    {
        return false;
    }
}
