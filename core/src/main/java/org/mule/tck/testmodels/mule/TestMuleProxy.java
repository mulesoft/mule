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

import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.DefaultMuleProxy;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOModel;
import org.mule.util.object.ObjectPool;

/**
 * Makes the underlying component object available for unit testing.
 */
public class TestMuleProxy extends DefaultMuleProxy
{
    private Object component;
    
    public TestMuleProxy(Object component, MuleDescriptor descriptor, UMOModel model, ObjectPool proxyPool)
        throws UMOException
    {
        super(component, descriptor, model, proxyPool);
        this.component = component;
    }

    public Object getComponent()
    {
        return component;
    }
}


