/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.String.format;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.ValidationResult.failure;
import static org.mule.runtime.api.el.ValidationResult.success;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.util.ClassUtils.isInstance;
import static org.mule.runtime.core.api.util.StreamingUtils.updateTypedValueForStreaming;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultValidationResult;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.util.OneTimeWarning;
import org.mule.runtime.core.privileged.el.GlobalBindingContextProvider;
import org.mule.runtime.core.privileged.util.TemplateParser;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.slf4j.Logger;

public class DefaultExpressionManager implements ExtendedExpressionManager, Initialisable {

  public static final String DW_PREFIX = "dw";
  public static final String MEL_PREFIX = "mel";
  public static final String PREFIX_EXPR_SEPARATOR = ":";
  public static final int DW_PREFIX_LENGTH = (DW_PREFIX + PREFIX_EXPR_SEPARATOR).length();
  private static final Logger LOGGER = getLogger(DefaultExpressionManager.class);
  protected static final String COMPATILIBILITY_PLUGIN_INSTALLED = "_compatilibilityPluginInstalled";

  private final OneTimeWarning parseWarning = new OneTimeWarning(LOGGER,
                                                                 "Expression parsing is deprecated, regular evaluations should be used instead.");

  private AtomicBoolean initialized = new AtomicBoolean();

  private MuleContext muleContext;
  private StreamingManager streamingManager;
  private Registry registry;

  private ExtendedExpressionLanguageAdaptor expressionLanguage;
  // Default style parser
  private final TemplateParser parser = TemplateParser.createMuleStyleParser();
  private boolean melDefault;

  @Override
  public void initialise() throws InitialisationException {
    if (!initialized.getAndSet(true)) {
      final DataWeaveExpressionLanguageAdaptor dwExpressionLanguage =
          DataWeaveExpressionLanguageAdaptor.create(muleContext, registry);

      MVELExpressionLanguage mvelExpressionLanguage = null;
      if (registry.lookupByName(COMPATILIBILITY_PLUGIN_INSTALLED).isPresent()) {
        mvelExpressionLanguage = registry.<MVELExpressionLanguage>lookupByName(OBJECT_EXPRESSION_LANGUAGE).get();
      }
      this.expressionLanguage = new ExpressionLanguageAdaptorHandler(dwExpressionLanguage, mvelExpressionLanguage);
      this.melDefault = ((ExpressionLanguageAdaptorHandler) expressionLanguage).isMelDefault();

      Collection<GlobalBindingContextProvider> contextProviders = registry.lookupAllByType(GlobalBindingContextProvider.class);

      contextProviders.stream()
          .map(GlobalBindingContextProvider::getBindingContext)
          .forEach(expressionLanguage::addGlobalBindings);

      if (melDefault) {
        LOGGER.warn("Using MEL as the default expression language.");
      }
    }
  }

  @Override
  public void addGlobalBindings(BindingContext bindingContext) {
    expressionLanguage.addGlobalBindings(bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression) {
    return evaluate(expression, NULL_BINDING_CONTEXT);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event) {
    return evaluate(expression, event, NULL_BINDING_CONTEXT);
  }

  @Override
  public TypedValue evaluate(String expression, BindingContext context) {
    return evaluate(expression, null, null, null, context);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, BindingContext context) {
    return evaluate(expression, event, CoreEvent.builder(event), null, context);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation) {
    return evaluate(expression, event, CoreEvent.builder(event), componentLocation, NULL_BINDING_CONTEXT);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                             ComponentLocation componentLocation) {
    return evaluate(expression, event, eventBuilder, componentLocation, NULL_BINDING_CONTEXT);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation,
                             BindingContext context) {
    return evaluate(expression, event, CoreEvent.builder(event), componentLocation, context);
  }

  private TypedValue evaluate(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                              ComponentLocation componentLocation,
                              BindingContext context) {
    return updateTypedValueForStreaming(expressionLanguage.evaluate(expression, event, eventBuilder, componentLocation, context),
                                        event, streamingManager);
  }

  @Override
  public TypedValue evaluate(String expression, DataType outputType) {
    return evaluate(expression, outputType, NULL_BINDING_CONTEXT);
  }

  @Override
  public TypedValue evaluate(String expression, DataType outputType, BindingContext context) {
    return evaluate(expression, outputType, context, null);
  }

  @Override
  public TypedValue evaluate(String expression, DataType outputType, BindingContext context, CoreEvent event) {
    return evaluate(expression, outputType, context, event, null, false);
  }

  @Override
  public TypedValue evaluate(String expression, DataType outputType, BindingContext context, CoreEvent event,
                             ComponentLocation componentLocation, boolean failOnNull)
      throws ExpressionRuntimeException {
    return updateTypedValueForStreaming(expressionLanguage.evaluate(expression, outputType, event, componentLocation, context,
                                                                    failOnNull),
                                        event, streamingManager);
  }

  private TypedValue transform(TypedValue target, DataType sourceType, DataType outputType) throws TransformerException {
    if (target.getValue() != null && !isInstance(outputType.getType(), target.getValue())) {
      Object result = ((MuleContextWithRegistries) muleContext).getRegistry().lookupTransformer(sourceType, outputType)
          .transform(target.getValue());
      return new TypedValue<>(result, outputType);
    } else {
      return target;
    }
  }

  @Override
  public void enrich(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                     ComponentLocation componentLocation,
                     TypedValue value) {
    expressionLanguage.enrich(expression, event, eventBuilder, componentLocation, value);
  }

  @Override
  public boolean evaluateBoolean(String expression, CoreEvent event, ComponentLocation componentLocation)
      throws ExpressionRuntimeException {
    return evaluateBoolean(expression, event, componentLocation, false, false);
  }

  @Override
  public boolean evaluateBoolean(String expression, CoreEvent event, ComponentLocation componentLocation,
                                 boolean nullReturnsTrue,
                                 boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException {
    return resolveBoolean(evaluate(expression, DataType.BOOLEAN, NULL_BINDING_CONTEXT, event, componentLocation, false)
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
  public String parse(String expression, CoreEvent event, ComponentLocation componentLocation)
      throws ExpressionRuntimeException {
    Builder eventBuilder = CoreEvent.builder(event);
    parseWarning.warn();
    if (hasMelExpression(expression) || melDefault) {
      return parser.parse(token -> {
        Object result = evaluate(token, event, eventBuilder, componentLocation).getValue();
        if (result instanceof Message) {
          return ((Message) result).getPayload().getValue();
        } else {
          return result;
        }
      }, expression);
    } else if (isExpression(expression)) {
      TypedValue evaluation = evaluate(expression, event, eventBuilder, componentLocation);
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
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, ComponentLocation componentLocation,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return expressionLanguage.split(expression, event, componentLocation, bindingContext);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, BindingContext bindingContext)
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

  @Inject
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Inject
  public void setStreamingManager(StreamingManager streamingManager) {
    this.streamingManager = streamingManager;
  }

  @Inject
  public void setRegistry(Registry registry) {
    this.registry = registry;
  }
}
