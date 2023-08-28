/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel;

import org.mule.mvel2.ParserContext;
import org.mule.mvel2.ast.Function;
import org.mule.mvel2.integration.VariableResolverFactory;

@SuppressWarnings("serial")
class MVELFunctionAdaptor extends Function {

  private ExpressionLanguageFunction function;

  public MVELFunctionAdaptor(String name, ExpressionLanguageFunction function, ParserContext parserContext) {
    super(name, new char[] {}, 0, 0, 0, 0, 0, parserContext);
    this.function = function;
  }

  @Override
  public Object call(Object ctx, Object thisValue, VariableResolverFactory factory, Object[] parms) {
    while (!(factory instanceof ExpressionLanguageContext) && factory != null) {
      factory = factory.getNextFactory();
    }
    return function.call(parms, (ExpressionLanguageContext) factory);
  }

  @Override
  public void checkArgumentCount(int passing) {
    // no-op
  }
}
