/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.lifecycle;

/**
 * <code>UMOEventListener</code> is a marker interface that is implemented by
 * objects wishing to receive UMO events in managed environments, succh as an EJB
 * container. There are not methods on this interface as typically Mule will work out
 * the method to invoke on the listener. Developers can implement
 * <code>org.mule.umo.lifecycle.Callable</code> to implement a specific Mule
 * listener interface.
 * 
 * @see Callable
 */
public interface UMOEventListener
{
    // no methods
}
