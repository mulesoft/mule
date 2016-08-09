/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationFactory;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.module.extension.internal.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.model.property.ConfigTypeModelProperty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

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
    ListMultimap<Class, String> configParams = getParameterConfigsFromOperations(model);

    for (ConfigurationModel configurationModel : model.getConfigurationModels()) {
      if (!(configurationModel instanceof RuntimeConfigurationModel)) {
        continue;
      }

      for (Class clazz : configParams.keySet()) {
        final ConfigurationFactory configurationFactory =
            ((RuntimeConfigurationModel) configurationModel).getConfigurationFactory();
        if (!clazz.isAssignableFrom(configurationFactory.getObjectType())) {
          throw new IllegalConfigurationModelDefinitionException(String.format(
                                                                               "Extension '%s' defines the '%s' configuration. However, the extension's operations %s expect configurations of type '%s'. "
                                                                                   + "Please make sure that all configurations in the extension can be used with all its operations.",
                                                                               model.getName(),
                                                                               configurationFactory.getObjectType(), clazz,
                                                                               configParams.get(clazz)));
        }
      }
    }
  }

  private ListMultimap<Class, String> getParameterConfigsFromOperations(ExtensionModel model) {
    ListMultimap<Class, String> operationsByConfig = ArrayListMultimap.create();

    for (OperationModel operationModel : model.getOperationModels()) {
      operationModel.getModelProperty(ConfigTypeModelProperty.class)
          .ifPresent(modelProperty -> operationsByConfig.put(getType(modelProperty.getConfigType()), operationModel.getName()));
    }

    return operationsByConfig;
  }

}
