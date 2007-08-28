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
import org.mule.impl.model.seda.SedaModel;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;

/**
 * Exposes some internals of the SedaComponent useful for unit testing.
 */
public class TestSedaModel extends SedaModel
{
    public String getType()
    {
        return "seda-test";
    }
    
    protected UMOComponent createComponent(UMODescriptor descriptor)
    {
        return new TestSedaComponent((MuleDescriptor) descriptor, this);
    }
}
