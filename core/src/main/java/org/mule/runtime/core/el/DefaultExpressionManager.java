/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.lang.String.format;
import static org.mule.runtime.api.el.ValidationResult.failure;
import static org.mule.runtime.api.el.ValidationResult.success;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.util.ClassUtils.isInstance;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultValidationResult;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.el.GlobalBindingContextProvider;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.streaming.StreamingManager;
import org.mule.runtime.core.util.OneTimeWarning;
import org.mule.runtime.core.util.TemplateParser;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.slf4j.Logger;

public class DefaultExpressionManager implements ExtendedExpressionManager, Initialisable {

  public static final String DW_PREFIX = "dw";
  public static final String MEL_PREFIX = "mel";
  public static final String PREFIX_EXPR_SEPARATOR = ":";
  private static final Logger LOGGER = getLogger(DefaultExpressionManager.class);

  private final OneTimeWarning parseWarning = new OneTimeWarning(LOGGER,
                                                                 "Expression parsing is deprecated, regular evaluations should be used instead.");

  private final MuleContext muleContext;
  private final StreamingManager streamingManager;
  private final ExtendedExpressionLanguageAdaptor expressionLanguage;
  // Default style parser
  private final TemplateParser parser = TemplateParser.createMuleStyleParser();
  private final boolean melDefault;


  @Inject
  public DefaultExpressionManager(MuleContext muleContext, StreamingManager streamingManager) {
    this.muleContext = muleContext;
    this.streamingManager = streamingManager;
    final DataWeaveExpressionLanguageAdaptor dwExpressionLanguage = new DataWeaveExpressionLanguageAdaptor(muleContext);
    final MVELExpressionLanguage mvelExpressionLanguage = muleContext.getRegistry().lookupObject(OBJECT_EXPRESSION_LANGUAGE);
    this.expressionLanguage = new ExpressionLanguageAdaptorHandler(dwExpressionLanguage, mvelExpressionLanguage);
    this.melDefault = ((ExpressionLanguageAdaptorHandler) expressionLanguage).isMelDefault();
  }

  @Override
  public void initialise() throws InitialisationException {
    Collection<GlobalBindingContextProvider> contextProviders =
        muleContext.getRegistry().lookupObjects(GlobalBindingContextProvider.class);
    for (GlobalBindingContextProvider contextProvider : contextProviders) {
      expressionLanguage.addGlobalBindings(contextProvider.getBindingContext());
    }
    if (melDefault) {
      LOGGER.warn("Using MEL as the default expression language.");
    }
  }

  @Override
  public void addGlobalBindings(BindingContext bindingContext) {
    expressionLanguage.addGlobalBindings(bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression) {
    return evaluate(expression, BindingContext.builder().build());
  }

  @Override
  public TypedValue evaluate(String expression, Event event) {
    return evaluate(expression, event, BindingContext.builder().build());
  }

  @Override
  public TypedValue evaluate(String expression, BindingContext context) {
    return evaluate(expression, null, null, null, context);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, BindingContext context) {
    return evaluate(expression, event, Event.builder(event), null, context);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct) {
    return evaluate(expression, event, Event.builder(event), flowConstruct, BindingContext.builder().build());
  }

  @Override
  public TypedValue evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct) {
    return evaluate(expression, event, eventBuilder, flowConstruct, BindingContext.builder().build());
  }

