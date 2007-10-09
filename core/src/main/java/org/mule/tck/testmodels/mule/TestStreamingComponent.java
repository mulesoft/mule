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

import org.mule.impl.model.streaming.StreamingComponent;
import org.mule.umo.UMOException;

/**
 * Exposes some internals of the StreamingComponent useful for unit testing.
 */
public class TestStreamingComponent extends StreamingComponent
{
    public TestStreamingComponent()
    {
        super();
    }

    //@Override
    public Object getOrCreateService() throws UMOException
    {
        return super.getOrCreateService();
    }
}
