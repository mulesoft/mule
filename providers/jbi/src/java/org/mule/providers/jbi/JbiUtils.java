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
package org.mule.providers.jbi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.umo.UMOMessage;

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
