/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.InfrastructureTypeMapping;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.extension.internal.property.InfrastructureParameterModelProperty;

/**
 * {@link ParameterDeclarerContributor} implementation which given a {@link ExtensionParameter} which their type
 * is one of the considered as an Infrastructure Type ({@link InfrastructureTypeMapping}) changes the
 * {@link ExpressionSupport} to {@link ExpressionSupport#NOT_SUPPORTED} and adds {@link InfrastructureParameterModelProperty}
 *
 * @since 4.0
 */
public class InfrastructureFieldContributor implements ParameterDeclarerContributor {

  /**
   * {@inheritDoc}
   */
  @Override
  public void contribute(ExtensionParameter parameter, ParameterDeclarer declarer,
                         ParameterDeclarationContext declarationContext) {
    if (InfrastructureTypeMapping.getMap().containsKey(parameter.getType().getDeclaringClass())) {
      declarer.withModelProperty(new InfrastructureParameterModelProperty());
      declarer.withExpressionSupport(NOT_SUPPORTED);
    }
  }
}

