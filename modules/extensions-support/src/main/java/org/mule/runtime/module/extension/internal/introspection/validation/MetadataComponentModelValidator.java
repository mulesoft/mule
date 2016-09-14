/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.apache.commons.lang.StringUtils.join;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import static org.mule.runtime.core.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver.NULL_CATEGORY_NAME;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.api.metadata.resolving.MetadataResolver;
import org.mule.runtime.extension.api.ExtensionWalker;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.RuntimeComponentModel;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.introspection.operation.HasOperationModels;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.source.HasSourceModels;
import org.mule.runtime.extension.api.introspection.source.SourceModel;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates that all {@link OperationModel operations} which return type is a {@link Object} or a {@link Map} have defined a
 * {@link MetadataOutputResolver}. The {@link MetadataOutputResolver} can't be the {@link NullMetadataResolver}.
 *
 * @since 4.0
 */
public class MetadataComponentModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel extensionModel) throws IllegalModelDefinitionException {
    new ExtensionWalker() {

      @Override
      public void onOperation(HasOperationModels owner, OperationModel model) {
        validateMetadataReturnType(extensionModel, model);
      }

      @Override
      public void onSource(HasSourceModels owner, SourceModel model) {
        validateMetadataReturnType(extensionModel, model);
      }
    }.walk(extensionModel);
  }

  private void validateMetadataReturnType(ExtensionModel extensionModel, ComponentModel componentModel) {
    RuntimeComponentModel component = (RuntimeComponentModel) componentModel;
    MetadataType returnMetadataType = component.getOutput().getType();
    validateCategories(component.getMetadataResolverFactory());

    if (returnMetadataType instanceof ObjectType) {
      validateReturnType(extensionModel, component, getType(returnMetadataType));
    } else if (returnMetadataType instanceof DictionaryType) {
      validateReturnType(extensionModel, component, getType(((DictionaryType) returnMetadataType).getValueType()));
    }
  }

  private void validateReturnType(ExtensionModel extensionModel, RuntimeComponentModel component, Class<?> returnType) {
    if (Object.class.equals(returnType)
        && component.getMetadataResolverFactory().getOutputResolver() instanceof NullMetadataResolver) {
      throw new IllegalModelDefinitionException(format("Component '%s' in Extension '%s' specifies '%s' as a return type. Operations and Sources with "
          + "return type such as Object or Map must have defined a not null MetadataOutputResolver", component.getName(),
                                                       extensionModel.getName(), returnType.getName()));
    }
  }

  private void validateCategories(MetadataResolverFactory metadataResolverFactory) {
    if (metadataResolverFactory != null) {
      validateCategoryNames(metadataResolverFactory.getKeyResolver(), metadataResolverFactory.getContentResolver(),
                            metadataResolverFactory.getOutputAttributesResolver(), metadataResolverFactory.getOutputResolver());
    }
  }

  private void validateCategoryNames(MetadataResolver... resolvers) {
    stream(resolvers).filter(r -> isBlank(r.getCategoryName()))
        .findFirst().ifPresent(r -> {
          throw new IllegalModelDefinitionException(format("Metadata resolver '%s' should have a  non empty category name",
                                                           r.getClass().getSimpleName()));
        });

    Set<String> names = stream(resolvers)
        .map(MetadataResolver::getCategoryName)
        .filter(r -> !r.equals(NULL_CATEGORY_NAME))
        .collect(Collectors.toSet());

    if (names.size() > 1) {
      throw new IllegalModelDefinitionException(format("Metadata resolvers should belong to the same category but resolvers from [%s] categories were found",
                                                       join(names, ",")));
    }
  }
}
