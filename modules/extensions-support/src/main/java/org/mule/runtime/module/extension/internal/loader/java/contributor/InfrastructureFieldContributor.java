/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.InfrastructureTypeMapping;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;

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
    InfrastructureTypeMapping.InfrastructureType infrastructureType =
        InfrastructureTypeMapping.getMap().get(parameter.getType().getDeclaringClass());
    if (infrastructureType != null && !isBlank(infrastructureType.getName())) {
      declarer.withModelProperty(new InfrastructureParameterModelProperty(infrastructureType.getSequence()));
      declarer.withExpressionSupport(NOT_SUPPORTED);
      InfrastructureTypeMapping.getQName(infrastructureType.getName()).ifPresent(declarer::withModelProperty);
      InfrastructureTypeMapping.getDslConfiguration(infrastructureType.getName()).ifPresent(declarer::withDsl);
    }
  }
}

