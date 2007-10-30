/*
 * $Id: CxfMessageAdapter.java 6306 2007-05-04 03:02:55Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import org.mule.MuleException;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.cxf.i18n.CxfMessages;
import org.mule.providers.soap.MuleSoapHeaders;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.transformer.UMOTransformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.activation.DataHandler;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.AbstractWrappedMessage;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;

/**
 *
 */
public class CxfMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1L;

    private final Message payload;

    private UMOTransformer trans = new SerializableToByteArray();

    // Kill, kill, kill...
    private static final String MULE_SOAP_HEADER_URI = MuleSoapHeaders.MULE_10_ACTOR;
    private static final String MULE_SOAP_HEADER_LOCAL_NAME = MuleSoapHeaders.MULE_HEADER;

    public CxfMessageAdapter(Message message) throws MuleException
    {
        if (message == null)
        {
            throw new MuleException(CxfMessages.unableToConstructAdapterForNullMessage());
        }
        this.payload = message;
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
        return new String(getPayloadAsBytes(), encoding);
    }

    /**
     * Converts the payload implementation into a String representation
     * 
     * @return String representation of the payload
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {

        return (byte[]) trans.transform(payload);
    }

    /**
     * @return the current payload
     */
    public Object getPayload()
    {
        List<Object> objs = CastUtils.cast(payload.getContent(List.class));

        if (objs == null)
        {
            // Seems Providers get objects stored this way
            Object o = payload.getContent(Object.class);
            if (o != null)
            {
                return o;
            }
            else
            {
                return new Object[0];
            }
        }

        return objs.toArray();
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        Collection<Attachment> attachments = getAttachments();
        AttachmentImpl newA = new AttachmentImpl(name);
        newA.setDataHandler(dataHandler);
        attachments.add(newA);
    }

    public void removeAttachment(String name) throws Exception
    {
        Collection<Attachment> attachments = getAttachments();
        List<Attachment> newAttachments = new ArrayList<Attachment>();
        for (Attachment attachment : attachments)
        {
            // @TODO: Get some clarify around expected contract, e.g., is <null> an
            // ID? Ever?
            if (attachment.getId() != null && attachment.getId().equals(name))
            {
                continue;
            }
            newAttachments.add(attachment);
        }
        payload.setAttachments(newAttachments);
    }

    private Collection<Attachment> getAttachments() throws MuleException
    {
        if (payload instanceof AbstractWrappedMessage)
        {
            AbstractWrappedMessage soap = (AbstractWrappedMessage) payload;
            return soap.getAttachments();
        }
        else
        {
            // @TODO: Maybe pass the connector down and use connector exception
            // instead?
            throw new MuleException(CxfMessages.inappropriateMessageTypeForAttachments(payload.getClass()
                .getName()));
        }
    }
}
