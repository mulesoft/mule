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
 *
 */
package org.mule.providers.jms.transformers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.Messages;
import org.mule.impl.RequestContext;
import org.mule.providers.jms.JmsMessageUtils;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.PropertiesHelper;
import org.mule.util.compression.CompressionHelper;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.Session;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>AbstractJmsTransformer</code> is an abstract class the should be used for all transformers where a JMS message
 * <p/>
 * will be the transformed or transformee object.  It provides services for compressing  and uncompressing messages.
 *
 * @author Ross Mason
 * @version 1.2
 */

public abstract class AbstractJmsTransformer extends AbstractTransformer
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(AbstractJmsTransformer.class);

    private Session session = null;

    public AbstractJmsTransformer()
    {
        super();
    }

    /**
     * Transforms the object.
     *
     * @param src     The source object to transform.
     * @param session
     * @return The transformed object as an XMLMessage
     */
    public Object transform(Object src, Session session) throws TransformerException
    {
        if (session == null && this.session == null)
        {
            throw new TransformerException(new org.mule.config.i18n.Message("jms", 1), this);
        }
        if (session != null)
        {
            this.session = session;
        }
        if (src == null)
        {
            throw new TransformerException(new org.mule.config.i18n.Message(Messages.TRANSFORM_FAILED_FROM_X_TO_X, "null", "Object"), this);
        }
        Object ret = transform(src);
        if (logger.isDebugEnabled())
        {
            logger.debug("Transformed message from type: " + src.getClass().getName() + " to type: " + ret.getClass().getName());
        }
        return ret;
    }


    /**
     * @param src The source data to compress
     * @return
     * @throws TransformerException
     */
    protected Message transformToMessage(Object src) throws TransformerException
    {
        try
        {
            //The session can be closed by the dispatcher closing so its more
            //reliable to get it from the dispatcher each time
            if (session == null || getEndpoint()!=null)
            {
                session = (Session) getEndpoint().getConnector().getDispatcher("transformerSession").getDelegateSession();
                //throw new TransformerException("You must set the JMS AbstractMessageDispatcher on a AbstractJmsTransformer before using it");
            }
            
//            if (getDoCompression())
//            {
//                byte[] buffer = compressMessage(src);
//                if (logger.isDebugEnabled())
//                {
//                    logger.debug("Compressing message in transformation");
//                }
//                BytesMessage bMsg = session.createBytesMessage();
//                bMsg.clearBody();
//                bMsg.setBooleanProperty(JMS_PROPERTY_COMPRESSED, true);
//
//                //was getting strange results with the unit tests if I used
//                //bMsg.writeObject(buffer) or bMsg.writeBytes(buffer)  to do with an
//                //optimisation in the server I am testing with.
//                //Doing it byte by byte for now
//                for (int i = 0; i < buffer.length; i++)
//                {
//                    bMsg.writeByte(buffer[i]);
//                }
//                return bMsg;
//            } else {
                Message msg = null;
                if (src instanceof Message) {
                    msg = (Message)src;
                    msg.clearProperties();
                } else {
                    msg = JmsMessageUtils.getMessageForObject(src, session);
                }
                //set the event properties on the Message
                UMOEventContext ctx  = RequestContext.getEventContext();
                if(ctx==null) {
                    logger.warn("There is no current event context");
                    return msg;
                }

                Map props = ctx.getProperties();
                props = PropertiesHelper.getPropertiesWithoutPrefix(props, "JMS");
                Map.Entry entry;
                String key;
                for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
                {
                    entry =  (Map.Entry)iterator.next();
                    key = entry.getKey().toString();
                    if(MuleProperties.MULE_CORRELATION_ID_PROPERTY.equals(key)) {
                        msg.setJMSCorrelationID(entry.getValue().toString());
                    }
                    msg.setObjectProperty(key, entry.getValue());
                }

                return msg;
         //   }
        } catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }


    protected Object transformFromMessage(Message source) throws TransformerException
    {
        Object result = null;
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Message type received is: " + source.getClass().getName());
            }
            if (source instanceof BytesMessage)
            {
                //If this bytes Message is not compressed it will throw a NotGZipFormatException
                //It would be nice if we could check the custom JMS compression property here.  However
                //When jms bridging other, non-JMS-compliant message servers occur, there is no guarantee that
                //Custom properties will be proporgated
                byte[] bytes = JmsMessageUtils.getBytesFromMessage((BytesMessage) source);
                if (CompressionHelper.isCompressed(bytes))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Message recieved is compressed");
                    }
                    result = CompressionHelper.uncompressByteArray(bytes);
                } else
                {
                    //If the message is not compressed, handle it the standard way
                    result = JmsMessageUtils.getObjectForMessage(source);
                }
            } else
            {
                result = JmsMessageUtils.getObjectForMessage(source);
            }
        } catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
        return result;
    }


    public Session getSession()
    {
        return session;
    }


    public void setSession(Session session)
    {
        this.session = session;
    }

}