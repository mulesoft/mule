/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.keygenerator;

import org.mule.api.MuleEvent;
import org.mule.api.MuleEventKeyGenerator;
import org.mule.util.StringUtils;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.security.MessageDigest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link org.mule.api.MuleEventKeyGenerator} applying SHA-256 digest to the event's
 * message payload.
 */
public class SHA256MuleEventKeyGenerator implements MuleEventKeyGenerator
{

    private static final Logger logger = LoggerFactory.getLogger(SHA256MuleEventKeyGenerator.class);

    public Serializable generateKey(MuleEvent event) throws NotSerializableException
    {
        try
        {
            byte[] bytesOfMessage = event.getMessageAsBytes();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String key = StringUtils.toHexString(md.digest(bytesOfMessage));

            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Generated key for event: %s key: %s", event, key));
            }

            return key;
        }
        catch (Exception e)
        {
            // TODO: The exception may not necessarily be caused by a serialization problem, but we still throw
            // NotSerializableException to keep backwards compatibility. The interface needs to be changed.

            NotSerializableException notSerializableException = new NotSerializableException(e.getMessage());
            notSerializableException.initCause(e);

            throw notSerializableException;
        }
    }
}
