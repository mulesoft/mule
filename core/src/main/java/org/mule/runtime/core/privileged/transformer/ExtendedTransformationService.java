/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transformer;

import static java.util.Arrays.asList;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.privileged.transformer.TransformerUtils.checkTransformerReturnClass;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.DefaultTransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;

import javax.inject.Inject;

public class ExtendedTransformationService extends DefaultTransformationService {

  private static final Logger logger = LoggerFactory.getLogger(ExtendedTransformationService.class);


  @Inject
  public ExtendedTransformationService(MuleContext muleContext) {
    super(muleContext);
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
  public Message applyTransformers(final Message message, final CoreEvent event,
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
  public Message applyTransformers(final Message message, final CoreEvent event, final Transformer... transformers)
      throws MuleException {
    return applyAllTransformers(message, event, asList(transformers));
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

  private Message applyAllTransformers(final Message message, final CoreEvent event,
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
          checkTransformerReturnClass(transformer, message.getPayload().getValue());
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

  private Message transformMessage(final Message message, final CoreEvent event, final Transformer transformer)
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

}
