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
package org.mule.providers.vm;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UniqueIdNotSupportedException;

/**
 * <code>VMMessageAdapter</code> provides a common abstraction of Mule Event
 * message. The message adapter allows a Mule event to be read and manipulated
 * like any other object data type from any external system that has a Mule
 * endpoint implementation.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class VMMessageAdapter extends AbstractMessageAdapter
{
    /**
     * The message itself in this case an UMOEvent
     */
    private UMOMessage message = null;

    public VMMessageAdapter(UMOMessage message) throws MessageTypeNotSupportedException
    {
        setMessage(message);
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
        return message.getPayloadAsString(encoding);
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return convertToBytes(message.getPayload());
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return message;
    }

    /**
     * @param message new value for the message
     */
    private void setMessage(UMOMessage message) throws MessageTypeNotSupportedException
    {
        if (message == null) {
            throw new MessageTypeNotSupportedException(null, getClass());
        }
        this.message = message;
    }

    public String getUniqueId() throws UniqueIdNotSupportedException
    {
        return message.getUniqueId();
    }

}
