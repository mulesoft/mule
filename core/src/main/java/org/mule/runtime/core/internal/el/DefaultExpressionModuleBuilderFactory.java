/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el;


import org.mule.runtime.api.el.AbstractExpressionModuleBuilderFactory;
import org.mule.runtime.api.el.ExpressionModule;
import org.mule.runtime.api.el.ModuleNamespace;

public class DefaultExpressionModuleBuilderFactory extends AbstractExpressionModuleBuilderFactory {

  @Override
  protected ExpressionModule.Builder create(ModuleNamespace moduleNamespace) {
    return new DefaultExpressionModuleBuilder(moduleNamespace);
  }
}
