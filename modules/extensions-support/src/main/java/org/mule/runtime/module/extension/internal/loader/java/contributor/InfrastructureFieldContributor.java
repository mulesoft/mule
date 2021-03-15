/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.util.collection.Collectors.toImmutableMap;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping.getDslConfiguration;
import static org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping.getQName;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping;
import org.mule.runtime.extension.internal.loader.util.InfrastructureTypeMapping.InfrastructureType;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

import java.util.Map;
import java.util.Optional;

/**
 * {@link ParameterDeclarerContributor} implementation which given a {@link ExtensionParameter} which their type is one of the
 * considered as an Infrastructure Type ({@link InfrastructureTypeMapping}) changes the {@link ExpressionSupport} to
 * {@link ExpressionSupport#NOT_SUPPORTED} and adds {@link InfrastructureParameterModelProperty}
 *
 * @since 4.0
 */
public class InfrastructureFieldContributor implements ParameterDeclarerContributor {

  private static final Map<Type, InfrastructureType> TYPE_MAPPING = InfrastructureTypeMapping.getMap().entrySet()
      .stream()
      .collect(toImmutableMap(entry -> new TypeWrapper(entry.getKey(),
                                                       new DefaultExtensionsTypeLoaderFactory()
                                                           .createTypeLoader(InfrastructureTypeMapping.class.getClassLoader())),
                              Map.Entry::getValue));


  public static Optional<InfrastructureType> getInfrastructureType(Type type) {
    return TYPE_MAPPING.entrySet()
        .stream()
        .filter(entry -> entry.getKey().isSameType(type))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void contribute(ExtensionParameter parameter, ParameterDeclarer declarer,
                         ParameterDeclarationContext declarationContext) {
    getInfrastructureType(parameter.getType()).ifPresent(infrastructureType -> {
      if (!isBlank(infrastructureType.getName())) {
        declarer.withModelProperty(new InfrastructureParameterModelProperty(infrastructureType.getSequence()));
        declarer.withExpressionSupport(NOT_SUPPORTED);
        getQName(infrastructureType.getName()).ifPresent(declarer::withModelProperty);
        getDslConfiguration(infrastructureType.getName()).ifPresent(declarer::withDsl);
      }
    });
  }
}

