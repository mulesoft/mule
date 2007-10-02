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
import org.mule.impl.model.MuleProxy;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.impl.model.seda.SedaModel;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOModel;
import org.mule.util.object.ObjectPool;


/**
 * Exposes some internals of the SedaComponent useful for unit testing.
 */
public class TestSedaComponent extends SedaComponent
{
    public TestSedaComponent(MuleDescriptor descriptor, SedaModel model)
    {
        super(descriptor, model);
    }

    public MuleProxy createComponentProxy(Object component, MuleDescriptor descriptor, UMOModel model, ObjectPool proxyPool) 
        throws UMOException
    {
        return new TestMuleProxy(component, descriptor, model, null);
    }    
    
    // Change from protected to public
    public MuleProxy getProxy() throws UMOException
    {
        return super.getProxy();
    }
}
