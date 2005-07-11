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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.provider.UMOConnectable;

/**
 * Attempts to make a connection once and fails if there is an exception
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SingleAttemptConnectionStrategy extends AbstractConnectionStrategy
{
    public void doConnect(UMOConnectable connectable) throws FatalConnectException
    {
        try {
        	connectable.connect();
        } catch (Exception e) {
            throw new FatalConnectException(new Message(Messages.RECONNECT_STRATEGY_X_FAILED_ENDPOINT_X,
                                                        getClass().getName(),
                                                        getDescription(connectable)), e, connectable);

        }
    }
}
