/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.keygenerator;

import org.mule.api.MuleEventKeyGenerator;
import org.mule.api.MuleEvent;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.security.MessageDigest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implements {@link org.mule.api.MuleEventKeyGenerator} applying an MD5 digest to the event's
 * message payload.
 */
public class MD5MuleEventKeyGenerator implements MuleEventKeyGenerator
{

    protected Log logger = LogFactory.getLog(getClass());

    public Serializable generateKey(MuleEvent event) throws NotSerializableException
    {
        try
        {
            byte[] bytesOfMessage = event.getMessageAsBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            String key = new String(md.digest(bytesOfMessage));

            if (logger.isDebugEnabled())
            {
                logger.debug("Generated key for event: " + event + " key: " + key);
            }

            return key;
        }
        catch (Exception e)
        {
            NotSerializableException notSerializableException = new NotSerializableException(e.getMessage());
            notSerializableException.initCause(e);

            throw notSerializableException;
        }
    }
}
