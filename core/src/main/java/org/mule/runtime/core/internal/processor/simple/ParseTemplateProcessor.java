/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import static java.lang.System.getProperty;
import static java.nio.charset.Charset.forName;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.create;
import static org.mule.runtime.api.metadata.MediaType.parse;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsStream;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.compile;
import static org.mule.runtime.core.internal.util.rx.Operators.outputToTarget;

import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.privileged.processor.simple.SimpleMessageProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.activation.MimetypesFileTypeMap;


/**
 * Loads a template and parses its content to resolve expressions.
 */
public class ParseTemplateProcessor extends SimpleMessageProcessor {

  private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
  private static final Boolean KEEP_TYPE_TARGET_AND_TARGET_VAR =
      new Boolean(getProperty(SYSTEM_PROPERTY_PREFIX + "parse.template.keep.target.var.type", "true"));

  private String content;
  private MediaType outputMimeType;
  private Charset outputEncoding;
  private String target;
  private String location;
  private String targetValue;
  private CompiledExpression targetValueExpression;

  @Override
  public void initialise() throws InitialisationException {
    //Check if both content and location are defined. If so, raise exception due to ambiguity.
    if (content != null && location != null) {
      throw new InitialisationException(createStaticMessage("Can't define both location and content at the same time"), this);
    }
    if (content == null && location == null) {
      throw new InitialisationException(
                                        createStaticMessage("One of 'location' or 'content' should be defined but they are both null"),
                                        this);
    }
    if (location != null) {
      loadContentFromLocation();
      if (outputMimeType == null) {
        guessMimeType();
      }
    }

    if (targetValue != null && target == null) {
      throw new InitialisationException(createStaticMessage("Can't define a targetValue with no target"), this);
    }

    targetValueExpression = compile(targetValue, muleContext.getExpressionManager());
  }

  private void loadContentFromLocation() throws InitialisationException {
    InputStream contentStream;
    try {
      contentStream = getResourceAsStream(location, getClass());
    } catch (IOException e) {
      throw new InitialisationException(createStaticMessage("Error loading template from location"), this);
    }
    if (contentStream == null) {
      throw new InitialisationException(createStaticMessage("Template location: " + location + " not found"), this);
    }
    if (outputEncoding != null) {
      content = IOUtils.toString(contentStream, outputEncoding);
    } else {
      content = IOUtils.toString(contentStream);
    }
  }

  private void guessMimeType() {
    MediaType fromLocationMediaType = parse(mimetypesFileTypeMap.getContentType(location));
    //This is because BINARY is the default value returned if nothing can be resolved. We should not force the value.
    if (!BINARY.equals(fromLocationMediaType)) {
      outputMimeType = fromLocationMediaType;
    }
  }

  private MediaType buildMediaType() {
    if (outputMimeType != null) {
      if (outputEncoding != null) {
        return create(outputMimeType.getPrimaryType(), outputMimeType.getSubType(), outputEncoding);
      }
      return outputMimeType;
    }
    return null;
  }

  @Override
  public CoreEvent process(CoreEvent event) {
    String result = muleContext.getExpressionManager().parseLogTemplate(content, event, getLocation(), NULL_BINDING_CONTEXT);
    Message.Builder messageBuilder = Message.builder(event.getMessage()).value(result).nullAttributesValue();
    MediaType configuredMediaType = buildMediaType();
    if (configuredMediaType != null) {
      messageBuilder.mediaType(configuredMediaType);
    }
    Message resultMessage = messageBuilder.build();
    if (target == null) {
      return CoreEvent.builder(event).message(resultMessage).build();
    } else {
      if (targetValue == null) { //Return the whole message
        return CoreEvent.builder(event).addVariable(target, resultMessage).build();
      } else { //typeValue was defined by the user
        if (KEEP_TYPE_TARGET_AND_TARGET_VAR) {
          return outputToTarget(event, target, targetValueExpression, muleContext.getExpressionManager())
              .apply(CoreEvent.builder(event).message(resultMessage).build());
        } else {
          return CoreEvent.builder(event).addVariable(target,
                                                      muleContext.getExpressionManager()
                                                          .evaluate(targetValue, CoreEvent.builder(event)
                                                              .message(resultMessage).build()))
              .build();
        }
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

  public void setOutputMimeType(String outputMimeType) {
    this.outputMimeType = parse(outputMimeType);
  }

  public void setOutputEncoding(String encoding) {
    this.outputEncoding = forName(encoding);
  }
}
