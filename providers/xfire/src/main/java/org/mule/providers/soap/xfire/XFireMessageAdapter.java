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
package org.mule.providers.soap.xfire;

import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.attachments.Attachment;
import org.codehaus.xfire.attachments.SimpleAttachment;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.soap.MuleSoapHeaders;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.transformer.UMOTransformer;

import javax.activation.DataHandler;
import java.util.Iterator;

/**
 * <code>XFireMessageAdapter</code> Wrapps an XFire MessageContext, reading attahcments and Mule headers
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class XFireMessageAdapter extends AbstractMessageAdapter {
    private Object payload;
    private MessageContext messageContext;

    private UMOTransformer trans = new SerializableToByteArray();

    public XFireMessageAdapter(Object message) {
        this.payload = message;
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
        return new String(getPayloadAsBytes(), encoding);        
    }

    /**
     * Converts the payload implementation into a String representation
     *
     * @return String representation of the payload
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception {
        return (byte[]) trans.transform(payload);
    }

    /**
     * @return the current payload
     */
    public Object getPayload() {
        return payload;
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception {
        messageContext.getInMessage().getAttachments().addPart(new SimpleAttachment(name, dataHandler));
        super.addAttachment(name, dataHandler);
    }

    public void removeAttachment(String name) throws Exception {
        throw new UnsupportedOperationException("XFIRE: removeAttahcment");
             //Todo unable to remove an attahcment from XFire Attachements
    }

    public MessageContext getMessageContext() {
        return messageContext;
    }

    public void setMessageContext(MessageContext messageContext) {
        this.messageContext = messageContext;
        initHeaders();
        //Todo what is the expense of reading attachments??
        // initAttachments();
    }

    protected void initHeaders() {
        if(messageContext.getInMessage().hasHeader()) {
            MuleSoapHeaders header = new MuleSoapHeaders(messageContext.getInMessage().getHeader().getChildren().iterator());

            if (header.getReplyTo() != null && !"".equals(header.getReplyTo())) {
                setReplyTo(header.getReplyTo());
            }

            if (header.getCorrelationGroup() != null && !"".equals(header.getCorrelationGroup())
                    && !"-1".equals(header.getCorrelationGroup())) {
                setCorrelationGroupSize(Integer.parseInt(header.getCorrelationGroup()));
            }
            if (header.getCorrelationSequence() != null && !"".equals(header.getCorrelationSequence())
                    && !"-1".equals(header.getCorrelationSequence())) {
                setCorrelationSequence(Integer.parseInt(header.getCorrelationSequence()));
            }
            if (header.getCorrelationId() != null && !"".equals(header.getCorrelationId())) {
                setCorrelationId(header.getCorrelationId());
            }
        }
    }

     protected void initAttachments() {
        try {
            Attachment att = null;
            if(this.messageContext.getInMessage().getAttachments()!=null) {
                for (Iterator i = this.messageContext.getInMessage().getAttachments().getParts(); i.hasNext();) {
                    att = ((Attachment) i.next());
                    super.addAttachment(att.getId(), att.getDataHandler());
                }
            }
        } catch (Exception e) {
            //this will not happen
            logger.fatal("Failed to read attachments", e);
        }
    }
}
