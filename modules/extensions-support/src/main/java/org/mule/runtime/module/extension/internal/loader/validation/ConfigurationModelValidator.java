/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.module.extension.internal.loader.validation.ModelValidationUtils.validateConfigOverrideParametersNotAllowed;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getConfigurationFactory;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigTypeModelProperty;

import java.util.List;
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
        validateInjectedConfigType(owner, operationModel, problemsReporter);

        List<ParameterModel> overrides = getOverrideParameters(operationModel);

        if (!overrides.isEmpty()) {
          if (owner instanceof ConfigurationModel) {

            List<String> invalidOverrides = getInvalidOverrides((ConfigurationModel) owner, overrides);

            if (!invalidOverrides.isEmpty()) {
              reportNoMatchingParameterForOverride(((ConfigurationModel) owner).getName(), operationModel, invalidOverrides,
                                                   problemsReporter);
            }

          } else if (!model.getConfigurationModels().isEmpty()) {
            reportNoConfigurationForOverride(operationModel, problemsReporter);
          }
        }
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel sourceModel) {
        List<ParameterModel> overrides = getOverrideParameters(sourceModel);

        if (!overrides.isEmpty()) {
          if (owner instanceof ConfigurationModel) {

            List<String> invalidOverrides = getInvalidOverrides((ConfigurationModel) owner, overrides);

            if (!invalidOverrides.isEmpty()) {
              reportNoMatchingParameterForOverride(((ConfigurationModel) owner).getName(), sourceModel, invalidOverrides,
                                                   problemsReporter);
            }

          } else if (!model.getConfigurationModels().isEmpty()) {
            reportNoConfigurationForOverride(sourceModel, problemsReporter);
          }
        }

      }

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        validateConfigOverrideParametersNotAllowed(model, problemsReporter, "Configuration");
      }
    }.walk(model);
  }

  private void reportNoMatchingParameterForOverride(String ownerName, ComponentModel model, List<String> parameters,
                                                    ProblemsReporter reporter) {
    reporter.addError(new Problem(model, format(
                                                "%s '%s' declares parameters %s as '%s' but associated Configuration '%s' does not declare parameters of the same name or alias.",
                                                capitalize(getComponentModelTypeName(model)),
                                                model.getName(),
                                                parameters,
                                                ConfigOverride.class.getSimpleName(),
                                                ownerName)));
  }

  private void reportNoConfigurationForOverride(ComponentModel model, ProblemsReporter reporter) {
    reporter.addError(new Problem(model, format(
                                                "%s '%s' declares parameters as '%s' but is not associated to a Configuration.",
                                                capitalize(getComponentModelTypeName(model)),
                                                model.getName(),
                                                ConfigOverride.class.getSimpleName())));
  }

  private List<ParameterModel> getOverrideParameters(ParameterizedModel model) {
    return model.getAllParameterModels().stream()
        .filter(ParameterModel::isOverrideFromConfig).collect(toList());
  }

  private List<String> getInvalidOverrides(ConfigurationModel owner, List<ParameterModel> overrides) {
    return overrides.stream()
        .filter(override -> owner.getAllParameterModels().stream()
            .noneMatch(p -> p.getName().equals(override.getName())))
        .map(ParameterModel::getName)
        .collect(toList());
  }

  private void validateInjectedConfigType(HasOperationModels owner, OperationModel operationModel,
                                          ProblemsReporter problemsReporter) {
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
}
