/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformOnObjectNotOfSpecifiedType;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.func.CheckedSupplier;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;

import java.io.InputStream;
import java.nio.charset.Charset;

import javax.inject.Inject;

/**
 * Provides the same operations previously exposed by {@link Message} but decoupled from Message.
 *
 * TODO Redefine this interface as part of Mule 4.0 transformation improvements (MULE-9141)
 */
public class DefaultTransformationService implements TransformationService {

  protected MuleContext muleContext;

  @Inject
  public DefaultTransformationService(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  /**
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
  private Message internalTransform(Message message, DataType outputDataType) throws MessageTransformerException {
    checkNotNull(message, "Message cannot be null");
    checkNotNull(outputDataType, "DataType cannot be null");

    return Message.builder(message).value(getPayload(message, outputDataType, resolveEncoding(message))).build();
  }

  /**
   * @param value the value to transform
   * @param valueDataType the value's {@link DataType}
   * @param expectedDataType the expected type's {@link DataType}
   * @return the transformed value
   * @throws MessageTransformerException If a problem occurs transforming the value
   * @throws TransformerException If a problem occurs transforming the value
   */
  private Object internalTransform(Object value, DataType valueDataType, DataType expectedDataType)
      throws MessageTransformerException, TransformerException {
    Transformer transformer;
    if (value != null) {
      try {
        transformer = ((MuleContextWithRegistries) muleContext).getRegistry().lookupTransformer(valueDataType, expectedDataType);
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
  protected <T> T getPayload(Message message, DataType resultType, Charset encoding) throws MessageTransformerException {
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
      transformer = ((MuleContextWithRegistries) muleContext).getRegistry().lookupTransformer(dataType, resultType);
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

  protected void checkResultDataType(Message message, DataType resultType, Object value) throws MessageTransformerException {
    if (value != null && !resultType.getType().isAssignableFrom(value.getClass())) {
      TypedValue<Object> actualType = TypedValue.of(value);
      Message transformedMessage = Message.builder(message).payload(actualType).build();
      throw new MessageTransformerException(transformOnObjectNotOfSpecifiedType(resultType, actualType), null,
                                            transformedMessage);
    }
  }

  protected Charset resolveEncoding(Message message) {
    return message.getPayload().getDataType().getMediaType().getCharset().orElse(getDefaultEncoding(muleContext));
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
