/* 
* $Id$
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
package org.mule.providers.xmpp.transformers;

import java.util.Iterator;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XMPPError;
import org.mule.providers.xmpp.XmppConnector;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.transformer.TransformerException;

/**
 * Creates an Xmpp message packet from a UMOMessage
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ObjectToXmppPacket extends AbstractEventAwareTransformer {

    /**
     * Serial version
     */
    private static final long serialVersionUID = -815224066829254844L;

    public ObjectToXmppPacket() {
        registerSourceType(String.class);
        setReturnClass(Message.class);
    }

    public Object transform(Object src, String encoding, UMOEventContext context)
            throws TransformerException
    {
        Message result = null;
        try {
            result = new Message(context.getMessageAsString(encoding));
        }
        catch (UMOException e) {
            throw new TransformerException(this, e);
        }

        UMOMessage msg = context.getMessage();
        if (msg.getExceptionPayload() != null) {
            result.setError(new XMPPError(503, context.getMessage().getExceptionPayload().getMessage()));
        }

        for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();) {
            String name = (String)iterator.next();
            if (name.equals(XmppConnector.XMPP_THREAD)) {
                result.setThread((String)msg.getProperty(name));
            }
            else if (name.equals(XmppConnector.XMPP_SUBJECT)) {
                result.setSubject((String)msg.getProperty(name));
            }
            else if (name.equals(XmppConnector.XMPP_FROM)) {
                result.setFrom((String)msg.getProperty(name));
            }
            else if (name.equals(XmppConnector.XMPP_TO)) {
                result.setTo((String)msg.getProperty(name));
            }
            else {
                result.setProperty(name, msg.getProperty(name));
            }
        }
        return result;
    }

}
