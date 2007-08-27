/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ThreadSafeAccess;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.umo.MessagingException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Part;

/**
 * <code>SimpleMailMessageAdapter</code> is an adapter for mail messages.  
 * Unlike {@link MailMessageAdapter} this preserves the message intact in its original 
 * form.
 *
 * <p>Header values are stored in two formats.  First, as historically used by
 * {@link MailMessageAdapter}, a single String value is stored for each distinct
 * header name (if a header is repeated only one value is stored).
 * Secondly, a list of values for each distinct header is stored in a property name
 * prefixed by HEADER_LIST_PREFIX
 * (which produces an invalid header name according to RFC 822 and so (i) avoids
 * conflict with the first property type and (ii) will cause current applications
 * that wrongly assume all properties are simple header values to fail fast).
 * The utility methods
 * {@link #isListHeader(String)}, {@link #toHeader(String)} and
 * {@link #toListHeader(String)} identify and convert between property and
 * header names as required.
 */
public class SimpleMailMessageAdapter extends AbstractMessageAdapter
{

    private static final long serialVersionUID = 8002607243523460556L;
    public static final String HEADER_LIST_PREFIX = "List:";
    private Part message;
    private byte[] cache = null;

    public SimpleMailMessageAdapter(Object object) throws MessagingException 
    {
        Message message = assertMessageType(object);

        try 
        {
            setMessageDetails(message);
            handleMessage(message);
        } 
        catch (Exception e) 
        {
            throw new MessagingException(CoreMessages.failedToCreate("Message Adapter"), e);
        }
    }

    protected SimpleMailMessageAdapter(SimpleMailMessageAdapter template)
    {
        super(template);
        message = template.message;
        cache = template.cache;
    }

    /**
     * By default, this simply stores the entire message as a single message.
     * Sub-classes may override with more complex processing.
     */
    protected void handleMessage(Message message) throws Exception 
    {
        setMessage(message);
    }

    protected void setMessage(Part message) 
    {
        this.message = message;
    }

    public Object getPayload() {
        return message;
    }

    public byte[] getPayloadAsBytes() throws Exception 
    {
        return buildCache(getEncoding());
    }

    public String getPayloadAsString(String encoding) throws Exception
    {
        // TODO - i don't understand how encoding is used here
        // could this method be called with various encodings?
        // does that invalidate the cache?
        // (ie there are two encodings -- one used to generate the cache from
        // the mail message, and one used to generate the string from the cache)
        return new String(buildCache(encoding), encoding);
    }

    private byte[] buildCache(String encoding) throws Exception
    {
        if (null == cache)
        {
            if (message.getContentType().startsWith("text/"))
            {
                cache = textPayload(encoding);
            }
            else
            {
                cache = binaryPayload();
            }
        }
        return cache;
    }

    private static Message assertMessageType(Object message) throws MessageTypeNotSupportedException
    {
        if (message instanceof Message)
        {
            return (Message)message;
        }
        else
        {
            throw new MessageTypeNotSupportedException(message, MailMessageAdapter.class);
        }
    }

    private void setMessageDetails(Message message) throws javax.mail.MessagingException 
    {
        setProperty(MailProperties.TO_ADDRESSES_PROPERTY,
            MailUtils.mailAddressesToString(message.getRecipients(Message.RecipientType.TO)));
        setProperty(MailProperties.CC_ADDRESSES_PROPERTY,
            MailUtils.mailAddressesToString(message.getRecipients(Message.RecipientType.CC)));
        setProperty(MailProperties.BCC_ADDRESSES_PROPERTY,
            MailUtils.mailAddressesToString(message.getRecipients(Message.RecipientType.BCC)));
        setProperty(MailProperties.REPLY_TO_ADDRESSES_PROPERTY,
            MailUtils.mailAddressesToString(message.getReplyTo()));
        setProperty(MailProperties.FROM_ADDRESS_PROPERTY, 
            MailUtils.mailAddressesToString(message.getFrom()));
        setProperty(MailProperties.SUBJECT_PROPERTY, 
            StringUtils.defaultIfEmpty(message.getSubject(), "(no subject)"));
        setProperty(MailProperties.CONTENT_TYPE_PROPERTY, 
            StringUtils.defaultIfEmpty(message.getContentType(), "text/plain"));

        Date sentDate = message.getSentDate();
        if (sentDate == null)
        {
            sentDate = new Date();
        }
        setProperty(MailProperties.SENT_DATE_PROPERTY, sentDate);

        for (Enumeration e = message.getAllHeaders(); e.hasMoreElements();)
        {
            Header header = (Header)e.nextElement();
            String name = header.getName();
            String listName = toListHeader(name);
            String value = header.getValue();

            if (null == getProperty(name))
            {
                setProperty(name, value);
            }

            if (null == getProperty(listName))
            {
                setProperty(listName, new LinkedList());
            }
            if (getProperty(listName) instanceof List)
            {
                ((List) getProperty(listName)).add(header.getValue());
            }
       }
    }

    /**
     * Check whether a property name has the format associated with a list
     * of header values
     * @param name A property name
     * @return true if the name is associated with a list of header values
     * (more exactly, if it starts with HEADER_LIST_PREFIX, which gives an
     * invalid header name according to RFC822).
     */
    public static boolean isListHeader(String name)
    {
        return null != name && name.startsWith(HEADER_LIST_PREFIX);
    }

    /**
     * Convert a property name associated with a list of header values to
     * the relevant header name (ie drop the prefix)
     * @param name A property name
     * @return The associated header name (ie with HEADER_LIST_PREFIX removed)
     */
    public static String toHeader(String name)
    {
        if (isListHeader(name))
        {
            return name.substring(HEADER_LIST_PREFIX.length());
        }
        else
        {
            return name;
        }
    }

    /**
     * Convert a header name to the property name associated with a list of
     * header values (ie prepend the prefix)
     * @param header A header name
     * @return The associated list property name (ie with HEADER_LIST_PREFIX prepended)
     */
    public static String toListHeader(String header)
    {
        if (isListHeader(header))
        {
            return header;
        }
        else
        {
            return HEADER_LIST_PREFIX + header;
        }
    }

    private static InputStream addBuffer(InputStream stream)
    {
        if (!(stream instanceof BufferedInputStream))
        {
            stream = new BufferedInputStream(stream);
        }
        return stream;
    }

    private byte[] binaryPayload() throws Exception 
    {
        InputStream stream = addBuffer(message.getInputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream(32768);
        IOUtils.copy(stream, baos);
        return baos.toByteArray();
    }

    private byte[] textPayload(String encoding) throws Exception 
    {
        InputStream stream = addBuffer(message.getInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuffer buffer = new StringBuffer(32768);

        String line;
        while ((line = reader.readLine()) != null)
        {
            buffer.append(line).append(SystemUtils.LINE_SEPARATOR);
        }

        return buffer.toString().getBytes(encoding);
    }

    public ThreadSafeAccess newThreadCopy()
    {
        return new SimpleMailMessageAdapter(this);
    }
    
}
