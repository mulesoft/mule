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
package org.mule.providers.gs;

import net.jini.core.entry.Entry;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.Utility;

/**
 * <code>JiniMessageAdapter</code> wraps a JavaSpaceMessage entry.
 *
 * @see Entry
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class JiniMessageAdapter extends AbstractMessageAdapter
{
	private Entry message;
	
    public JiniMessageAdapter(Object message) throws MessagingException
    {
        if(message==null) {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
        if(message instanceof Entry) {
            this.message = (Entry)message;
        } else {
    	    this.message = new JiniMessage(null, message);
        }
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding The encoding to use when transforming the message (if necessary). The parameter is
     *                 used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception {
        if(message instanceof JiniMessage) {
            return ((JiniMessage)message).getPayload().toString();
        } else {
            return message.toString();
        }
    }

    public byte[] getPayloadAsBytes() throws Exception
    {
        if(message instanceof JiniMessage) {
            return Utility.objectToByteArray(((JiniMessage)message).getPayload());
        } else {
            return Utility.objectToByteArray(message);
        }
    }

    public Object getPayload()
    {
        return message;
    }
}
