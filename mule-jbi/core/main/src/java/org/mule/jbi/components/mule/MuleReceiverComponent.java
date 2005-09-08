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
package org.mule.jbi.components.mule;

import org.mule.config.i18n.Message;
import org.mule.impl.MuleMessage;
import org.mule.jbi.messaging.NoMessageException;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.InternalMessageListener;
import org.mule.providers.jbi.JbiMessageAdapter;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.provider.UMOMessageReceiver;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import java.io.OutputStream;

/**
 * Exposes a Mule receiver as a Jbi component allowing other Jbi components to receive
 * events on Mule endpoints
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleReceiverComponent extends AbstractEndpointComponent implements InternalMessageListener
{
    private AbstractMessageReceiver receiver;

    public AbstractMessageReceiver getReceiver() {
        return receiver;
    }

    public void setReceiver(AbstractMessageReceiver receiver) {
        this.receiver = receiver;
    }

    protected void doInit() throws Exception
    {
        super.doInit();
        UMOMessageReceiver receiver = endpoint.getConnector().registerListener(
                new NullUMOComponent(getName()), endpoint);
        if(receiver==null) {
            throw new NullPointerException(new Message("jbi", 1, getName()).toString());
        } else if(receiver instanceof AbstractMessageReceiver) {
            this.receiver = (AbstractMessageReceiver)receiver;
        } else {
            throw new IllegalArgumentException(new Message("jbi", 2, getName(),
                    AbstractMessageReceiver.class.getName()).toString());
        }

        this.receiver.setListener(this);
    }

    public UMOMessage onMessage(UMOMessage message, UMOTransaction trans, boolean synchronous, OutputStream outputStream) throws UMOException
    {
        MessageExchange me = null;
        try {
            if(synchronous) {
                //todo manage exchanges
                me = getExchangeFactory().createInOutExchange();
            } else {
                me = getExchangeFactory().createInOnlyExchange();
            }
            me.setService(targetService);
            ServiceEndpoint endpoint = null;
            ServiceEndpoint[] eps = context.getEndpointsForService(targetService);
            if(eps.length==0) {
                //container should handle this
                //throw new MessagingException("There are no endpoints registered for targetService: " + targetService);
            } else {
                endpoint = eps[0];
            }

            if (endpoint != null) {
                me.setEndpoint(endpoint);
            }

            NormalizedMessage nmessage = me.createMessage();
            JbiUtils.populateNormalizedMessage(message, nmessage);
            me.setMessage(nmessage, IN);
            if(synchronous) {
                //todo timeout
                getContext().getDeliveryChannel().sendSync(me);
                NormalizedMessage result = null;
                try {
                    result = getOutMessage(me);
                } catch (NoMessageException noMessageException) {
                    return null;
                }
                done(me);
                return new MuleMessage(new JbiMessageAdapter(result));
            } else {
                getContext().getDeliveryChannel().send(me);

                return null;
            }
        } catch (MessagingException e) {
            try {
                error(me, e);
                return null;
            } catch (MessagingException e1) {
                logger.error(e.getMessage(), e);
                return null;
            }
            //throw new org.mule.umo.MessagingException(new Message(Messages.FAILED_TO_INVOKE_X, getName()), message, e);
        }
    }
}