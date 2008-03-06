/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.interceptor;

import org.mule.api.MuleMessage;
import org.mule.api.interceptor.Invocation;
import org.mule.api.MuleException;


/**
 * <code>Interceptor</code> is based on a similar concept of servlet filters and
 * works much the same way. This method is more commonally known as the interceptor
 * pattern and it allows for pre and processing of invocations on the object being
 * intercepted.
 *
 * <p>In 2.x this is depreacted, but we provide an adapter, in the Spring Extras package, to
 * help use old implementations.  There is one significant change, however - because the
 * interception is now "lower" in the call chain, {@link org.mule.api.interceptor.Invocation#execute()}
 * returns an Object rather than a {@link org.mule.api.MuleMessage}.  To simplify handling this,
 * the adapter we provide will construct a suitable MuleMessage for you if you return null. 
 *
 * @deprecated - This is only used for backwards compatability with old style (Mule 1.x) interceptors
 */
public interface Interceptor
{

    /**
     * Invoked when the component should be called.  The implementation can call
     * {@link Invocation#execute()} to call the component.
     * 
     * @param invocation the invocation containing info about the current message and
     *            service
     * @return A result message that may have been altered by this invocation
     * @throws org.mule.api.MuleException if the invocation fails
     */
    MuleMessage intercept(Invocation invocation) throws MuleException;

}
