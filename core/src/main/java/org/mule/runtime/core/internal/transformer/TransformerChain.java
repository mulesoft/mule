/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noCurrentEventForTransformer;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformOnObjectUnsupportedTypeOfEndpoint;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import static org.apache.commons.lang3.StringUtils.capitalize;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.StringMessageUtils;
import org.mule.runtime.core.privileged.transformer.TransformerUtils;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

/**
 * A referencable chain of transformers that can be used as a single transformer
 */
public final class TransformerChain extends AbstractTransformer {

  private final List<Transformer> transformers;

  public TransformerChain(List<Transformer> transformers) {
    super();
    if (transformers.size() < 1) {
      throw new IllegalArgumentException("You must set at least one transformer");
    }
    this.transformers = new LinkedList<>(transformers);
  }

  public TransformerChain(Transformer... transformers) {
    this(asList(transformers));
    this.name = generateTransformerName();
    setReturnDataType(transformers[transformers.length - 1].getReturnDataType());
  }

  public TransformerChain(String name, List<Transformer> transformers) {
    this(transformers);
    this.name = name;
  }

  public TransformerChain(String name, Transformer... transformers) {
    this(name, asList(transformers));
  }

  @Override
  protected Object doTransform(Object src, Charset enc) throws TransformerException {
    throw new UnsupportedOperationException();
  }

  /**
   * Transform the message with no event specified.
   */
  @Override
  public final Object transform(Object src, Charset enc) throws TransformerException {
    DataType sourceType = DataType.fromType(src.getClass());
    if (!isSourceDataTypeSupported(sourceType)) {
      if (isIgnoreBadInput()) {
        logger
            .debug("Source type is incompatible with this transformer and property 'ignoreBadInput' is set to true, so the transformer chain will continue.");
        return src;
      } else {
        throw new TransformerException(transformOnObjectUnsupportedTypeOfEndpoint(getName(), src.getClass()), this);
      }
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Applying transformer {} ({})", getName(), getClass().getName());
      logger.debug("Object before transform: {}", StringMessageUtils.toString(src));
    }

    Message message;
    if (src instanceof Message) {
      message = (Message) src;
    } else if (muleContext.getConfiguration().isAutoWrapMessageAwareTransform()) {
      message = of(src);
    } else {
      throw new TransformerException(noCurrentEventForTransformer(), this);
    }

    Object result = transformMessage(message, enc);

    if (logger.isDebugEnabled()) {
      logger.debug(format("Object after transform: %s", StringMessageUtils.toString(result)));
    }
    result = checkReturnClass(result);
    return result;
  }

  private Object transformMessage(Message message, Charset outputEncoding) throws TransformerException {
    Message result = message;
    Object temp = message;
    Transformer lastTransformer = null;
    for (Object element : transformers) {
      lastTransformer = (Transformer) element;
      temp = lastTransformer.transform(temp);

      if (temp instanceof Message) {
        result = (Message) temp;
      } else {
        result = Message.builder(message).value(temp).build();
      }
    }
    if (lastTransformer != null && Message.class.isAssignableFrom(lastTransformer.getReturnDataType().getType())) {
      return result;
    } else {
      return result.getPayload().getValue();
    }
  }

  /**
   * Check if the return class is supported by this transformer
   */
  private Object checkReturnClass(Object object) throws TransformerException {
    try {
      TransformerUtils.checkTransformerReturnClass(this, object);
    } catch (TransformerException e) {
      throw new TransformerException(createStaticMessage(e.getMessage()), this);
    }

    return object;
  }

  @Override
  public void initialise() throws InitialisationException {
    for (Transformer transformer : transformers) {
      transformer.initialise();
    }
  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    super.setMuleContext(muleContext);
    for (Transformer transformer : transformers) {
      transformer.setMuleContext(muleContext);
    }
  }

  @Override
  protected String generateTransformerName() {
    String name = transformers.get(0).getClass().getSimpleName();
    int i = name.indexOf("To");
    DataType dt = transformers.get(transformers.size() - 1).getReturnDataType();
    if (i > 0 && dt != null) {
      String target = dt.getType().getSimpleName();
      if (target.equals("byte[]")) {
        target = "byteArray";
      }
      name = name.substring(0, i + 2) + capitalize(target);
    }
    return name;
  }

  public List<Transformer> getTransformers() {
    return unmodifiableList(transformers);
  }
}
