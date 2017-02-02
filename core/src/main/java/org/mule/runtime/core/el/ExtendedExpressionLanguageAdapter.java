/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_MEL_AS_DEFAULT;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.el.DefaultExpressionManager.MEL_PREFIX;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionLanguage;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.el.mvel.MVELExpressionLanguage;

/**
 * Implementation of an {@link ExtendedExpressionLanguage} which adapts MVEL and DW together, deciding via a prefix whether one or
 * the other should be call. It will allow MVEL and DW to be used together in compatibility mode.
 *
 * @since 4.0
 */
public class ExtendedExpressionLanguageAdapter implements ExtendedExpressionLanguage {

  //DW based expression language
  private DataWeaveExpressionLanguage dataWeaveExpressionLanguage;
  //MVEL based expression language
  private MVELExpressionLanguage mvelExpressionLanguage;

  private boolean forceMel = false;

  public ExtendedExpressionLanguageAdapter(DataWeaveExpressionLanguage dataWeaveExpressionLanguage,
                                           MVELExpressionLanguage mvelExpressionLanguage) {
    this.dataWeaveExpressionLanguage = dataWeaveExpressionLanguage;
    this.mvelExpressionLanguage = mvelExpressionLanguage;
    forceMel = valueOf(getProperty(MULE_MEL_AS_DEFAULT, "false")) || !dataWeaveExpressionLanguage.isEnabled();
  }

  public boolean isForceMel() {
    return forceMel;
  }

  @Override
  public void registerGlobalContext(BindingContext bindingContext) {
    dataWeaveExpressionLanguage.registerGlobalContext(bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, BindingContext context)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).evaluate(expression, event, context);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).evaluate(expression, event, flowConstruct, bindingContext);
  }

  @Override
  public ValidationResult validate(String expression) {
    return selectExpressionLanguage(expression).validate(expression);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                             BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return selectExpressionLanguage(expression).evaluate(expression, event, eventBuilder, flowConstruct, bindingContext);
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct, Object object) {
    selectExpressionLanguage(expression).enrich(expression, event, eventBuilder, flowConstruct, object);
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                     TypedValue value) {
    selectExpressionLanguage(expression).enrich(expression, event, eventBuilder, flowConstruct, value);
  }

  private ExtendedExpressionLanguage selectExpressionLanguage(String expression) {
    if (isMelExpression(expression) || forceMel) {
      return mvelExpressionLanguage;
    } else {
      return dataWeaveExpressionLanguage;
    }
  }

  protected boolean isMelExpression(String expression) {
    return expression.startsWith(DEFAULT_EXPRESSION_PREFIX + MEL_PREFIX) || expression.startsWith(MEL_PREFIX);
  }
}
