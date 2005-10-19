/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.rmi;


/**
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */

public interface RmiAble
{
    /**
     * Set Mule receiver as parameter for callback
     *
     * @param receiver
     */
    public void setReceiver(RmiMessageReceiver receiver);

    /**
     * Implementing method should route message back to Mule receiver
     * and receive possible reply for program that calls this Receiver
     *
     * @param message from calling program
     * @return possible reply from Mule to be routed back to calling program as method result
     */
    public Object route(Object message);
}
