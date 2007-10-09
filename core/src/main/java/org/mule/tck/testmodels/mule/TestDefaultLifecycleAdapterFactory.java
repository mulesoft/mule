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

import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.UMOLifecycleAdapter;
import org.mule.umo.lifecycle.UMOLifecycleAdapterFactory;
import org.mule.umo.model.UMOEntryPointResolverSet;


public class TestDefaultLifecycleAdapterFactory implements UMOLifecycleAdapterFactory
{
    public TestDefaultLifecycleAdapterFactory()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.UMOLifecycleAdapterFactory#create(java.lang.Object,
     *      org.mule.umo.UMODescriptor, org.mule.umo.model.UMOEntryPointResolver)
     */
    public UMOLifecycleAdapter create(Object pojoService,
                                      UMOComponent component,
                                      UMOEntryPointResolverSet resolver) throws UMOException
    {
        return new TestDefaultLifecycleAdapter(pojoService, component, resolver);
    }

}
