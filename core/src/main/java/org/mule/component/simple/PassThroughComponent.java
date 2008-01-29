/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component.simple;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;

/**
 * <code>PassThroughComponent</code> will simply return the payload back as the result.
 * This typically you don't need to specify this, since it is used by default.
 */
public class PassThroughComponent implements Callable
{

    public Object onCall(MuleEventContext context) throws Exception
    {
        return context.transformMessage();
    }

}
