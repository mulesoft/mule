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
package org.mule.providers.xmpp.transformers;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.XMPPError;
import org.mule.providers.xmpp.XmppConnector;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.transformer.TransformerException;

import java.util.Iterator;

/**
 * Creates an Xmpp message packet from a UMOMessage
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ObjectToXmppPacket extends AbstractEventAwareTransformer {

    public ObjectToXmppPacket() {
        registerSourceType(String.class);
        setReturnClass(Message.class);
    }

    public Object transform(Object src, UMOEventContext context) throws TransformerException {
        Message result = null;
        try {
            result = new Message(context.getMessageAsString());
        } catch (UMOException e) {
            throw new TransformerException(this, e);
        }
        if (context.getMessage().getExceptionPayload() != null) {
            result.setError(new XMPPError(503, context.getMessage().getExceptionPayload().getMessage()));
        }
        for (Iterator iterator = context.getProperties().keySet().iterator(); iterator.hasNext();) {
            String name = (String) iterator.next();
            if (name.equals(XmppConnector.XMPP_THREAD)) {
                result.setThread((String) context.getProperty(name));
            } else if (name.equals(XmppConnector.XMPP_SUBJECT)) {
                result.setSubject((String) context.getProperty(name));
            } else if (name.equals(XmppConnector.XMPP_FROM)) {
                result.setFrom((String) context.getProperty(name));
            } else if (name.equals(XmppConnector.XMPP_TO)) {
                result.setTo((String) context.getProperty(name));
            } else {
                result.setProperty(name, context.getProperty(name));
            }
        }
        return result;
    }

}
