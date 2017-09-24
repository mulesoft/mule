/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;


import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;

import java.nio.charset.Charset;

/**
 * Transformer that modifies the payload of the message according to the provided value.
 */
public class SetPayloadTransformer extends AbstractMessageTransformer {

  private AttributeEvaluator valueEvaluator;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    valueEvaluator.initialize(muleContext.getExpressionManager());
  }

  public SetPayloadTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public Object transformMessage(CoreEvent event, Charset outputEncoding) throws MessageTransformerException {
    if (valueEvaluator.getRawValue() == null) {
      return null;
    }

    return valueEvaluator.resolveValue(event);
  }

  public void setValue(String value) {
    valueEvaluator = new AttributeEvaluator(value);
  }
}
