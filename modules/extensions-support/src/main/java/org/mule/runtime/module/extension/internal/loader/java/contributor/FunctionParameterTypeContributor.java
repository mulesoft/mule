/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.contributor;

import static java.lang.String.format;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getGenericTypeAt;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.utils.MetadataTypeUtils;
import org.mule.runtime.api.meta.model.ParameterDslConfiguration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.module.extension.internal.loader.java.FunctionParameterTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

import java.util.Optional;
import java.util.function.Function;

/**
 * {@link ParameterDeclarerContributor} implementation which given a {@link ExtensionParameter} of
 * {@link Function} type changes the declared type of the {@link ParameterDeclarer} from a
 * {@link Function} to the type of the generic.
 * Also adds a {@link FunctionParameterTypeModelProperty} which indicates that the Java parameters if of
 * {@link Function} type
 *
 * @see ParameterResolver
 * @since 4.0
 */
public class FunctionParameterTypeContributor implements ParameterDeclarerContributor {

  private ClassTypeLoader typeLoader;

  public FunctionParameterTypeContributor(ClassTypeLoader typeLoader) {
    this.typeLoader = typeLoader;
  }

  @Override
  public void contribute(ExtensionParameter parameter, ParameterDeclarer declarer,
                         ParameterDeclarationContext declarationContext) {
    MetadataType metadataType = parameter.getMetadataType(typeLoader);
    if (java.util.function.Function.class.isAssignableFrom(parameter.getType().getDeclaringClass())) {
      final Optional<MetadataType> eventType = getGenericTypeAt(metadataType, 0, typeLoader);
      if (eventType.isPresent() && isEventType(eventType.get())) {
        final Optional<MetadataType> expressionType = getGenericTypeAt(metadataType, 1, typeLoader);
        if (expressionType.isPresent()) {
          metadataType = expressionType.get();
        } else {
          throw new IllegalParameterModelDefinitionException(
                                                             format(
                                                                    "The parameter [%s] from the %s [%s] doesn't specify the %s parameterized types: Function<Event,{Type}>",
                                                                    parameter.getName(),
                                                                    declarationContext.getComponentType(),
                                                                    declarationContext.getName(),
                                                                    Function.class.getSimpleName()));
        }
        declarer.ofType(metadataType);
        declarer.withModelProperty(new FunctionParameterTypeModelProperty());
        declarer.withDsl(ParameterDslConfiguration.builder()
            .allowsInlineDefinition(false)
            .allowsReferences(true)
            .allowTopLevelDefinition(false)
            .build());
      }
    }
  }

  private boolean isEventType(MetadataType eventType) {
    Optional<String> typeId = MetadataTypeUtils.getTypeId(eventType);
    if (typeId.isPresent()) {
      try {
        return ClassUtils.getClass(typeId.get()).equals(Event.class);
      } catch (ClassNotFoundException e) {
        return false;
      }
    }
    return false;
  }
}
