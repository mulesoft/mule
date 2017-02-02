/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import static java.lang.String.format;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

/**
 * {@link ParameterDeclarerContributor} implementation which given a {@link ExtensionParameter} of {@link this#classToUnwrap}
 * type, replaces the parameter type with the generic type of it and adds a the given {@link this#modelProperty}
 * to communicate the change.
 *
 * @since 4.0
 */
public class ParameterTypeUnwrapperContributor implements ParameterDeclarerContributor {

  private ClassTypeLoader typeLoader;
  private Class<?> classToUnwrap;
  private ModelProperty modelProperty;

  public ParameterTypeUnwrapperContributor(ClassTypeLoader typeLoader, Class classToUnwrap, ModelProperty modelProperty) {
    this.typeLoader = typeLoader;
    this.classToUnwrap = classToUnwrap;
    this.modelProperty = modelProperty;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void contribute(ExtensionParameter parameter, ParameterDeclarer declarer,
                         ParameterDeclarationContext declarationContext) {

    if (classToUnwrap.isAssignableFrom(parameter.getType().getDeclaringClass())) {
      ResolvableType[] generics = ResolvableType.forType(parameter.getJavaType()).getGenerics();

      MetadataType metadataType;
      Type type;
      if (generics.length > 0) {
        type = generics[0].getType();
        metadataType = typeLoader.load(type);
      } else {
        throw new IllegalParameterModelDefinitionException(
                                                           format(
                                                                  "The parameter [%s] from the Operation [%s] doesn't specify the %s parameterized type",
                                                                  parameter.getName(),
                                                                  declarationContext.getName(),
                                                                  classToUnwrap.getSimpleName()));
      }
      declarer.ofType(metadataType);
      declarer.withModelProperty(modelProperty);
    }
  }
}
