/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.privileged.processor.simple.SimpleMessageProcessor;


/**
 * Loads a template and parses its content to resolve expressions.
 */
public class ParseTemplateProcessor extends SimpleMessageProcessor {

  private String content;
  private String target;
  private String location;
  private String targetValue;

  @Override
  public void initialise() throws InitialisationException {
    //Check if both content and location are defined. If so, raise exception due to ambiguity.
    if (content != null && location != null) {
      throw new InitialisationException(createStaticMessage("Can't define both location and content at the same time"), this);
    }
    if (location != null) {
      loadContentFromLocation();
    }
  }

  private void loadContentFromLocation() throws InitialisationException {
    try {
      if (location == null) {
        throw new IllegalArgumentException("Location cannot be null");
      }
      content = IOUtils.getResourceAsString(location, this.getClass());

    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
  }

  private void evaluateCorrectArguments() {
    if (content == null) {
      throw new IllegalArgumentException("Template content cannot be null");
    }
    if (targetValue != null && target == null) {
      throw new IllegalArgumentException("Can't define a targetValue with no target");
    }

  }

  @Override
  public CoreEvent process(CoreEvent event) {
    evaluateCorrectArguments();
    Object result = muleContext.getExpressionManager().parse(content, event, null);
    Message resultMessage = Message.builder(event.getMessage()).value(result).nullAttributesValue().build();
    if (target == null) {
      return CoreEvent.builder(event).message(resultMessage).build();
    } else {
      if (targetValue == null) { //Return the whole message
        return CoreEvent.builder(event).addVariable(target, resultMessage).build();
      } else { //typeValue was defined by the user
        return CoreEvent.builder(event).addVariable(target,
                                                    muleContext.getExpressionManager()
                                                        .evaluate(targetValue, CoreEvent.builder(event)
                                                            .message(resultMessage).build()))
            .build();
      }

    }
  }

  public void setContent(String content) {
    this.content = content;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public void setTargetValue(String targetValue) {
    this.targetValue = targetValue;
  }

}
