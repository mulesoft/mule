/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.simple;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

/**
 * Loads a template and parses its content to resolve expressions.
 */
public class ParseTemplateTransformer extends AbstractMessageTransformer {

  private String content;
  private String encoding;
  private Charset encoder;
  private String target;

  public ParseTemplateTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    if (encoding != null) {
      try {
        encoder = Charset.forName(encoding);
      } catch (Exception e) {
        throw new InitialisationException(createStaticMessage("%s is not a valid charset for encoding", encoding), e, this);
      }
    }
  }

  @Override
  public Event process(Event event) {
    if (content == null) {
      throw new IllegalArgumentException("Template cannot be null");
    }
    Object result = muleContext.getExpressionManager().parse(content, event, null);
    if (encoder != null) {
      result = encoder.encode((String) result);
    }
    Message resultMessage = Message.builder(event.getMessage()).payload(result).build();
    if (target == null) {
      return Event.builder(event).message(resultMessage).build();
    } else {
      return Event.builder(event).addVariable(target, resultMessage).build();
    }
  }

  @Override
  public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
    return event;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

}
