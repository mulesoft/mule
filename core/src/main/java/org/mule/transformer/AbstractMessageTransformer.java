/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.MessageTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;

/**
 * <code>AbstractMessageTransformer</code> is a transformer that has a reference
 * to the current message. This message can be used to obtain properties associated
 * with the current message which are useful to the transform. Note that when part of a
 * transform chain, the MuleMessage payload reflects the pre-transform message state,
 * unless there is no current event for this thread, then the message will be a new
 * DefaultMuleMessage with the src as its payload. Transformers should always work on the
 * src object not the message payload.
 *
 * @see org.mule.api.MuleMessage
 * @see org.mule.DefaultMuleMessage
 */

public abstract class AbstractMessageTransformer extends AbstractTransformer implements MessageTransformer
{
    /**
     * @param dataType the type to check against
     * @param exactMatch if set to true, this method will look for an exact match to
     *            the data type, if false it will look for a compatible data type.
     * @return whether the data type is supported
     */
    @Override
    public boolean isSourceDataTypeSupported(DataType<?> dataType, boolean exactMatch)
    {
        //TODO RM* This is a bit of hack since we could just register MuleMessage as a supportedType, but this has some
        //funny behaviour in certain ObjectToXml transformers
        return (super.isSourceDataTypeSupported(dataType, exactMatch) || MuleMessage.class.isAssignableFrom(dataType.getType()));
    }

    /**
     * Perform a non-message aware transform. This should never be called
     */
    @Override
    public final Object doTransform(Object src, String enc) throws TransformerException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Transform the message with no event specified.
     */
    @Override
    public final Object transform(Object src, String enc) throws TransformerException
    {
        try
        {
            return transform(src, enc, null);
        }
        catch (TransformerMessagingException e)
        {
            // Try to avoid double-wrapping
            Throwable cause = e.getCause();
            if (cause instanceof TransformerException)
            {
                TransformerException te = (TransformerException) cause;
                if (te.getTransformer() == this)
                {
                    throw te;
                }
            }
            throw new TransformerException(e.getI18nMessage(), this, e);
        }
    }

    public Object transform(Object src, MuleEvent event) throws TransformerMessagingException
    {
        return transform(src, getEncoding(src), event);
    }

    public final Object transform(Object src, String enc, MuleEvent event) throws TransformerMessagingException
    {
        DataType<?> sourceType = DataTypeFactory.create(src.getClass());
        if (!isSourceDataTypeSupported(sourceType))
        {
            if (isIgnoreBadInput())
            {
                logger.debug("Source type is incompatible with this transformer and property 'ignoreBadInput' is set to true, so the transformer chain will continue.");
                return src;
            }
            else
            {
                Message msg = CoreMessages.transformOnObjectUnsupportedTypeOfEndpoint(getName(),
                    src.getClass(), endpoint);
                /// FIXME
                throw new TransformerMessagingException(msg, event, this);
            }
        }
        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Applying transformer %s (%s)", getName(), getClass().getName()));
            logger.debug(String.format("Object before transform: %s", StringMessageUtils.toString(src)));
        }

        MuleMessage message;
        if (src instanceof MuleMessage)
        {
            message = (MuleMessage) src;
        }
        else if (muleContext.getConfiguration().isAutoWrapMessageAwareTransform())
        {
            message = new DefaultMuleMessage(src, muleContext);
        }
        else
        {
            if (event == null)
            {
                throw new TransformerMessagingException(CoreMessages.noCurrentEventForTransformer(), event, this);
            }
            message = event.getMessage();
            if (!message.getPayload().equals(src))
            {
                throw new IllegalStateException("Transform payload does not match current event");
            }
        }

        Object result;
        try
        {
            result = transformMessage(message, enc);
        }
        catch (TransformerException e)
        {
            throw new TransformerMessagingException(e.getI18nMessage(), event, this, e);
        }

        if (result == null)
        {
            result = NullPayload.getInstance();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Object after transform: %s", StringMessageUtils.toString(result)));
        }

        result = checkReturnClass(result, event);
        return result;
    }

    /**
     * Check if the return class is supported by this transformer
     */
    protected Object checkReturnClass(Object object, MuleEvent event) throws TransformerMessagingException
    {

        //Null is a valid return type
        if(object==null || object instanceof NullPayload && isAllowNullReturn())
        {
            return object;
        }

        if (returnType != null)
        {
            DataType<?> dt = DataTypeFactory.create(object.getClass());
            if (!returnType.isCompatibleWith(dt))
            {
                throw new TransformerMessagingException(
                        CoreMessages.transformUnexpectedType(dt, returnType),
                        event, this);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("The transformed object is of expected type. Type is: " +
                    ClassUtils.getSimpleName(object.getClass()));
        }

        return object;
    }

    /**
     * Transform the message
     */
    public abstract Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException;
}
