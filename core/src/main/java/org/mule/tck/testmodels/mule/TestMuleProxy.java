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

import org.mule.api.MuleException;
import org.mule.api.MuleContext;
import org.mule.api.component.Component;
import org.mule.component.DefaultMuleProxy;

/**
 * Makes the underlying POJO service object available for unit testing.
 */
public class TestMuleProxy extends DefaultMuleProxy
{
    private Object pojoService;
    
    public TestMuleProxy(Object pojoService, Component component, MuleContext muleContext)
        throws MuleException
    {
        super(pojoService, component, muleContext);
        this.pojoService = pojoService;
    }

    /** Returns the underlying POJO service object for unit testing. */
    public Object getPojoService() throws Exception
    {
        return pojoService;
    }
}


