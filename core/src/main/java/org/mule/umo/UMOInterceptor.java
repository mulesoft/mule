/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

/**
 * <code>UMOInterceptor</code> is based on a similar concept of servlet filters and
 * works much the same way. This method is more commonally known as the interceptor
 * pattern and it allows for pre and processing of invocations on the object being
 * intercepted.
 */
public interface UMOInterceptor
{
    /**
     * Invoked by the previous interceptor in the chain
     * 
     * @param invocation the invocation containing info about the current message and
     *            component
     * @return A result message that may have been altered by this invocation
     * @throws UMOException if the invocation fails
     */
    UMOMessage intercept(Invocation invocation) throws UMOException;
}
