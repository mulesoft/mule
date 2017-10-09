/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.expression;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.transformUnexpectedType;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;

import javax.inject.Inject;

/**
 * TODO
 */
public class ExpressionArgument extends AbstractComponent {

  private String expression;
  private String name;
  private boolean optional;
  private Class<?> returnClass;
  protected ClassLoader expressionEvaluationClassLoader = ExpressionArgument.class.getClassLoader();

  @Inject
  private MuleContext muleContext;

  public ExpressionArgument() {
    super();
  }

  public ExpressionArgument(String name, String expression, boolean optional) {
    this(name, expression, optional, null);
  }

  public ExpressionArgument(String name, String expression, boolean optional, Class<?> returnClass) {
    this.expression = expression;
    this.name = name;
    this.optional = optional;
    this.returnClass = returnClass;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isOptional() {
    return optional;
  }

  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  protected void validate() {
    if (expression == null) {
      throw new IllegalArgumentException(objectIsNull("expression").getMessage());
    }
  }

  /**
   * Evaluates this Expression against the passed in Message. If a returnClass is set on this Expression Argument it will be
   * checked to ensure the Argument returns the correct class type.
   *
   * @param event the event to execute the expression on
   * @return the result of the expression
   * @throws ExpressionRuntimeException if the wrong return type is returned from the expression.
   */
  public Object evaluate(CoreEvent event) throws ExpressionRuntimeException {

    // MULE-4797 Because there is no way to specify the class-loader that script
    // engines use and because scripts when used for expressions are compiled in
    // runtime rather than at initialization the only way to ensure the correct
    // class-loader to used is to switch it out here. We may want to consider
    // passing the class-loader to the MuleExpressionLanguage and only doing this for
    // certain ExpressionEvaluators further in.
    Object result =
        withContextClassLoader(expressionEvaluationClassLoader,
                               () -> muleContext.getExpressionManager().evaluate(getExpression(), event).getValue());

    if (getReturnClass() != null && result != null) {
      if (!getReturnClass().isInstance(result)) {
        // If the return type does not match, lets attempt to transform it before throwing an error
        try {
          Transformer t = ((MuleContextWithRegistries) muleContext).getRegistry()
              .lookupTransformer(DataType.fromObject(result), DataType.fromType(getReturnClass()));
          result = t.transform(result);
        } catch (TransformerException e) {
          throw new ExpressionRuntimeException(transformUnexpectedType(result.getClass(), getReturnClass()), e);
        }

      }
      // if(result instanceof Collection && ((Collection)result).size()==0 && !isOptional())
      // {
      // throw new ExpressionRuntimeException(CoreMessages.expressionEvaluatorReturnedNull(this.getEvaluator(),
      // this.getExpression()));
      // }
    }
    return result;
  }

  public String getExpression() {
    return expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public Class<?> getReturnClass() {
    return returnClass;
  }

  public void setReturnDataType(Class<?> returnClass) {
    this.returnClass = returnClass;
  }

  public void setExpressionEvaluationClassLoader(ClassLoader expressionEvaluationClassLoader) {
    this.expressionEvaluationClassLoader = expressionEvaluationClassLoader;
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
