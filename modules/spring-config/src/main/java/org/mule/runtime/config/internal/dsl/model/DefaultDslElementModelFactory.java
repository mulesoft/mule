/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model;

import static java.util.stream.Collectors.toMap;
import org.mule.runtime.app.declaration.api.ElementDeclaration;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.util.Map;
import java.util.Optional;

/**
 * Default implementation of a {@link DslElementModelFactory}
 *
 * @since 4.0
 */
public class DefaultDslElementModelFactory implements DslElementModelFactory {

  private final DeclarationBasedElementModelFactory declarationBasedDelegate;
  private final ConfigurationBasedElementModelFactory configurationBasedDelegate;

  public DefaultDslElementModelFactory(DslResolvingContext context) {
    final Map<ExtensionModel, DslSyntaxResolver> resolvers = context.getExtensions().stream()
        .collect(toMap(e -> e, e -> DslSyntaxResolver.getDefault(e, context)));

    this.declarationBasedDelegate = new DeclarationBasedElementModelFactory(context, resolvers);
    this.configurationBasedDelegate = new ConfigurationBasedElementModelFactory(resolvers);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Optional<DslElementModel<T>> create(ElementDeclaration componentDeclaration) {
    return declarationBasedDelegate.create(componentDeclaration);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Optional<DslElementModel<T>> create(ComponentConfiguration configuration) {
    return configurationBasedDelegate.create(configuration);
  }
}
