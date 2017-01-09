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
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.DefaultValidationResult;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionLanguage;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.el.GlobalBindingContextProvider;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.el.v2.DataWeaveExpressionLanguage;

import org.mule.runtime.core.util.TemplateParser;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.slf4j.Logger;

public class DefaultExpressionManager implements ExtendedExpressionManager, Initialisable {

  public static final String DW_PREFIX = "dw:";
  private static final Logger logger = getLogger(DefaultExpressionManager.class);

  private MuleContext muleContext;
  private ExtendedExpressionLanguage expressionLanguage;
  // Default style parser
  private TemplateParser parser = TemplateParser.createMuleStyleParser();

  @Inject
  public DefaultExpressionManager(MuleContext muleContext) {
    this.muleContext = muleContext;
    DataWeaveExpressionLanguage dataWeaveExpressionLanguage =
        new DataWeaveExpressionLanguage(muleContext.getExecutionClassLoader());
    MVELExpressionLanguage mvelExpressionLanguage = muleContext.getRegistry().lookupObject(OBJECT_EXPRESSION_LANGUAGE);
    this.expressionLanguage = new ExtendedExpressionLanguageAdapter(dataWeaveExpressionLanguage, mvelExpressionLanguage);
  }

  @Override
  public void initialise() throws InitialisationException {
    Collection<GlobalBindingContextProvider> contextProviders =
        muleContext.getRegistry().lookupObjects(GlobalBindingContextProvider.class);
    for (GlobalBindingContextProvider contextProvider : contextProviders) {
      expressionLanguage.registerGlobalContext(contextProvider.getBindingContext());
    }
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

  @Override
  public TypedValue evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                             BindingContext context) {
    return expressionLanguage.evaluate(expression, event, eventBuilder, flowConstruct, context);
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
    TypedValue result = evaluate(expression, event, null, null, context);
    DataType sourceType = result.getDataType();
    try {
      return transform(result, sourceType, outputType);
    } catch (TransformerException e) {
      throw new ExpressionRuntimeException(createStaticMessage(
                                                               format("Failed to apply implicit transformation from type %s to %s",
                                                                      sourceType, outputType),
                                                               e));
    }
  }

  private TypedValue transform(TypedValue target, DataType sourceType, DataType outputType) throws TransformerException {
    Object result = muleContext.getRegistry().lookupTransformer(sourceType, outputType).transform(target.getValue());
    return new TypedValue(result, outputType);
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct, Object object) {
    expressionLanguage.enrich(expression, event, eventBuilder, flowConstruct, object);
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
    return resolveBoolean(evaluate(expression, event, flowConstruct).getValue(), nullReturnsTrue, nonBooleanReturnsTrue,
                          expression);
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
        logger.warn("Expression: " + expression + ", returned an non-boolean result. Returning: " + nonBooleanReturnsTrue);
        return nonBooleanReturnsTrue;
      }
    }
  }

  @Override
  public String parse(String expression, Event event, FlowConstruct flowConstruct) throws ExpressionRuntimeException {
    return parse(expression, event, Event.builder(event), flowConstruct);
  }

  @Override
  public String parse(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct)
      throws ExpressionRuntimeException {
    logger.warn("Expression parsing is deprecated, regular evaluations should be used instead.");
    if (isDwExpression(expression)) {
      TypedValue evaluation = evaluate(expression, event, eventBuilder, flowConstruct);
      try {
        return (String) transform(evaluation, evaluation.getDataType(), STRING).getValue();
      } catch (TransformerException e) {
        throw new ExpressionRuntimeException(createStaticMessage(format("Failed to transform %s to %s.", evaluation.getDataType(),
                                                                        STRING)),
                                             e);
      }
    } else {
      return parser.parse(token -> {
        Object result = evaluate(token, event, eventBuilder, flowConstruct).getValue();
        if (result instanceof InternalMessage) {
          return ((InternalMessage) result).getPayload().getValue();
        } else {
          return result;
        }
      }, expression);
    }
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
      if (logger.isDebugEnabled()) {
        logger.debug("Validate expressions is turned off, no checking done for: " + expression);
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

  protected static boolean isDwExpression(String expression) {
    //TODO: MULE-10410 - Remove once DW is the default language.
    return expression.startsWith(DEFAULT_EXPRESSION_PREFIX + DW_PREFIX) || expression.startsWith(DW_PREFIX);
  }

}
