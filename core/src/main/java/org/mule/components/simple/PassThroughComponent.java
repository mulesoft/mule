/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.simple;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

/**
 * <code>PassThroughComponent</code> will simply return the payload back as the result.
 * Normally {@link BridgeComponent} should be used since it removes the slight overhead of
 * invoking the service object by never actually causing an invocation; however,
 * interceptors will only be invoked using this class.
 */
public class PassThroughComponent implements Callable
{

    public Object onCall(UMOEventContext context) throws Exception
    {
        return context.getTransformedMessage();
    }

}
