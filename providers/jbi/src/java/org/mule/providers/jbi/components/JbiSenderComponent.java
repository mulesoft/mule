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
package org.mule.providers.jbi.components;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleSession;
import org.mule.jbi.components.AbstractComponent;
import org.mule.jbi.components.MessageListener;
import org.mule.providers.jbi.JbiUtils;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageDispatcher;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JbiSenderComponent extends AbstractComponent implements MessageListener {
    private UMOEndpoint endpoint;

    public UMOEndpoint getReceiver() {
        return endpoint;
    }

    public void setReceiver(UMOEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    protected void doInit() throws Exception {
        if (endpoint == null) {
            throw new NullPointerException("A Mule endpoint must be set on this component");
        }

    }

    public void onMessage(MessageExchange messageExchange) throws MessagingException {
        try {
            UMOMessageDispatcher dispatcher = endpoint.getConnector().getDispatcher(endpoint.getEndpointURI().getAddress());
            NormalizedMessage out = getOutMessage(messageExchange);
            UMOMessage message = JbiUtils.createMessage(out);
            UMOEvent event = new MuleEvent(message, endpoint, new MuleSession(), endpoint.isSynchronous());
            if (endpoint.isSynchronous()) {
                UMOMessage result = dispatcher.send(event);
                //todo send result back
            } else {
                dispatcher.dispatch(event);
            }

        } catch (Exception e) {
            error(messageExchange, e);
        }

    }
}
