/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.parse;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.simple.SimpleMessageProcessor;

import javax.activation.MimetypesFileTypeMap;


/**
 * Loads a template and parses its content to resolve expressions.
 */
public class ParseTemplateProcessor extends SimpleMessageProcessor {

  private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();

  private String content;
  private MediaType contentMediaType;
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
      content = getResourceAsString(location, this.getClass());
      if (contentMediaType == null) {
        MediaType fromLocationMediaType = parse(mimetypesFileTypeMap.getContentType(location));
        //This is because BINARY is the default value returned if nothing can be resolved. We should not force the value.
        if (!BINARY.equals(fromLocationMediaType)) {
          contentMediaType = fromLocationMediaType;
        }
      }
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
    Object result = muleContext.getExpressionManager().parseLogTemplate(content, event, getLocation(), NULL_BINDING_CONTEXT);
    Message.Builder mesageBuilder = Message.builder(event.getMessage()).value(result).nullAttributesValue();
    if (contentMediaType != null) {
      mesageBuilder.mediaType(contentMediaType);
    }
    Message resultMessage = mesageBuilder.build();
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

  public void setContentMediaType(MediaType contentMediaType) {
    this.contentMediaType = contentMediaType;
  }
}
