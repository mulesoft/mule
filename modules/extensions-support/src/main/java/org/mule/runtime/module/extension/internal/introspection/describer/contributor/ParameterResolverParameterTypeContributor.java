/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.contributor;

import static java.lang.String.format;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getGenericTypeAt;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.module.extension.internal.introspection.describer.ParameterResolverTypeModelProperty;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ExtensionParameter;
import org.mule.runtime.module.extension.internal.introspection.utils.ParameterDeclarationContext;

import java.util.Optional;

/**
 * {@link ParameterDeclarerContributor} implementation which given a {@link ExtensionParameter} of
 * {@link ParameterResolver} type changes the declared type of the {@link ParameterDeclarer} from a
 * {@link ParameterResolver} to the type of the generic.
 * Also adds a {@link ParameterResolverTypeModelProperty} which indicates that the Java parameters if of
 * {@link ParameterResolver} type
 *
 * @since 4.0
 * @see ParameterResolver
 */
public class ParameterResolverParameterTypeContributor implements ParameterDeclarerContributor {

  private ClassTypeLoader typeLoader;

  public ParameterResolverParameterTypeContributor(ClassTypeLoader typeLoader) {
    this.typeLoader = typeLoader;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void contribute(ExtensionParameter parameter, ParameterDeclarer declarer,
                         ParameterDeclarationContext declarationContext) {
    MetadataType metadataType = parameter.getMetadataType(typeLoader);
    if (ParameterResolver.class.isAssignableFrom(parameter.getType().getDeclaringClass())) {
      final Optional<MetadataType> expressionResolverType = getGenericTypeAt(metadataType, 0, typeLoader);
      if (expressionResolverType.isPresent()) {
        metadataType = expressionResolverType.get();
      } else {
        throw new IllegalParameterModelDefinitionException(
                                                           format(
                                                                  "The parameter [%s] from the Operation [%s] doesn't specify the %s parameterized type",
                                                                  parameter.getName(),
                                                                  declarationContext.getName(),
                                                                  ParameterResolver.class.getSimpleName()));
      }
      declarer.ofType(metadataType);
      declarer.withModelProperty(new ParameterResolverTypeModelProperty());
    }
  }
}
