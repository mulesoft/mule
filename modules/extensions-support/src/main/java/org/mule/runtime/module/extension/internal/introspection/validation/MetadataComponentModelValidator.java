/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.lang.String.format;
import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.extension.api.ExtensionWalker;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.RuntimeComponentModel;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.introspection.operation.HasOperationModels;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.source.HasSourceModels;
import org.mule.runtime.extension.api.introspection.source.SourceModel;

import java.util.Map;

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
}
