/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.attachments.Attachment;
import org.codehaus.xfire.attachments.Attachments;
import org.codehaus.xfire.attachments.SimpleAttachment;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mule.config.MuleProperties;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.soap.MuleSoapHeaders;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.transformer.UMOTransformer;

import javax.activation.DataHandler;
import java.util.Iterator;

/**
 * <code>XFireMessageAdapter</code> Wrapps an XFire MessageContext, reading
 * attahcments and Mule headers
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 419878758858206446L;

    private Object payload;
    private MessageContext messageContext;

    private UMOTransformer trans = new SerializableToByteArray();

    public XFireMessageAdapter(Object message)
    {
        this.payload = message;
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @param encoding
     *            The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte
     *            array
     * @return String representation of the message payload
     * @throws Exception
     *             Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        return new String(getPayloadAsBytes(), encoding);
    }

    /**
     * Converts the payload implementation into a String representation
     *
     * @return String representation of the payload
     * @throws Exception
     *             Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return (byte[])trans.transform(payload);
    }

    /**
     * @return the current payload
     */
    public Object getPayload()
    {
        return payload;
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        messageContext.getInMessage().getAttachments().addPart(new SimpleAttachment(name, dataHandler));
        super.addAttachment(name, dataHandler);
    }

    public void removeAttachment(String name) throws Exception
    {
        throw new UnsupportedOperationException("XFIRE: removeAttachment");
        // TODO unable to remove an attachment from XFire Attachments
    }

    public MessageContext getMessageContext()
    {
        return messageContext;
    }

    public void setMessageContext(MessageContext messageContext)
    {
        this.messageContext = messageContext;
        initHeaders();
        // TODO what is the expense of reading attachments??
        initAttachments();
    }

    protected void initHeaders()
    {
        if (messageContext.getInMessage() != null) {
            Element header = messageContext.getInMessage().getHeader();
            if (header == null) return;

            Namespace ns = Namespace.getNamespace(MuleSoapHeaders.MULE_NAMESPACE,
                    MuleSoapHeaders.MULE_10_ACTOR);
            Element muleHeaders = header.getChild(MuleSoapHeaders.MULE_HEADER, ns);
            if (muleHeaders != null) {
                Element child = muleHeaders.getChild(MuleProperties.MULE_CORRELATION_ID_PROPERTY, ns);
                if (child != null) {
                    setCorrelationId(child.getText());
                }
                child = muleHeaders.getChild(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, ns);
                if (child != null) {
                    setCorrelationGroupSize(Integer.valueOf(child.getText()).intValue());
                }
                child = muleHeaders.getChild(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, ns);
                if (child != null) {
                    setCorrelationSequence(Integer.valueOf(child.getText()).intValue());
                }
                child = muleHeaders.getChild(MuleProperties.MULE_REPLY_TO_PROPERTY, ns);
                if (child != null) {
                    setReplyTo(child.getText());
                }
            }
        }

    }

    protected void initAttachments()
    {
        try {
            Attachments atts = this.messageContext.getInMessage().getAttachments();
            if (atts != null) {
                for (Iterator i = atts.getParts(); i.hasNext();) {
                    Attachment att = ((Attachment)i.next());
                    super.addAttachment(att.getId(), att.getDataHandler());
                }
            }
        }
        catch (Exception e) {
            // this will not happen
            logger.fatal("Failed to read attachments", e);
        }
    }

}