  @Override
  public TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct, BindingContext context) {
    return evaluate(expression, event, Event.builder(event), flowConstruct, context);
  }

  private TypedValue evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                              BindingContext context) {
    return handleStreaming(expressionLanguage.evaluate(expression, event, eventBuilder, flowConstruct, context), event);
  }

  @Override
  public TypedValue evaluate(String expression, DataType outputType) {
    return evaluate(expression, outputType, BindingContext.builder().build());
  }

  @Override
  public TypedValue evaluate(String expression, DataType outputType, BindingContext context) {
    return evaluate(expression, outputType, context, null);
  }

  @Override
  public TypedValue evaluate(String expression, DataType outputType, BindingContext context, Event event) {
    return evaluate(expression, outputType, context, event, null, false);
  }

  @Override
  public TypedValue evaluate(String expression, DataType outputType, BindingContext context, Event event,
                             FlowConstruct flowConstruct, boolean failOnNull)
      throws ExpressionRuntimeException {
    return handleStreaming(expressionLanguage.evaluate(expression, outputType, event, flowConstruct, context, failOnNull), event);
  }

  private TypedValue handleStreaming(TypedValue value, Event event) {
    //TODO required a better fix for MULE-12486
    if (event == null) {
      return value;
    }

    Object payload = value.getValue();
    if (payload instanceof CursorProvider) {
      value = new TypedValue<>(streamingManager.manage((CursorProvider) payload, event), value.getDataType());
    }

    return value;
  }

  private TypedValue transform(TypedValue target, DataType sourceType, DataType outputType) throws TransformerException {
    if (target.getValue() != null && !isInstance(outputType.getType(), target.getValue())) {
      Object result = muleContext.getRegistry().lookupTransformer(sourceType, outputType).transform(target.getValue());
      return new TypedValue<>(result, outputType);
    } else {
      return target;
    }
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct, TypedValue value) {
    expressionLanguage.enrich(expression, event, eventBuilder, flowConstruct, value);
  }

  @Override
  public boolean evaluateBoolean(String expression, Event event, FlowConstruct flowConstruct)
      throws ExpressionRuntimeException {
    return evaluateBoolean(expression, event, flowConstruct, false, false);
  }

  @Override
  public boolean evaluateBoolean(String expression, Event event, FlowConstruct flowConstruct, boolean nullReturnsTrue,
                                 boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException {
    return resolveBoolean(evaluate(expression, DataType.BOOLEAN, BindingContext.builder().build(), event, flowConstruct, false)
        .getValue(), nullReturnsTrue,
                          nonBooleanReturnsTrue, expression);
  }

  protected boolean resolveBoolean(Object result, boolean nullReturnsTrue, boolean nonBooleanReturnsTrue, String expression) {
    if (result == null) {
      return nullReturnsTrue;
    } else {
      Object value = result;
      if (value instanceof Boolean) {
        return (Boolean) value;
      } else if (value instanceof String) {
        if (value.toString().toLowerCase().equalsIgnoreCase("false")) {
          return false;
        } else if (result.toString().toLowerCase().equalsIgnoreCase("true")) {
          return true;
        } else {
          return nonBooleanReturnsTrue;
        }
      } else {
        LOGGER.warn("Expression: " + expression + ", returned an non-boolean result. Returning: " + nonBooleanReturnsTrue);
        return nonBooleanReturnsTrue;
      }
    }
  }

  @Override
  public String parse(String expression, Event event, FlowConstruct flowConstruct) throws ExpressionRuntimeException {
    Builder eventBuilder = Event.builder(event);
    parseWarning.warn();
    if (hasMelExpression(expression) || melDefault) {
      return parser.parse(token -> {
        Object result = evaluate(token, event, eventBuilder, flowConstruct).getValue();
        if (result instanceof Message) {
          return ((Message) result).getPayload().getValue();
        } else {
          return result;
        }
      }, expression);
    } else if (isExpression(expression)) {
      TypedValue evaluation = evaluate(expression, event, eventBuilder, flowConstruct);
      try {
        return (String) transform(evaluation, evaluation.getDataType(), STRING).getValue();
      } catch (TransformerException e) {
        throw new ExpressionRuntimeException(createStaticMessage(format("Failed to transform %s to %s.", evaluation.getDataType(),
                                                                        STRING)),
                                             e);
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(String.format("No expression marker found in expression '%s'. Parsing as plain String.", expression));
      }
      return expression;
    }
  }

  private String parse(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct)
      throws ExpressionRuntimeException {
    parseWarning.warn();
    if (hasMelExpression(expression) || melDefault) {
      return parser.parse(token -> {
        Object result = evaluate(token, event, eventBuilder, flowConstruct).getValue();
        if (result instanceof Message) {
          return ((Message) result).getPayload().getValue();
        } else {
          return result;
        }
      }, expression);
    } else if (isExpression(expression)) {
      TypedValue evaluation = evaluate(expression, event, eventBuilder, flowConstruct);
      try {
        return (String) transform(evaluation, evaluation.getDataType(), STRING).getValue();
      } catch (TransformerException e) {
        throw new ExpressionRuntimeException(createStaticMessage(format("Failed to transform %s to %s.", evaluation.getDataType(),
                                                                        STRING)),
                                             e);
      }
    } else {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(String.format("No expression marker found in expression '%s'. Parsing as plain String.", expression));
      }
      return expression;
    }
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, Event event, FlowConstruct flowConstruct,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return expressionLanguage.split(expression, event, flowConstruct, bindingContext);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, Event event, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return expressionLanguage.split(expression, event, bindingContext);
  }

  @Override
  public boolean isExpression(String expression) {
    return expression.contains(DEFAULT_EXPRESSION_PREFIX);
  }

  @Override
  public boolean isValid(String expression) {
    return validate(expression).isSuccess();
  }

  @Override
  public ValidationResult validate(String expression) {
    if (!muleContext.getConfiguration().isValidateExpressions()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Validate expressions is turned off, no checking done for: " + expression);
      }
      return new DefaultValidationResult(true, null);
    }
    final StringBuilder message = new StringBuilder();
    try {
      parser.validate(expression);
      final AtomicBoolean valid = new AtomicBoolean(true);
      if (expression.contains(DEFAULT_EXPRESSION_PREFIX)) {
        parser.parse(token -> {
          if (valid.get()) {
            ValidationResult result = expressionLanguage.validate(token);
            if (!result.isSuccess()) {
              valid.compareAndSet(true, false);
              message.append(token).append(" is invalid\n");
              message.append(result.errorMessage().orElse(""));
            }
          }
          return null;
        }, expression);
      } else {
        return expressionLanguage.validate(expression);
      }
    } catch (IllegalArgumentException e) {
      return failure(e.getMessage(), expression);
    }

    if (message.length() > 0) {
      return failure(message.toString());
    }
    return success();
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, BindingContext context) {
    return expressionLanguage.split(expression, null, context);
  }

  /**
   * Checks if an expression has MEL prefix.
   * 
   * @param expression the expression to check to see if is a MEL expression.
   * @return true if the expression is a MEL expression
   */
  public static boolean hasMelExpression(String expression) {
    return expression.contains(DEFAULT_EXPRESSION_PREFIX + MEL_PREFIX + PREFIX_EXPR_SEPARATOR);
  }
}
