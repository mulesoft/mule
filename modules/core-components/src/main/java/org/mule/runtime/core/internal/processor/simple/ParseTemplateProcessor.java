/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.simple;

import static org.mule.runtime.api.el.BindingContextUtils.MESSAGE;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.ExpressionLanguageUtils.sanitize;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MediaType.BINARY;
import static org.mule.runtime.api.metadata.MediaType.create;
import static org.mule.runtime.api.metadata.MediaType.parse;
import static org.mule.runtime.api.metadata.MediaType.parseDefinedInApp;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.api.util.MuleSystemProperties.isParseTemplateUseLegacyDefaultTargetValue;
import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsStream;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.compile;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.isSanitizedPayload;
import static org.mule.runtime.core.internal.util.rx.Operators.outputToTarget;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;
import static java.nio.charset.Charset.forName;

import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.el.ExpressionLanguageUtils;
import org.mule.runtime.core.internal.interception.HasParamsAsTemplateProcessor;
import org.mule.runtime.core.privileged.processor.simple.SimpleMessageProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import jakarta.activation.MimetypesFileTypeMap;
import jakarta.inject.Inject;

/**
 * Loads a template and parses its content to resolve expressions.
 */
public class ParseTemplateProcessor extends SimpleMessageProcessor implements HasParamsAsTemplateProcessor {

  private static final MimetypesFileTypeMap mimetypesFileTypeMap = new MimetypesFileTypeMap();
  private static final boolean KEEP_TYPE_TARGET_AND_TARGET_VAR =
      parseBoolean(getProperty(SYSTEM_PROPERTY_PREFIX + "parse.template.keep.target.var.type", "true"));
  private static final String LEGACY_DEFAULT_TARGET_VALUE = "#[" + MESSAGE + "]";

  private ExtendedExpressionManager expressionManager;

  private String content;
  private MediaType outputMimeType;
  private Charset outputEncoding;
  private String target;
  private String location;
  private String targetValue;
  private CompiledExpression targetValueExpression;

  @Override
  public void initialise() throws InitialisationException {
    // Check if both content and location are defined. If so, raise exception due to ambiguity.
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

    if (targetValue != null) {
      targetValueExpression = compile(targetValue, expressionManager);
    }
  }

  private void loadContentFromLocation() throws InitialisationException {
    InputStream contentStream = null;
    try {
      contentStream = getResourceAsStream(location, getClass());

      if (contentStream == null) {
        throw new InitialisationException(createStaticMessage("Template location: " + location + " not found"), this);
      }
      if (outputEncoding != null) {
        content = IOUtils.toString(contentStream, outputEncoding);
      } else {
        content = IOUtils.toString(contentStream);
      }
    } catch (IOException e) {
      throw new InitialisationException(createStaticMessage("Error loading template from location"), this);
    } finally {
      if (contentStream != null) {
        closeQuietly(contentStream);
      }
    }
  }

  private void guessMimeType() {
    MediaType fromLocationMediaType = parse(mimetypesFileTypeMap.getContentType(location));
    // This is because BINARY is the default value returned if nothing can be resolved. We should not force the value.
    if (!BINARY.equals(fromLocationMediaType)) {
      outputMimeType = fromLocationMediaType;
    }
  }

  private void evaluateCorrectArguments() {
    if (target == null && !(isSanitizedPayload(sanitize(targetValue))
        || isParseTemplateUseLegacyDefaultTargetValue() && LEGACY_DEFAULT_TARGET_VALUE.equals(targetValue))) {
      throw new IllegalArgumentException("Can't define a targetValue with no target");
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
    evaluateCorrectArguments();

    String result = expressionManager.parseLogTemplate(content, event, getLocation(), NULL_BINDING_CONTEXT);
    Message.Builder messageBuilder = Message.builder(event.getMessage()).value(result).nullAttributesValue();
    MediaType configuredMediaType = buildMediaType();
    if (configuredMediaType != null) {
      messageBuilder.mediaType(configuredMediaType);
    }
    Message resultMessage = messageBuilder.build();

    if (target == null) {
      return CoreEvent.builder(event).message(resultMessage).build();
    } else {
      if (KEEP_TYPE_TARGET_AND_TARGET_VAR) {
        CoreEvent resultEvent = CoreEvent.builder(event).message(resultMessage).build();
        return outputToTarget(event, resultEvent, target, targetValueExpression, expressionManager);
      } else {
        return CoreEvent.builder(event).addVariable(target,
                                                    expressionManager
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

  public void setOutputMimeType(String outputMimeType) {
    this.outputMimeType = parseDefinedInApp(outputMimeType);
  }

  public void setOutputEncoding(String encoding) {
    this.outputEncoding = forName(encoding);
  }

  @Inject
  public void setExpressionManager(ExtendedExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }
}
