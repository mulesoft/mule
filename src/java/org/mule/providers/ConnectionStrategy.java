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
 *
 */
package org.mule.providers;

/**
 * Allows developers to plug in customised reconnection behaviour
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface ConnectionStrategy {
    /**
     * Attempts to connect to a resource according the strategy implemented
     * @param receiver the receiver to connect to a resource
     * @throws FatalConnectException is thrown if the strategy finally fails to make a
     * connection.
     */
    public void connect(AbstractMessageReceiver receiver) throws FatalConnectException;
}
