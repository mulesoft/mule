/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.umo;

import org.mule.umo.endpoint.UMOEndpoint;


/**
 * <code>UMOExceptionStrategy</code> is a strategy class used for customising the behaviour of
 * exception handling in different components in the system.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOExceptionStrategy
{
    /**
     * This is called when an exception occurs. By implementing this you can provide different stratgies
     * for handling exceptions
     *
     * @param message Can be anthing, but is usually The message being processed when
     *        the exception occurred.  The message could be an event and implmenting methods
     *        should expect that an UMOEvent maybe passed to this method from the
     *        framework.
     * @param t       The Throwable exception that occurred
     * @return java.lang.Throwable The user may wish to return any exception which could be thrown on
     *         depending on implementation
     */
    public Throwable handleException(Object message, Throwable t);

    UMOEndpoint getEndpoint();

    void setEndpoint(UMOEndpoint exceptionEndpoint);
}