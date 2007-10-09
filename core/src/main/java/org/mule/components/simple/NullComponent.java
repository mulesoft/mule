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
 * <code>NullComponent</code> is a component that is used as a placeholder. This
 * implementation will throw an exception if a message is received for it.
 */
public class NullComponent implements Callable
{

    public Object onCall(UMOEventContext context) throws Exception
    {
        throw new UnsupportedOperationException("This component cannot receive messages. Component is: "
                                                + context.getComponent().getName());
    }

}
