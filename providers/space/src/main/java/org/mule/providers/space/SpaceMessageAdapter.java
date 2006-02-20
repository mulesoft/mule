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
package org.mule.providers.space;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.UUID;
import org.mule.util.Utility;

/**
 * Wraps a JavaSpaces Entry object
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SpaceMessageAdapter extends AbstractMessageAdapter {

    private String id;
    private Object message;

    /**
     * Creates a default message adapter with properties and attachments
     *
     * @param message the message to wrap. If this is null and NullPayload object will be used
     * @see org.mule.providers.NullPayload
     */
    public SpaceMessageAdapter(Object message) throws MessageTypeNotSupportedException {
        id = new UUID().getUUID();
        if (message == null) {
            throw new MessageTypeNotSupportedException(null, getClass());
        } else {
            this.message = message;
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
        return message.toString();
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception {
        return Utility.objectToByteArray(message);
    }

    /**
     * @return the current message
     */
    public Object getPayload() {
        return message;
    }

    public String getUniqueId() {
        return id;
    }
}
