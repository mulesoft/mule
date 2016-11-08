/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import static java.util.Arrays.asList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.expression.ExpressionConfig;
import org.mule.runtime.core.expression.transformers.ExpressionArgument;
import org.mule.runtime.core.expression.transformers.ExpressionTransformer;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.dsl.api.component.ObjectFactory;

import java.util.List;

/**
 * {@link ObjectFactory} that constructs {@link ExpressionTransformer} from the mule
 * configuration.
 *
 * @since 4.0
 */
public class ExpressionTransformerObjectFactory extends TransformerObjectFactory implements Initialisable {

  private boolean returnSourceIfNull = false;
  private String expression;
  protected List<ExpressionArgument> arguments;

  @Override
  protected AbstractTransformer createInstance() {
    ExpressionTransformer expressionTransformer = new ExpressionTransformer();
    expressionTransformer.setReturnSourceIfNull(returnSourceIfNull);
    expressionTransformer.setArguments(arguments);
    return expressionTransformer;
  }

  /**
   * @param returnSourceIfNull if true and all arguments are null then the input message will be return, otherwise the results
   *        will be return even if they are null.
   */
  public void setReturnSourceIfNull(boolean returnSourceIfNull) {
    this.returnSourceIfNull = returnSourceIfNull;
  }

  /**
   * @param expression the expression to retrieve the argument value.
   */
  public void setExpression(String expression) {
    this.expression = expression;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (expression != null && arguments != null) {
      throw new InitialisationException(createStaticMessage("Expression transformer do not support expression attribute or return-data child element at the same time."),
                                        this);
    }
    if (expression != null) {
      this.arguments = asList(new ExpressionArgument("single", new ExpressionConfig(expression), false));
    }
  }
}
