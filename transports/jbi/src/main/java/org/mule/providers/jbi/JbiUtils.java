/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jbi;

import org.mule.config.MuleProperties;
import org.mule.impl.MuleMessage;
import org.mule.umo.UMOMessage;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.output.ByteArrayOutputStream;

/**
 * Useful helpers for converting message types
 */
public class JbiUtils
{

    public static UMOMessage createMessage(NormalizedMessage message) throws MessagingException
    {
        Map properties = new HashMap();
        for (Iterator iterator = message.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String s = (String)iterator.next();
            properties.put(s, message.getProperty(s));
        }
        if (message.getSecuritySubject() != null)
        {
            properties.put(MuleProperties.MULE_USER_PROPERTY, message.getSecuritySubject());
        }
        try
        {
            // TODO source transformer
            Source source = message.getContent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(baos);
            TransformerFactory.newInstance().newTransformer().transform(source, result);
            UMOMessage msg = new MuleMessage(baos.toByteArray(), properties);
            baos.close();
            return msg;
        }
        catch (Exception e)
        {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    public static void populateNormalizedMessage(UMOMessage muleMessage, NormalizedMessage message)
        throws MessagingException
    {
        try
        {
            message.setContent(new StreamSource(new ByteArrayInputStream(muleMessage.getPayloadAsBytes())));
        }
        catch (Exception e)
        {
            throw new MessagingException(e.getMessage(), e);
        }

        for (Iterator iterator = muleMessage.getPropertyNames().iterator(); iterator.hasNext();)
        {
            String s = (String)iterator.next();
            message.setProperty(s, muleMessage.getProperty(s));
        }

        for (Iterator iterator = muleMessage.getAttachmentNames().iterator(); iterator.hasNext();)
        {
            String s = (String)iterator.next();
            message.addAttachment(s, muleMessage.getAttachment(s));
        }
    }
}
