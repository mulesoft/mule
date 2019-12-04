/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.collection.SmallMap.of;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.DW_PREFIX;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.MEL_PREFIX;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.benmanes.caffeine.cache.LoadingCache;

/**
 * Implementation of an {@link ExtendedExpressionLanguageAdaptor} which adapts MVEL and DW together, deciding via a prefix whether
 * one or the other should be call. It will allow MVEL and DW to be used together in compatibility mode.
 *
 * @since 4.0
 */
public class ExpressionLanguageAdaptorHandler implements ExtendedExpressionLanguageAdaptor {

  private static final String EXPR_PREFIX_LANGS_TOKEN = "LANGS";
  private static final String EXPR_PREFIX_PATTERN_TEMPLATE =
      "^\\s*(?:(?:#\\[)?\\s*(" + EXPR_PREFIX_LANGS_TOKEN + "):|\\%(" + EXPR_PREFIX_LANGS_TOKEN + ") \\d).*";
  static final String MVEL_NOT_INSTALLED_ERROR = "MVEL expression language configured as default but not installed";

  private final Pattern exprPrefixPattern;
  private Map<String, ExtendedExpressionLanguageAdaptor> expressionLanguages;
  private LoadingCache<String, ExtendedExpressionLanguageAdaptor> expressionLanguagesByExpressionCache =
      newBuilder().build(expression -> {
        final String languagePrefix = resolveLanguagePrefix(expression);
        ExtendedExpressionLanguageAdaptor extendedExpressionLanguageAdaptor = expressionLanguages.get(languagePrefix);
        if (extendedExpressionLanguageAdaptor == null) {
          throw new IllegalStateException(format("There is no expression language registered for '%s'", languagePrefix));
        }
        return extendedExpressionLanguageAdaptor;
      });

  private boolean melDefault = false;

  public ExpressionLanguageAdaptorHandler(ExtendedExpressionLanguageAdaptor defaultExtendedExpressionLanguage,
                                          ExtendedExpressionLanguageAdaptor mvelExpressionLanguage) {
    expressionLanguages = of(DW_PREFIX, defaultExtendedExpressionLanguage);
    if (mvelExpressionLanguage != null) {
      expressionLanguages.put(MEL_PREFIX, mvelExpressionLanguage);
    }

    exprPrefixPattern = compile(EXPR_PREFIX_PATTERN_TEMPLATE.replaceAll("LANGS", join(expressionLanguages.keySet(), '|')));

    melDefault = MuleProperties.isMelDefault();
    if (isMelDefault() && mvelExpressionLanguage == null) {
      throw new IllegalStateException(MVEL_NOT_INSTALLED_ERROR);
    }
  }

  boolean isMelDefault() {
    return melDefault;
  }

