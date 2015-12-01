/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.MessageTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides the same operations previously exposed by {@link MuleMessage} but decoupled from MuleMessage.
 *
 * TODO Redefine this interface as part of Mule 4.0 transformation improvements (MULE-9141)
 */
public class TransformationService
{

    private static final Log logger = LogFactory.getLog(TransformationService.class);

    private MuleContext muleContext;

    public TransformationService(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    /**
     * Applies a list of transformers returning the result of the transformation as a new message instance. If the list
     * of transformers is empty or transformation would be redundant then the same message instances will be returned.
     *
     * @param event the event being processed
     * @param transformers the transformers to apply to the message payload
     * @return the result of transformation
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    public MuleMessage applyTransformers(final MuleMessage message, final MuleEvent event, final List<? extends Transformer> transformers) throws MuleException
    {
        return applyTransformers(message, event, transformers, null);
    }

    /**
     * Applies a list of transformers returning the result of the transformation as a new message instance. If the list
     * of transformers is empty or transformation would be redundant then the same message instances will be returned.
     *
     * @param event the event being processed
     * @param transformers the transformers to apply to the message payload
     * @return the result of transformation
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    public MuleMessage applyTransformers(final MuleMessage message, final MuleEvent event, final Transformer... transformers) throws MuleException
    {
        return applyTransformers(message, event, Arrays.asList(transformers), null);
    }

    /**
     * Applies a list of transformers returning the result of the transformation as a new message instance. If the list
     * of transformers is empty or transformation would be redundant then the same message instances will be returned.
     *
     * @param event the event being processed
     * @param transformers the transformers to apply to the message payload
     * @param outputType the required output type for this transformation. By adding this parameter some additional
     * transformations will occur on the message payload to ensure that the final payload is of the specified type.
     * If no transformers can be found in the registry that can transform from the return type of the transformation
     * list to the outputType and exception will be thrown
     * @return the result of transformation
     * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a
     * are incompatible with the message payload
     */
    public MuleMessage applyTransformers(final MuleMessage message, final MuleEvent event, final List<? extends
            Transformer> transformers, Class<?> outputType) throws MuleException
    {
        MuleMessage result = message;
        if (!transformers.isEmpty())
        {
            result = applyAllTransformers(message, event, transformers);
        }

        if (null != outputType && !result.getPayload().getClass().isAssignableFrom(outputType))
        {
            result = new DefaultMuleMessage(getPayload(result, DataTypeFactory.create(outputType)), result, muleContext);
        }
        return result;
    }


    /**
     * Attempts to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be found
     * an exception is thrown.  Any transformers added to the registry will be checked for compatibility.
     * <p/>
     * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be
     * replaced with a byte[] representation as part of this operations.
     *
     * @param outputType the desired return type
     * @return The converted payload of this message. Note that this method will not alter the payload of this
     * message *unless* the payload is an InputStream in which case the stream will be read and the payload will become
     * the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during transformation of the
     * payload
     */
    public <T> Object getPayload(MuleMessage message, Class<T> outputType) throws TransformerException
    {
        DataType<T> dataType = DataTypeFactory.create(outputType);
        if (message instanceof MuleMessageCollection)
        {
            MuleMessageCollection messageCollection = (MuleMessageCollection) message;
            if (messageCollection.isInvalidatedPayload())
            {
                return getPayload(messageCollection, outputType);
            }
            else
            {
                List<T> results = new ArrayList<>(messageCollection.getMessageList().size());
                for (MuleMessage messageInList : messageCollection.getMessageList())
                {
                    results.add(getPayload(messageInList, dataType, messageInList.getEncoding()));
                }
                return results;
            }
        }
        else
        {
            return getPayload(message, dataType, message.getEncoding());
        }
    }

    /**
     * Attempts to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be found
     * an exception is thrown.  Any transformers added to the registry will be checked for compatibility
     * <p/>
     * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be
     * replaced with a byte[] representation as part of this operations.
     * <p/>
     * <b>NOTE:</b> This method is inconsistent with other 'getPayload' methods as it does not have any handling for
     * {@link MuleMessageCollection} messages at all and as such this method will always return a single object and
     * never a List of objects.
     *
     * @param outputType the desired return type
     * @return The converted payload of this message. Note that this method will not alter the payload of this
     * message *unless* the payload is an InputStream in which case the stream will be read and the payload will become
     * the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during transformation of the
     * payload
     */
    public <T> T getPayload(MuleMessage message, DataType<T> outputType) throws TransformerException
    {
        return getPayload(message, outputType, message.getEncoding());
    }

    /**
     * Obtains a {@link String} representation of the message payload. If encoding is required it will use the encoding
     * set on the message.
     * <p/>
     * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be
     * replaced with a byte[] representation as part of this operations.
     *
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     *
     */
    public String getPayloadAsString(MuleMessage message) throws Exception
    {
        return getPayloadAsString(message, message.getEncoding());
    }

    /**
     * Obtains a {@link String} representation of the message payload.
     * <p/>
     * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be
     * replaced with a byte[] representation as part of this operations.
     *
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(MuleMessage message, String encoding) throws Exception
    {
        if (message instanceof MuleMessageCollection)
        {
            if (((MuleMessageCollection) message).isInvalidatedPayload())
            {
                return getPayloadAsString(message);
            }
            else
            {
                throw new UnsupportedOperationException("getPayloadAsString(), use getPayload(DataType.STRING_DATA_TYPE)");
            }
        }
        else
        {
            return getPayload(message, DataType.STRING_DATA_TYPE, encoding);
        }
    }

    /**
     * Obtains a byte[] representation of the message payload.
     * <p/>
     * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be
     * replaced with a byte[] representation as part of this operations.
     *
     * @return byte array of the message
     * @throws Exception Implemetation may throw an endpoint specific exception
     *
     */
    public byte[] getPayloadAsBytes(MuleMessage message) throws Exception
    {
        if (message instanceof MuleMessageCollection)
        {
            if (((MuleMessageCollection) message).isInvalidatedPayload())
            {
                return getPayloadAsBytes(message);
            }
            else
            {
                throw new UnsupportedOperationException("getPayloadAsBytes(), use getPayload(DataType" +
                                                        ".BYTE_ARRAY_DATA_TYPE)");
            }
        }
        else
        {
            return getPayload(message, DataType.BYTE_ARRAY_DATA_TYPE, message.getEncoding());
        }
    }

    /**
     * Obtains a {@link String} representation of the message payload for logging without throwing exception.
     * <p/>
     * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be
     * replaced with a byte[] representation as part of this operations.
     *
     * @return message payload as object
     */
    public String getPayloadForLogging(MuleMessage message)
    {
        return getPayloadForLogging(message, message.getEncoding());
    }

    /**
     * Obtains a {@link String} representation of the message payload for logging without throwing exception.
     * If encoding is required it will use the encoding set on the message.
     * <p/>
     * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be
     * replaced with a byte[] representation as part of this operations.
     *
     * @return message payload as a String
     */
    public String getPayloadForLogging(MuleMessage message, String encoding)
    {
        try
        {
            if (message instanceof MuleMessageCollection)
            {
                if (((MuleMessageCollection) message).isInvalidatedPayload())
                {
                    return getPayloadAsString(message, encoding);
                }
                else
                {
                    return "[This is a message collection]";
                }
            }
            else
            {
                return getPayloadAsString(message, encoding);
            }
        }
        catch (Exception e)
        {
            return "[Message could not be converted to a String]";
        }
    }

    private MuleMessage applyAllTransformers(final MuleMessage message, final MuleEvent event, final List<? extends Transformer> transformers) throws MuleException
    {
        MuleMessage result = message;
        if (!transformers.isEmpty())
        {
            for (int index = 0; index < transformers.size(); index++)
            {
                Transformer transformer = transformers.get(index);

                Class<?> srcCls = result.getPayload().getClass();
                DataType<?> originalSourceType = DataTypeFactory.create(srcCls);

                if (transformer.isSourceDataTypeSupported(originalSourceType))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Using " + transformer + " to transform payload.");
                    }
                    result = transformMessage(result, event, transformer);
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Transformer " + transformer + " doesn't support the source payload: " + srcCls);
                    }

                    if (useExtendedTransformations())
                    {
                        if (canSkipTransformer(result, transformers, index))
                        {
                            continue;
                        }

                        // Resolves implicit conversion if possible
                        Transformer implicitTransformer = muleContext.getDataTypeConverterResolver().resolve(originalSourceType, transformer.getSourceDataTypes());

                        if (implicitTransformer != null)
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("Performing implicit transformation with: " + transformer);
                            }
                            result = transformMessage(result, event, implicitTransformer);
                            result = transformMessage(result, event, transformer);
                        }
                        else
                        {
                            throw new IllegalArgumentException("Cannot apply transformer " + transformer + " on source payload: " + srcCls);
                        }
                    }
                    else if (!transformer.isIgnoreBadInput())
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Exiting from transformer chain (ignoreBadInput = false)");
                        }
                        break;
                    }
                }
            }
        }
        return result;
    }

    private boolean canSkipTransformer(MuleMessage message, List<? extends Transformer> transformers, int index)
    {
        Transformer transformer = transformers.get(index);

        boolean skipConverter = false;

        if (transformer instanceof Converter)
        {
            if (index == transformers.size() - 1)
            {
                try
                {
                    TransformerUtils.checkTransformerReturnClass(transformer, message.getPayload());
                    skipConverter = true;
                }
                catch (TransformerException e)
                {
                    // Converter cannot be skipped
                }
            }
            else
            {
                skipConverter= true;
            }
        }

        if (skipConverter)
        {
            logger.debug("Skipping converter: " + transformer);
        }

        return skipConverter;
    }

    private boolean useExtendedTransformations()
    {
        boolean result = true;
        if (muleContext != null && muleContext.getConfiguration() != null)
        {
            result = muleContext.getConfiguration().useExtendedTransformations();
        }

        return result;
    }

    private MuleMessage transformMessage(final MuleMessage message, final MuleEvent event, final Transformer transformer) throws TransformerMessagingException, TransformerException
    {
        Object result;

        if (transformer instanceof MessageTransformer)
        {
            result = ((MessageTransformer) transformer).transform(message, event);
        }
        else
        {
            result = transformer.transform(message);
        }

        if (result instanceof MuleMessage)
        {
            if (!result.equals(message))
            {
                // Only copy the payload and properties of mule message transformer result if the message is a different
                // instance
                MuleMessage transformResult = (MuleMessage) result;
                return new DefaultMuleMessage(result, transformResult, muleContext, transformResult.getDataType());
            }
            return  message;
        }
        else
        {
            return new DefaultMuleMessage(result, message, muleContext, mergeDataType(message, transformer.getReturnDataType()));
        }
    }

    private DataType<?> mergeDataType(MuleMessage message, DataType<?> transformed)
    {
        DataType<?> original = message.getDataType();
        String mimeType = transformed.getMimeType() == null || MimeTypes.ANY.equals(transformed.getMimeType()) ? original.getMimeType() : transformed.getMimeType();
        String encoding = transformed.getEncoding() == null ? message.getEncoding() : transformed.getEncoding();
        Class<?> type = transformed.getType() == Object.class ? original.getType() : transformed.getType();

        DataType mergedDataType = DataTypeFactory.create(type, mimeType);
        mergedDataType.setEncoding(encoding);
        return mergedDataType;
    }

    /**
     * Attempts to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be
     * found an exception is thrown. Any transformers added to the registry will be checked for
     * compatibility.
     *
     * @param resultType the desired return type
     * @param encoding   the encoding to use if required
     * @return The converted payload of this message. Note that this method will not alter the
     *         payload of this message <b>unless</b> the payload is an {@link InputStream} in which
     *         case the stream will be read and the payload will become the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during
     *                              transformation of the payload.
     */
    @SuppressWarnings("unchecked")
    private  <T> T getPayload(MuleMessage message, DataType<T> resultType, String encoding) throws TransformerException
    {
        // Handle null by ignoring the request
        if (resultType == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("resultType").getMessage());
        }

        DataType source = DataTypeFactory.createFromObject(message);

        // If no conversion is necessary, just return the payload as-is
        if (resultType.isCompatibleWith(source))
        {
            return (T) message.getPayload();
        }

        // The transformer to execute on this message
        Transformer transformer = muleContext.getRegistry().lookupTransformer(source, resultType);
        if (transformer == null)
        {
            throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, resultType));
        }

        // Pass in the message itself
        Object result = transformer.transform(message, encoding);

        // Unless we disallow Object.class as a valid return type we need to do this extra check
        if (!resultType.getType().isAssignableFrom(result.getClass()))
        {
            throw new TransformerException(CoreMessages.transformOnObjectNotOfSpecifiedType(resultType, result));
        }

        // TODO MULE-9142 Seems to me that if the payload is consumable, then the new value should be set on the message
        // before throwing the previous exception

        // If the payload is a stream and we've consumed it, then we should set the payload on the
        // message. This is the only time this method will alter the payload on the message
        if (((DefaultMuleMessage) message).isPayloadConsumed(source.getType()))
        {
            message.setPayload(result, message.getDataType());
        }

        return (T) result;
    }

}
