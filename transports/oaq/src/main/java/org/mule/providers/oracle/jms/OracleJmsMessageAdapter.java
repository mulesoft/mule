/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import org.mule.providers.jms.JmsMessageAdapter;
import org.mule.umo.MessagingException;

import oracle.jms.AdtMessage;
import oracle.xdb.XMLType;

/**
 * If the message payload is XML, returns the XML as a string. If the message payload
 * is an ADT, simply returns {@code Object.toString()} in order to avoid a null
 * pointer exception. Any other message is handled by the standard
 * {@code JmsMessageAdapter}.
 */
public class OracleJmsMessageAdapter extends JmsMessageAdapter
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = -5304537031626649816L;

    public OracleJmsMessageAdapter(Object message) throws MessagingException
    {
        super(message);
    }

    /**
     * If the message payload is XML, returns the XML as an array of bytes. If the
     * message payload is an ADT, simply returns {@code Object.toString().getBytes()}
     * in order to avoid a null pointer exception. Any other message is handled by
     * the standard {@code JmsMessageAdapter}
     * 
     * @see JmsMessageAdapter#getPayloadAsBytes
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        Object jmsMessage = getPayload();
        if (jmsMessage instanceof AdtMessage)
        {
            Object adtMessage = ((AdtMessage)jmsMessage).getAdtPayload();
            if (adtMessage instanceof XMLType)
            {
                return ((XMLType)adtMessage).getBytesValue();
            }
            else
            {
                return adtMessage.toString().getBytes(getEncoding());
            }
        }
        else
        {
            return super.getPayloadAsBytes();
        }
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        Object jmsMessage = getPayload();
        if (jmsMessage instanceof AdtMessage)
        {
            Object adtMessage = ((AdtMessage)jmsMessage).getAdtPayload();
            if (adtMessage instanceof XMLType)
            {
                return ((XMLType)adtMessage).getStringVal();
            }
            else
            {
                return adtMessage.toString();
            }
        }
        else
        {
            return super.getPayloadAsString(encoding);
        }
    }

}
