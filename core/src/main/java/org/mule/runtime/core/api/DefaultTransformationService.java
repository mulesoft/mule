/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformOnObjectNotOfSpecifiedType;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.core.privileged.transformer.TransformerUtils;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides the same operations previously exposed by {@link Message} but decoupled from Message.
 *
 * TODO Redefine this interface as part of Mule 4.0 transformation improvements (MULE-9141)
 */
public class DefaultTransformationService implements TransformationService {

  private static final Logger logger = LoggerFactory.getLogger(DefaultTransformationService.class);

  private MuleContext muleContext;

  @Inject
  public DefaultTransformationService(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
   * TODO MULE-12982 - cleanup DefaultTransformationService
   * <p/>
   * @deprecated use {@code {@link #transform(Message, DataType)} or {@link #transform(Object, DataType, DataType)}} instead
   * or use a method that could be exposed in the API.
   * <p/>
   * Given a {@code value) it will try to transform it to the expected type defined in the {@code expectedDataType}
   * <p/>
   * @param value the value to transform
   * @param valueDataType the value's {@link DataType}
   * @param expectedDataType the expected type's {@link DataType}
   * @param event the event to perform the transformation
   * @return the transformed value
   * @throws MessagingException If could not be able to find a proper transformer do obtain the desired type
   * @throws MessageTransformerException If a problem occurs transforming the value
   * @throws TransformerException If a problem occurs transforming the value
   */
  @Deprecated
  public Object internalTransform(Object value, DataType valueDataType, DataType expectedDataType)
      throws MessagingException, MessageTransformerException, TransformerException {
    Transformer transformer;
    if (value != null) {
      try {
        transformer = muleContext.getRegistry().lookupTransformer(valueDataType, expectedDataType);
      } catch (TransformerException e) {
        throw new TransformerException(createStaticMessage(String
            .format("The value '%s' of type %s could not be transformed to the desired type %s",
                    value.toString(), value.getClass().getName(), expectedDataType.getType().getName()), e));
      }

      return transformer.transform(value);
    }
    return null;
  }

  /**
   * Applies a list of transformers returning the result of the transformation as a new message instance. If the list of
   * transformers is empty or transformation would be redundant then the same message instances will be returned.
   *
   *
   * @param message
   * @param event the event being processed
   * @param transformers the transformers to apply to the message payload
   *
   * @return the result of transformation
   * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a are incompatible
   *         with the message payload
   */
  public Message applyTransformers(final Message message, final BaseEvent event,
                                   final List<? extends Transformer> transformers)
      throws MuleException {
    return applyAllTransformers(message, event, transformers);
  }

  /**
   * Applies a list of transformers returning the result of the transformation as a new message instance. If the list of
   * transformers is empty or transformation would be redundant then the same message instances will be returned.
   *
   *
   * @param message
   * @param event the event being processed
   * @param transformers the transformers to apply to the message payload
   *
   * @return the result of transformation
   * @throws TransformerException if a transformation error occurs or one or more of the transformers passed in a are incompatible
   *         with the message payload
   */
  public Message applyTransformers(final Message message, final BaseEvent event, final Transformer... transformers)
      throws MuleException {
    return applyAllTransformers(message, event, asList(transformers));
  }

  /**
   * TODO MULE-12982 - cleanup DefaultTransformationService
   * <p/>
   * @deprecated use {@code {@link #transform(Message, DataType)} or {@link #transform(Object, DataType, DataType)}} instead
   * or use a method that could be exposed in the API.
   * <p/>
   * Attempts to obtain the payload of this message with the desired Class type. This will try and resolve a transformer that can
   * do this transformation. If a transformer cannot be found an exception is thrown. Any transformers added to the registry will
   * be checked for compatibility
   * <p/>
   * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be replaced
   * with a byte[] representation as part of this operations.
   * <p/>
   *
   *
   * @param message
   * @param outputDataType the desired return type
   * @return The converted payload of this message. Note that this method will not alter the payload of this message *unless* the
   *         payload is an InputStream in which case the stream will be read and the payload will become the fully read stream.
   * @throws MessageTransformerException if a transformer cannot be found or there is an error during transformation of the payload
   */
  @Deprecated
  public Message internalTransform(Message message, DataType outputDataType) throws MessageTransformerException {
    checkNotNull(message, "Message cannot be null");
    checkNotNull(outputDataType, "DataType cannot be null");

    return Message.builder(message).value(getPayload(message, outputDataType, resolveEncoding(message))).build();
  }

  /**
   * Obtains a {@link String} representation of the message payload for logging without throwing exception.
   * <p/>
   * If the existing payload is consumable (i.e. can't be read twice) then the existing payload of the message will be replaced
   * with a byte[] representation as part of this operations.
   *
   * @return message payload as object
   * @param message
   */
  public String getPayloadForLogging(Message message) {
    return getPayloadForLogging(message, resolveEncoding(message));
  }

  protected Charset resolveEncoding(Message message) {
    return message.getPayload().getDataType().getMediaType().getCharset().orElse(getDefaultEncoding(muleContext));
  }

  /**
   * Obtains a {@link String} representation of the message payload for logging without throwing exception. If encoding is
   * required it will use the encoding set on the message.
   * <p>
   * If the existing payload is consumable (i.e. can't be read twice) or an exception occurs during transformation then the an
   * exception won't be thrown but rather a description of the payload type will be returned.
   *
   * @return message payload as a String or message with the payload type if payload can't be converted to a String
   */
  public String getPayloadForLogging(Message message, Charset encoding) {
    DataType dataType = message.getPayload().getDataType();
    if (!dataType.isStreamType()) {
      try {
        return getPayload(message, DataType.STRING, encoding);
      } catch (MessageTransformerException e) {
        return "Payload could not be converted to a String. Payload type is " + dataType.getType();
      }
    }
    return "Payload is a stream of type: " + dataType.getType();
  }

  private Message applyAllTransformers(final Message message, final BaseEvent event,
                                       final List<? extends Transformer> transformers)
      throws MuleException {
    Message result = message;
    if (!transformers.isEmpty()) {
      for (int index = 0; index < transformers.size(); index++) {
        Transformer transformer = transformers.get(index);

        Class<?> srcCls = result.getPayload().getDataType().getType();
        DataType originalSourceType = DataType.fromType(srcCls);

        if (transformer.isSourceDataTypeSupported(originalSourceType)) {
          if (logger.isDebugEnabled()) {
            logger.debug("Using " + transformer + " to transform payload.");
          }
          result = transformMessage(result, event, transformer);
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("Transformer " + transformer + " doesn't support the source payload: " + srcCls);
          }

          if (canSkipTransformer(result, transformers, index)) {
            continue;
          }

          // Resolves implicit conversion if possible
          Transformer implicitTransformer =
              muleContext.getDataTypeConverterResolver().resolve(originalSourceType, transformer.getSourceDataTypes());

          if (implicitTransformer != null) {
            if (logger.isDebugEnabled()) {
              logger.debug("Performing implicit transformation with: " + transformer);
            }
            result = transformMessage(result, event, implicitTransformer);
            result = transformMessage(result, event, transformer);
          } else {
            throw new IllegalArgumentException("Cannot apply transformer " + transformer + " on source payload: " + srcCls);
          }
        }
      }

      Transformer lastTransformer = transformers.get(transformers.size() - 1);
      DataType returnDataType = lastTransformer.getReturnDataType();
      checkResultDataType(message, returnDataType, result.getPayload().getValue());
    }
    return result;
  }

  private boolean canSkipTransformer(Message message, List<? extends Transformer> transformers, int index) {
    Transformer transformer = transformers.get(index);

    boolean skipConverter = false;

    if (transformer instanceof Converter) {
      if (index == transformers.size() - 1) {
        try {
          TransformerUtils.checkTransformerReturnClass(transformer, message.getPayload().getValue());
          skipConverter = true;
        } catch (TransformerException e) {
          // Converter cannot be skipped
        }
      } else {
        skipConverter = true;
      }
    }

    if (skipConverter) {
      logger.debug("Skipping converter: " + transformer);
    }

    return skipConverter;
  }

  private Message transformMessage(final Message message, final BaseEvent event, final Transformer transformer)
      throws MessageTransformerException, TransformerException {
    Object result;

    if (transformer instanceof MessageTransformer) {
      result = ((MessageTransformer) transformer).transform(message, event);
    } else {
      result = transformer.transform(message);
    }

    if (result instanceof Message) {
      return (Message) result;
    } else {
      // We need to use message from event if it's available in case the transformer mutated the message by creating
      // a new message instance. This issue goes away once transformers are cleaned up and always return event or
      // message. See MULE-9342
      Message messagePostTransform = (event != null && event.getMessage() != null) ? event.getMessage() : message;
      return Message.builder(messagePostTransform).value(result)
          .mediaType(mergeMediaType(messagePostTransform, transformer.getReturnDataType())).build();
    }
  }

  private MediaType mergeMediaType(Message message, DataType transformed) {
    DataType original = message.getPayload().getDataType();
    MediaType mimeType = ANY.matches(transformed.getMediaType()) ? original.getMediaType() : transformed.getMediaType();
    Charset encoding = transformed.getMediaType().getCharset()
        .orElse(message.getPayload().getDataType().getMediaType().getCharset().orElse(getDefaultEncoding(muleContext)));

    return DataType.builder().mediaType(mimeType).charset(encoding).build().getMediaType();
  }

  /**
   * Attempts to obtain the payload of this message with the desired Class type. This will try and resolve a transformer that can
   * do this transformation. If a transformer cannot be found an exception is thrown. Any transformers added to the registry will
   * be checked for compatibility.
   *
   * @param resultType the desired return type
   * @param encoding the encoding to use if required
   * @return The converted payload of this message. Note that this method will not alter the payload of this message <b>unless</b>
   *         the payload is an {@link InputStream} in which case the stream will be read and the payload will become the fully
   *         read stream.
   * @throws MessageTransformerException if a transformer cannot be found or there is an error during transformation of the payload.
   */
  @SuppressWarnings("unchecked")
  private <T> T getPayload(Message message, DataType resultType, Charset encoding) throws MessageTransformerException {
    // Handle null by ignoring the request
    if (resultType == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("resultType").getMessage());
    }

    DataType dataType = DataType.builder(resultType).type(message.getPayload().getDataType().getType()).build();

    // If no conversion is necessary, just return the payload as-is
    if (resultType.isCompatibleWith(dataType)) {
      return (T) message.getPayload().getValue();
    }

    // The transformer to execute on this message
    Transformer transformer = null;
    try {
      transformer = muleContext.getRegistry().lookupTransformer(dataType, resultType);
      if (transformer == null) {
        throw new MessageTransformerException(CoreMessages.noTransformerFoundForMessage(dataType, resultType), null, message);
      }

      // Pass in the message itself
      Object result = transformer.transform(message, encoding);
      // Unless we disallow Object.class as a valid return type we need to do this extra check
      checkResultDataType(message, resultType, result);

      return (T) result;
    } catch (MessageTransformerException e) {
      throw e;
    } catch (TransformerException e) {
      throw new MessageTransformerException(transformer, e, message);
    }
  }

  private void checkResultDataType(Message message, DataType resultType, Object value) throws MessageTransformerException {
    if (value != null && !resultType.getType().isAssignableFrom(value.getClass())) {
      TypedValue<Object> actualType = TypedValue.of(value);
      Message transformedMessage = Message.builder(message).payload(actualType).build();
      throw new MessageTransformerException(transformOnObjectNotOfSpecifiedType(resultType, actualType), null,
                                            transformedMessage);
    }
  }

  @Override
  public Object transform(Object value, DataType valueDataType, DataType expectedDataType) {
    return transformThrowingRuntimeException(() -> this.internalTransform(value, valueDataType, expectedDataType));
  }

  @Override
  public Message transform(Message message, DataType outputDataType) {
    return transformThrowingRuntimeException(() -> this.internalTransform(message, outputDataType));
  }

  private <T> T transformThrowingRuntimeException(CheckedSupplier<T> supplier) {
    try {
      return supplier.get();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }
}
