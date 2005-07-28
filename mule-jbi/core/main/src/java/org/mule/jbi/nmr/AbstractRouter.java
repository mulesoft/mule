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
package org.mule.jbi.nmr;

import org.mule.jbi.messaging.DeliveryChannelImpl;
import org.mule.jbi.messaging.MessageExchangeProxy;
import org.mule.jbi.messaging.UnknownRoleException;
import org.mule.jbi.registry.Registry;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

/**
 * A useful base class for custom internal router implementations 
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractRouter implements InternalRouter
{
    private Registry registry;

    protected AbstractRouter(Registry registry) {
        this.registry = registry;
    }

    public Registry getRegistry() {
        return registry;
    }

    public void setRegistry(Registry registry) {
        this.registry = registry;
    }

    public void routeExchange(MessageExchange me) throws MessagingException {
        String target = null;

        if (me.getRole() == MessageExchange.Role.CONSUMER) {
            target = ((MessageExchangeProxy) me).getProvider();
        } else if (me.getRole() == MessageExchange.Role.PROVIDER) {
            target = ((MessageExchangeProxy) me).getConsumer();
        } else {
            throw new UnknownRoleException(me.getRole());
        }
        DeliveryChannelImpl ch = (DeliveryChannelImpl) registry.getComponent(target).getChannel();
		ch.enqueue(((MessageExchangeProxy) me).getTwin());
    }
}
