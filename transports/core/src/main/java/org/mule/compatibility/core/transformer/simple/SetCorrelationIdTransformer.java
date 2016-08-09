/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.util.AttributeEvaluator;

import java.nio.charset.Charset;

public class SetCorrelationIdTransformer extends AbstractMessageTransformer {

  private AttributeEvaluator correlationIdEvaluator;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    correlationIdEvaluator.initialize(muleContext.getExpressionManager());
  }

  public SetCorrelationIdTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    final MuleMessage msg =
        MuleMessage.builder(event.getMessage()).correlationId(correlationIdEvaluator.resolveValue(event).toString()).build();
    event.setMessage(msg);
    return msg;
  }


  public void setCorrelationId(String correlationId) {
    correlationIdEvaluator = new AttributeEvaluator(correlationId);
  }
}
