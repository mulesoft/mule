/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.expression;

import static java.lang.Thread.currentThread;
import static java.util.Objects.hash;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.AbstractMessageTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * This transformer will evaluate one or more expressions on the current message and return the results as an Array. If only one
 * expression is defined it will return the object returned from the expression.
 * <p/>
 * You can use expressions to extract
 * <ul>
 * <li>headers (single, map or list)</li>
 * <li>attachments (single, map or list)</li>
 * <li>payload</li>
 * <li>xpath</li>
 * <li>groovy</li>
 * <li>bean</li>
 * </ul>
 * and more.
 * <p/>
 * This transformer provides a very powerful way to pull different bits of information from the message and pass them to the
 * service.
 */
public abstract class AbstractExpressionTransformer extends AbstractMessageTransformer {

  protected List<ExpressionArgument> arguments;

  public AbstractExpressionTransformer() {
    // No type checking by default
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
    arguments = new ArrayList<>(4);
  }

  public void addArgument(ExpressionArgument argument) {
    arguments.add(argument);
  }

  public boolean removeArgument(ExpressionArgument argument) {
    return arguments.remove(argument);
  }

  /**
   * Template method were deriving classes can do any initialisation after the properties have been set on this transformer
   *
   * @throws InitialisationException
   *
   */
  @Override
  public void initialise() throws InitialisationException {
    if (arguments == null || arguments.size() == 0) {
      throw new InitialisationException(objectIsNull("arguments[]"), this);
    }

    for (ExpressionArgument argument : arguments) {
      argument.setMuleContext(muleContext);
      argument.setExpressionEvaluationClassLoader(currentThread().getContextClassLoader());
      try {
        argument.validate();
      } catch (Exception e) {
        throw new InitialisationException(e, this);
      }
    }
  }

  public List<ExpressionArgument> getArguments() {
    return arguments;
  }

  public void setArguments(List<ExpressionArgument> arguments) {
    this.arguments = arguments;
  }

  @Override
  public int hashCode() {
    return hash(super.hashCode(), getArguments());
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj) && this.getArguments().equals(((AbstractExpressionTransformer) obj).getArguments());
  }

}
