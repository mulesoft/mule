/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
 *
 * @deprecated Use {@code SHA256MuleEventKeyGenerator} instead.
 */
@Deprecated
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
