/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.getModelName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.getParameterNameFromExtractionExpression;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.util.NameUtils;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldsValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.InjectableParameterInfo;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ExtensionModelValidator} for the correct usage of {@link ValueProviderModel} and
 * {@link ValueProviderFactoryModelProperty}
 *
 * @since 4.0
 */
public final class ValueProviderModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel model, ProblemsReporter problemsReporter) {
    final ReflectionCache reflectionCache = new ReflectionCache();
    final ValueProvidersIdValidator valueProvidersIdValidator = new ValueProvidersIdValidator(problemsReporter);
    new IdempotentExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        validateModel(model, problemsReporter, false, valueProvidersIdValidator, reflectionCache);
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderModel model) {
        validateModel(model, problemsReporter, false, valueProvidersIdValidator, reflectionCache);
      }

      @Override
      protected void onSource(SourceModel model) {
        validateModel(model, problemsReporter, true, valueProvidersIdValidator, reflectionCache);
      }

      @Override
      protected void onOperation(OperationModel model) {
        validateModel(model, problemsReporter, true, valueProvidersIdValidator, reflectionCache);
      }
    }.walk(model);
    valueProvidersIdValidator.validateIdsAreUnique();
  }


  private void validateModel(ParameterizedModel model, ProblemsReporter problemsReporter, boolean supportsConnectionsAndConfigs,
                             ValueProvidersIdValidator valueProvidersIdValidator, ReflectionCache reflectionCache) {
    model.getAllParameterModels()
        .forEach(param -> {
          Optional<ValueProviderFactoryModelProperty> valueProviderFactoryModelProperty =
              param.getModelProperty(ValueProviderFactoryModelProperty.class);
          Optional<FieldsValueProviderFactoryModelProperty> fieldValueProviderFactoryModelProperty =
              param.getModelProperty(FieldsValueProviderFactoryModelProperty.class);

          if (valueProviderFactoryModelProperty.isPresent() && fieldValueProviderFactoryModelProperty.isPresent()) {
            problemsReporter
                .addError(new Problem(model,
                                      format("Parameter [%s] from %s with name %s has both a Value Provider and a Field Value Provider",
                                             param.getName(), getComponentModelTypeName(model), getModelName(model))));
          } else if (valueProviderFactoryModelProperty.isPresent()) {
            validateOptionsResolver(param, true, null, valueProviderFactoryModelProperty.get(), model, problemsReporter,
                                    supportsConnectionsAndConfigs, reflectionCache, valueProvidersIdValidator);
          } else if (fieldValueProviderFactoryModelProperty.isPresent()) {
            fieldValueProviderFactoryModelProperty.get().getFieldsValueProviderFactories()
                .forEach((targetSelector,
                          fieldsValueProviderFactoryModelProperty) -> validateOptionsResolver(param, false,
                                                                                              targetSelector,
                                                                                              fieldsValueProviderFactoryModelProperty,
                                                                                              model, problemsReporter,
                                                                                              supportsConnectionsAndConfigs,
                                                                                              reflectionCache,
                                                                                              valueProvidersIdValidator));
          }
        });
  }

  private void validateOptionsResolver(ParameterModel param, boolean mustBeStringType,
                                       String targetSelector,
                                       ValueProviderFactoryModelProperty modelProperty,
                                       ParameterizedModel model, ProblemsReporter problemsReporter,
                                       boolean supportsConnectionsAndConfigs, ReflectionCache reflectionCache,
                                       ValueProvidersIdValidator valueProvidersIdValidator) {
    Class<?> valueProvider = modelProperty.getValueProvider();
    String providerName = valueProvider.getSimpleName();

    Optional<? extends ValueProviderModel> valueProviderModel;
    if (targetSelector != null) {
      valueProviderModel = param.getFieldValueProviderModels().stream()
          .filter(fieldValueProviderModel -> fieldValueProviderModel.getTargetSelector().equals(targetSelector))
          .findAny();
    } else {
      valueProviderModel = param.getValueProviderModel();
    }

    if (!valueProviderModel.isPresent()) {
      throw new IllegalStateException(format("Parameter %s from %s with name %s has should have a ValueProviderModel associated.",
                                             param.getName(), getComponentModelTypeName(model), getModelName(model)));
    } else {
      valueProvidersIdValidator
          .addValueProviderInformation(new ValueProviderInformation(valueProviderModel.get(), model, valueProvider.getName()));
    }
    Map<String, MetadataType> allParameters =
        model.getAllParameterModels().stream().collect(toMap(ParameterModel::getName, ParameterModel::getType));
    String modelName = NameUtils.getModelName(model);
    String modelTypeName = getComponentModelTypeName(model);

    if (!isInstantiable(valueProvider, reflectionCache)) {
      problemsReporter.addError(new Problem(model, format("The Value Provider [%s] is not instantiable but it should",
                                                          providerName)));
    }

    if (mustBeStringType && !(param.getType() instanceof StringType)) {
      problemsReporter.addError(new Problem(model,
                                            format("The parameter [%s] of the %s '%s' is not of String type. Parameters that provides Values should be of String type.",
                                                   param.getName(), modelTypeName, modelName)));
    }

    for (InjectableParameterInfo parameterInfo : modelProperty.getInjectableParameters()) {
      String parameterName = getParameterNameFromExtractionExpression(parameterInfo.getExtractionExpression());
      if (!allParameters.containsKey(parameterName)) {
        problemsReporter.addError(new Problem(model,
                                              format("The Value Provider [%s] declares to use a parameter '%s' which doesn't exist in the %s '%s'",
                                                     providerName, parameterName, modelTypeName, modelName)));
      } else {
        if (parameterInfo.getExtractionExpression().equals(parameterInfo.getParameterName())) {
          MetadataType metadataType = allParameters.get(parameterInfo.getParameterName());
          Class<?> expectedType = getType(metadataType)
              .orElseThrow(() -> new IllegalStateException(format("Unable to get Class for parameter: %s",
                                                                  parameterInfo.getParameterName())));
          Class<?> gotType = getType(parameterInfo.getType())
              .orElseThrow(() -> new IllegalStateException(format("Unable to get Class for parameter: %s",
                                                                  parameterInfo.getParameterName())));

          if (!expectedType.equals(gotType)) {
            problemsReporter.addError(new Problem(model,
                                                  format("The Value Provider [%s] defines a parameter '%s' of type '%s' but in the %s '%s' is of type '%s'",
                                                         providerName, parameterInfo.getParameterName(), gotType, modelTypeName,
                                                         modelName, expectedType)));
          }
        }
      }
    }

    if (supportsConnectionsAndConfigs && modelProperty.usesConnection() && model instanceof ConnectableComponentModel) {
      boolean requiresConnection = ((ConnectableComponentModel) model).requiresConnection();
      if (requiresConnection != modelProperty.usesConnection()) {
        problemsReporter.addError(new Problem(model,
                                              format("The Value Provider [%s] defines that requires a connection, but is used in the %s '%s' which is connection less",
                                                     providerName, modelTypeName, modelName)));
      }
    }

    if (!supportsConnectionsAndConfigs) {
      if (modelProperty.usesConnection()) {
        problemsReporter.addError(new Problem(model,
                                              format("The Value Provider [%s] defines that requires a connection which is not allowed for a Value Provider of a %s's parameter [%s]",
                                                     providerName, modelTypeName, modelName)));
      }

      if (modelProperty.usesConfig()) {
        problemsReporter.addError(new Problem(model,
                                              format("The Value Provider [%s] defines that requires a configuration which is not allowed for a Value Provider of a %s's parameter [%s]",
                                                     providerName, modelTypeName, modelName)));
      }
    }
  }

  private static final class ValueProviderInformation {

    private ValueProviderModel valueProviderModel;
    private ParameterizedModel ownerModel;
    private String implementationClassName;

    public ValueProviderInformation(ValueProviderModel valueProviderModel,
                                    ParameterizedModel ownerModel, String implementationClassName) {
      this.valueProviderModel = valueProviderModel;
      this.ownerModel = ownerModel;
      this.implementationClassName = implementationClassName;
    }

    public ValueProviderModel getValueProviderModel() {
      return valueProviderModel;
    }

    public ParameterizedModel getOwnerModel() {
      return ownerModel;
    }

    public String getImplementationClassName() {
      return implementationClassName;
    }
  }

  private static final class ValueProvidersIdValidator {

    private Map<String, ValueProviderInformation> valueProvidersImplementationToInformation = new HashMap<>();
    private MultiMap<String, String> valueProvidersIdToImplementations = new MultiMap<>();
    private ProblemsReporter problemsReporter;

    public ValueProvidersIdValidator(ProblemsReporter problemsReporter) {
      this.problemsReporter = problemsReporter;
    }

    public void addValueProviderInformation(ValueProviderInformation valueProviderInformation) {
      String valueProviderImplementation = valueProviderInformation.getImplementationClassName();
      if (!valueProvidersImplementationToInformation.containsKey(valueProviderImplementation)) {
        valueProvidersImplementationToInformation.put(valueProviderImplementation, valueProviderInformation);
        valueProvidersIdToImplementations.put(valueProviderInformation.getValueProviderModel().getProviderId(),
                                              valueProviderImplementation);
      }
    }

    public void validateIdsAreUnique() {
      valueProvidersIdToImplementations.keySet().forEach((valueProviderId) -> {
        List<String> valueProviderImplementations = valueProvidersIdToImplementations.getAll(valueProviderId);

        if (valueProviderImplementations.size() > 1) {
          String firstValueProviderImplementation = valueProviderImplementations.get(0);
          ValueProviderInformation valueProviderInformation =
              valueProvidersImplementationToInformation.get(firstValueProviderImplementation);
          problemsReporter.addError(new Problem(valueProviderInformation.getOwnerModel(),
                                                format("The following ValueProvider implementations [%s] use the same id [%s]. "
                                                    +
                                                    "ValueProvider ids must be unique.",
                                                       join(", ", valueProviderImplementations), valueProviderId)));
        }
      });
    }
  }

}
