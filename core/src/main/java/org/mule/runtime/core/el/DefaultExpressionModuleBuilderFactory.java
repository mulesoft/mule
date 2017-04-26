/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;


import org.mule.runtime.api.el.AbstractExpressionModuleBuilderFactory;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.el.ModuleNamespace;

public class DefaultExpressionModuleBuilderFactory extends AbstractExpressionModuleBuilderFactory {

  @Override
  protected ExpressionModule.Builder create(ModuleNamespace moduleNamespace) {
    return new DefaultExpressionModuleBuilder(moduleNamespace);
  }
}
