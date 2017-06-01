/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.DefaultEventContext.create;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.client.MuleClientFlowConstruct;
import org.mule.runtime.core.api.transformer.MessageTransformer;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.core.util.StringMessageUtils;

import java.nio.charset.Charset;

/**
 * <code>AbstractMessageTransformer</code> is a transformer that has a reference to the current message. This message can be used
 * to obtain properties associated with the current message which are useful to the transform. Note that when part of a transform
 * chain, the Message payload reflects the pre-transform message state, unless there is no current event for this thread, then the
 * message will be a new {@link Message} with the src as its payload. Transformers should always work on the src object not the
 * message payload.
 *
 * @see InternalMessage
 */

public abstract class AbstractMessageTransformer extends AbstractTransformer implements MessageTransformer {

  /**
   * @param dataType the type to check against
   * @param exactMatch if set to true, this method will look for an exact match to the data type, if false it will look for a
   *        compatible data type.
   * @return whether the data type is supported
   */
  @Override
  public boolean isSourceDataTypeSupported(DataType dataType, boolean exactMatch) {
    // TODO RM* This is a bit of hack since we could just register Message as a supportedType, but this has some
    // funny behaviour in certain ObjectToXml transformers
    return (super.isSourceDataTypeSupported(dataType, exactMatch) || Message.class.isAssignableFrom(dataType.getType()));
  }

  /**
   * Perform a non-message aware transform. This should never be called
   */
  @Override
  public final Object doTransform(Object src, Charset enc) throws TransformerException {
    throw new UnsupportedOperationException();
  }

  /**
   * Transform the message with no event specified.
   */
  @Override
  public final Object transform(Object src, Charset enc) throws TransformerException {
    try {
      return transform(src, enc, null);
    } catch (MessageTransformerException e) {
      // Try to avoid double-wrapping
      Throwable cause = e.getCause();
      if (cause instanceof TransformerException) {
        TransformerException te = (TransformerException) cause;
        if (te.getTransformer() == this) {
          throw te;
        }
      }
      throw new TransformerException(e.getI18nMessage(), this, e);
    }
  }

  @Override
  public Object transform(Object src, Event event) throws MessageTransformerException {
    return transform(src, resolveEncoding(src), event);
  }

  @Override
  public final Object transform(Object src, Charset enc, Event event) throws MessageTransformerException {
    DataType sourceType = DataType.fromType(src.getClass());
    if (!isSourceDataTypeSupported(sourceType)) {
      if (isIgnoreBadInput()) {
        logger
            .debug("Source type is incompatible with this transformer and property 'ignoreBadInput' is set to true, so the transformer chain will continue.");
        return src;
      } else {
        I18nMessage msg = CoreMessages.transformOnObjectUnsupportedTypeOfEndpoint(getName(), src.getClass());
        /// FIXME
        throw new MessageTransformerException(msg, this);
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Applying transformer %s (%s)", getName(), getClass().getName()));
      logger.debug(String.format("Object before transform: %s", StringMessageUtils.toString(src)));
    }

    Message message;
    if (src instanceof Message) {
      message = (Message) src;
    }
    // TODO MULE-9342 Clean up transformer vs message transformer confusion
    else if (src instanceof Event) {
      event = (Event) src;
      message = event.getMessage();
    } else if (muleContext.getConfiguration().isAutoWrapMessageAwareTransform()) {
      message = of(src);
    } else {
      if (event == null) {
        throw new MessageTransformerException(CoreMessages.noCurrentEventForTransformer(), this);
      }
      message = event.getMessage();
    }

    Object result;
    // TODO MULE-9342 Clean up transformer vs message transformer confusion
    if (event == null) {
      MuleClientFlowConstruct flowConstruct =
          new MuleClientFlowConstruct(muleContext);
      ComponentLocation location = getLocation() != null ? getLocation() : fromSingleComponent("AbstractMessageTransformer");
      event = Event.builder(create(flowConstruct, location)).message(message)
          .flow(flowConstruct).build();
    }
    try {
      result = transformMessage(event, enc);
    } catch (TransformerException e) {
      throw new MessageTransformerException(e.getI18nMessage(), this, e);
    }

    if (logger.isDebugEnabled()) {
      logger.debug(String.format("Object after transform: %s", StringMessageUtils.toString(result)));
    }

    result = checkReturnClass(result, event);
    return result;
  }

  /**
   * Check if the return class is supported by this transformer
   */
  protected Object checkReturnClass(Object object, Event event) throws MessageTransformerException {

    // Null is a valid return type
    if (object == null && isAllowNullReturn()) {
      return object;
    }

    if (getReturnDataType() != null) {
      DataType dt = DataType.fromObject(object);
      if (!getReturnDataType().isCompatibleWith(dt)) {
        throw new MessageTransformerException(CoreMessages.transformUnexpectedType(dt, getReturnDataType()), this);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("The transformed object is of expected type. Type is: " + ClassUtils.getSimpleName(object.getClass()));
    }

    return object;
  }

  /**
   * Transform the message
   */
  public abstract Object transformMessage(Event event, Charset outputEncoding) throws TransformerException;
}
