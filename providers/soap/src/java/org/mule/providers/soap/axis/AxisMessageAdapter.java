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
package org.mule.providers.soap.axis;

import org.apache.axis.MessageContext;
import org.mule.config.i18n.Message;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.transformers.simple.SerialisableToByteArray;
import org.mule.umo.MessagingException;
import org.mule.umo.transformer.UMOTransformer;

import javax.xml.soap.SOAPException;

/**
 * <code>AxisMessageAdapter</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class AxisMessageAdapter extends AbstractMessageAdapter
{
    private Object message;
    private UMOTransformer trans = new SerialisableToByteArray();

    public AxisMessageAdapter(Object message) throws MessagingException
    {
        this.message = message;
        try
        {
            MessageContext ctx = MessageContext.getCurrentContext();
            if(ctx!=null)
            {
                MuleSoapHeaders header = new MuleSoapHeaders(ctx.getMessage().getSOAPPart().getEnvelope().getHeader());

                if(header.getReplyTo()!=null && !"".equals(header.getReplyTo())) {
                    setReplyTo(header.getReplyTo());
                }

                if(header.getCorrelationGroup()!=null && !"".equals(header.getCorrelationGroup())
                        && !"-1".equals(header.getCorrelationGroup())) {
                    setCorrelationGroupSize(Integer.parseInt(header.getCorrelationGroup()));
                }
                if(header.getCorrelationSequence()!=null && !"".equals(header.getCorrelationSequence())
                        && !"-1".equals(header.getCorrelationSequence())) {
                    setCorrelationSequence(Integer.parseInt(header.getCorrelationSequence()));
                }
                if(header.getCorrelationId()!=null && !"".equals(header.getCorrelationId())) {
                    setCorrelationId(header.getCorrelationId());
                }
            }
        } catch (SOAPException e)
        {
            throw new MessagingException(new Message("soap", 5), message, e);
        }
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString() throws Exception
    {
        return new String(getPayloadAsBytes());
    }

    /**
     * Converts the message implementation into a String representation
     *
     * @return String representation of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return (byte[])trans.transform(message);
    }

    /**
     * @return the current message
     */
    public Object getPayload()
    {
        return message;
    }
}