  @Override
  public void addGlobalBindings(BindingContext bindingContext) {
    expressionLanguages.get(DW_PREFIX).addGlobalBindings(bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, BindingContext context)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).evaluate(expression, event, context);
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event, BindingContext context)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).evaluate(expression, expectedOutputType, event, context);
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event,
                             ComponentLocation componentLocation,
                             BindingContext context, boolean failOnNull)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).evaluate(expression, expectedOutputType, event, componentLocation, context,
                                                         failOnNull);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation,
                             BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).evaluate(expression, event, componentLocation, bindingContext);
  }

  @Override
  public TypedValue<?> evaluateLogExpression(String expression, CoreEvent event, ComponentLocation componentLocation,
                                             BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).evaluateLogExpression(expression, event, componentLocation, bindingContext);
  }

  @Override
  public ValidationResult validate(String expression) {
    return selectExpressionLanguage(expression).validate(expression);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                             ComponentLocation componentLocation,
                             BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).evaluate(expression, event, eventBuilder, componentLocation, bindingContext);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, ComponentLocation componentLocation,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).split(expression, event, componentLocation, bindingContext);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).split(expression, event, bindingContext);
  }

  private ExtendedExpressionLanguageAdaptor selectExpressionLanguage(String expression) {
    return expressionLanguagesByExpressionCache.get(expression);
  }

  public String getLanguagePrefix(String expression) {
    final Matcher matcher = exprPrefixPattern.matcher(expression);
    if (matcher.find()) {
      int i = 1;
      String currentGroup = null;
      while (currentGroup == null) {
        currentGroup = matcher.group(i++);
      }
      return currentGroup;
    } else {
      return null;
    }
  }

  @Override
  public ExpressionLanguageSessionAdaptor openSession(ComponentLocation componentLocation, CoreEvent event,
                                                      BindingContext bindingContext) {
    Map<String, ExpressionLanguageSessionAdaptor> sessions = new SmallMap<>();
    for (Entry<String, ExtendedExpressionLanguageAdaptor> exprLangEntry : expressionLanguages.entrySet()) {
      if (!MEL_PREFIX.equals(exprLangEntry.getKey()) || event != null) {
        sessions.put(exprLangEntry.getKey(), exprLangEntry.getValue().openSession(componentLocation, event, bindingContext));
      }
    }

    return new ExpressionLanguageSessionAdaptor() {

      @Override
      public TypedValue<?> evaluate(String expression, DataType expectedOutputType) throws ExpressionRuntimeException {
        return resolveSessionForLanguage(sessions, expression).evaluate(expression, expectedOutputType);
      }

      @Override
      public TypedValue<?> evaluate(String expression) throws ExpressionRuntimeException {
        return resolveSessionForLanguage(sessions, expression).evaluate(expression);
      }

      @Override
      public TypedValue<?> evaluate(String expression, long timeout) throws ExpressionRuntimeException {
        return resolveSessionForLanguage(sessions, expression).evaluate(expression, timeout);
      }

      @Override
      public TypedValue<?> evaluateLogExpression(String expression) throws ExpressionRuntimeException {
        return resolveSessionForLanguage(sessions, expression).evaluateLogExpression(expression);
      }

      @Override
      public Iterator<TypedValue<?>> split(String expression) {
        return resolveSessionForLanguage(sessions, expression).split(expression);
      }

      @Override
      public TypedValue<?> evaluate(CompiledExpression expression) throws ExpressionExecutionException {
        return sessions.get(DW_PREFIX).evaluate(expression);
      }

      @Override
      public TypedValue<?> evaluate(CompiledExpression expression, DataType expectedOutputType)
          throws ExpressionExecutionException {
        return sessions.get(DW_PREFIX).evaluate(expression, expectedOutputType);
      }

      @Override
      public TypedValue<?> evaluate(CompiledExpression expression, long timeout) throws ExpressionExecutionException {
        return sessions.get(DW_PREFIX).evaluate(expression, timeout);
      }

      @Override
      public TypedValue<?> evaluateLogExpression(CompiledExpression expression) throws ExpressionExecutionException {
        return sessions.get(DW_PREFIX).evaluateLogExpression(expression);
      }

      @Override
      public Iterator<TypedValue<?>> split(CompiledExpression expression) {
        return sessions.get(DW_PREFIX).split(expression);
      }

      protected ExpressionLanguageSessionAdaptor resolveSessionForLanguage(Map<String, ExpressionLanguageSessionAdaptor> sessions,
                                                                           String expression) {
        ExpressionLanguageSessionAdaptor elSession = sessions.get(resolveLanguagePrefix(expression));
        if (elSession == null) {
          throw new ExpressionRuntimeException(createStaticMessage("This sessions was not created with an event, so '"
              + resolveLanguagePrefix(expression) + "' languge cannot be used in this session."));
        }
        return elSession;
      }

      @Override
      public void close() {
        sessions.values().forEach(es -> es.close());
      }

    };
  }

  private String resolveLanguagePrefix(String expression) {
    final String languagePrefix = getLanguagePrefix(expression);
    if (isEmpty(languagePrefix)) {
      if (isMelDefault()) {
        return MEL_PREFIX;
      } else {
        return DW_PREFIX;
      }
    } else if (expressionLanguages.size() == 1) {
      return expressionLanguages.keySet().iterator().next();
    } else {
      return languagePrefix;
    }
  }

  @Override
  public String toString() {
    return this.getClass().getName() + "[" + expressionLanguages.toString() + "]";
  }
}
