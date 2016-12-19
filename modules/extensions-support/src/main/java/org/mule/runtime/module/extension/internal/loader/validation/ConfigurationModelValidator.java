/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getConfigurationFactory;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigTypeModelProperty;

import java.util.Optional;

/**
 * {@link ExtensionModelValidator} which applies to {@link ExtensionModel}s which contains {@link ConfigurationModel}s and
 * {@link OperationModel}s .
 * <p>
 * This validator makes sure that all {@link OperationModel operations } are compatible with the defined {@link ConfigurationModel
 * c}
 *
 * @since 4.0
 */
public final class ConfigurationModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel model, ProblemsReporter problemsReporter) {
    new ExtensionWalker() {

      @Override
      public void onOperation(HasOperationModels owner, OperationModel operationModel) {
        if (owner instanceof ConfigurationModel) {

          Class<?> configType = getConfigurationFactory((ConfigurationModel) owner).getObjectType();
          Optional<Class<?>> operationConfigParameterType = operationModel.getModelProperty(ConfigTypeModelProperty.class)
              .map(modelProperty -> modelProperty.getConfigType());

          if (operationConfigParameterType.isPresent() && !operationConfigParameterType.get().isAssignableFrom(configType)) {
            problemsReporter.addError(new Problem(operationModel, format(
                                                                         "Operation '%s' requires a configuration of type '%s'. However, the operation is "
                                                                             + "reachable from configuration '%s' of incompatible type '%s'.",
                                                                         operationModel.getName(),
                                                                         operationConfigParameterType.get().getName(),
                                                                         ((ConfigurationModel) owner).getName(),
                                                                         configType.getName())));
          }
        }
      }
    }.walk(model);
  }
}
