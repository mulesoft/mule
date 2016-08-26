/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.runtime.extension.api.ExtensionWalker;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.operation.HasOperationModels;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.introspection.property.ConfigTypeModelProperty;

import java.util.Optional;

/**
 * {@link ModelValidator} which applies to {@link ExtensionModel}s which contains {@link ConfigurationModel}s and
 * {@link OperationModel}s .
 * <p>
 * This validator makes sure that all {@link OperationModel operations } are compatible with the defined {@link ConfigurationModel
 * c}
 *
 * @since 4.0
 */
public final class ConfigurationModelValidator implements ModelValidator {

  @Override
  public void validate(ExtensionModel model) throws IllegalModelDefinitionException {
    new ExtensionWalker() {

      @Override
      public void onOperation(HasOperationModels owner, OperationModel operationModel) {
        if (owner instanceof RuntimeConfigurationModel) {

          Class<?> configType = ((RuntimeConfigurationModel) owner).getConfigurationFactory().getObjectType();
          Optional<Class<?>> operationConfigParameterType = operationModel.getModelProperty(ConfigTypeModelProperty.class)
              .map(modelProperty -> getType(modelProperty.getConfigType()));

          if (operationConfigParameterType.isPresent() && !operationConfigParameterType.get().isAssignableFrom(configType)) {
            throw new IllegalConfigurationModelDefinitionException(String.format(
                                                                                 "Extension '%s' defines operation '%s' which requires a configuration of type '%s'. However, the operation is "
                                                                                     + "reachable from configuration '%s' of incompatible type '%s'.",
                                                                                 model.getName(),
                                                                                 operationModel.getName(),
                                                                                 operationConfigParameterType.get().getName(),
                                                                                 ((RuntimeConfigurationModel) owner).getName(),
                                                                                 configType.getName()));
          }
        }
      }
    }.walk(model);
  }
}
