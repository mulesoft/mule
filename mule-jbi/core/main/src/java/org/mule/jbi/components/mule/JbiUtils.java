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

import org.mule.config.MuleProperties;
import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.jbi.components.AbstractComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;

import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Useful for converting message types
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JbiUtils {

    public static UMOMessage createMessage(NormalizedMessage message) {
        //todo source transformer
        Object source = message.getContent();
        Map properties = new HashMap();
        for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            properties.put(s, message.getProperty(s));
        }
        if (message.getSecuritySubject() != null) {
            properties.put(MuleProperties.MULE_USER_PROPERTY, message.getSecuritySubject());
        }
        return new MuleMessage(source, properties);
    }

    public static UMOEvent createEvent(NormalizedMessage message, AbstractComponent component) throws MessagingException {
        UMOMessage umoMessage = createMessage(message);
        UMOEndpoint endpoint = (UMOEndpoint)message.getProperty(MuleProperties.MULE_ENDPOINT_PROPERTY);
        if(endpoint == null) {
            throw new MessagingException("Endpoint property '" + MuleProperties.MULE_ENDPOINT_PROPERTY + "' not set on message");
        }
        return new MuleEvent(umoMessage, endpoint, new MuleSession(new NullUMOComponent(component.getName()), null), endpoint.isSynchronous());
    }

    public static void populateNormalizedMessage(UMOMessage muleMessage, NormalizedMessage message) throws MessagingException {
        //todo message.setContent();
        //todo securitySubject
        for (Iterator iterator = muleMessage.getPropertyNames(); iterator.hasNext();) {
            String s = (String)iterator.next();
            message.setProperty(s, muleMessage.getProperty(s));
        }
        for (Iterator iterator = muleMessage.getAttachmentNames().iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            message.addAttachment(s, muleMessage.getAttachment(s));
        }
    }
}
