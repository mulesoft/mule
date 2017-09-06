/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.el;


import org.mule.runtime.api.el.AbstractExpressionModuleBuilderFactory;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.el.ModuleNamespace;
import org.mule.runtime.core.internal.el.DefaultExpressionModuleBuilder;

public class DefaultExpressionModuleBuilderFactory extends AbstractExpressionModuleBuilderFactory {

  @Override
  protected ExpressionModule.Builder create(ModuleNamespace moduleNamespace) {
    return new DefaultExpressionModuleBuilder(moduleNamespace);
  }
}
