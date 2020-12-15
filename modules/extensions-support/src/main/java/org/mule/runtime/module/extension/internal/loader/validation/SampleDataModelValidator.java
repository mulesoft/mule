/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentModelTypeName;
import static org.mule.runtime.extension.api.util.NameUtils.getModelName;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getInterfaceGenerics;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isInstantiable;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.utils.JavaTypeUtils;
import org.mule.runtime.api.meta.model.ConnectableComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.data.sample.SampleDataProviderModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.property.InjectableParameterInfo;
import org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.data.sample.SampleDataProvider;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

/**
 * {@link ExtensionModelValidator} for the correct usage of {@link SampleDataProviderModel} and
 * {@link SampleDataProviderFactoryModelProperty}
 *
 * @since 4.4.0
 */
public final class SampleDataModelValidator implements ExtensionModelValidator {

  @Override
  public void validate(ExtensionModel model, ProblemsReporter problemsReporter) {
    final ReflectionCache reflectionCache = new ReflectionCache();
    final Delegate delegate = new Delegate(problemsReporter);
    new ExtensionWalker() {

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        validateModel(model, isConfig(owner), problemsReporter, delegate, reflectionCache);
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        validateModel(model, isConfig(owner), problemsReporter, delegate, reflectionCache);
      }

      private boolean isConfig(Object owner) {
        return owner instanceof ConfigurationModel;
      }
    }.walk(model);

    delegate.validateIdsAreUnique();
  }

  private void validateModel(ConnectableComponentModel model,
                             boolean modelHasConfig,
                             ProblemsReporter problemsReporter,
                             Delegate delegate,
                             ReflectionCache reflectionCache) {
    model.getModelProperty(SampleDataProviderFactoryModelProperty.class)
        .ifPresent(modelProperty -> validateResolver(model, modelHasConfig, modelProperty, problemsReporter, reflectionCache,
                                                     delegate));
  }

  private void validateResolver(ConnectableComponentModel model,
                                boolean modelHasConfig,
                                SampleDataProviderFactoryModelProperty modelProperty,
                                ProblemsReporter problemsReporter,
                                ReflectionCache reflectionCache,
                                Delegate delegate) {
    Class<? extends SampleDataProvider> providerClass = modelProperty.getSampleDataProviderClass();

    validateGenerics(model, problemsReporter, providerClass);

    String providerName = providerClass.getSimpleName();
    Optional<SampleDataProviderModel> providerModel = model.getSampleDataProviderModel();
    if (!providerModel.isPresent()) {
      throw new IllegalModelDefinitionException(format("Component %s should have an associated SampleDataProviderModel.",
                                                       model.getName()));
    } else {
      delegate.addInfo(
                       new SampleDataProviderInfo(providerModel.get(), model, providerClass.getName()));
    }

    Map<String, MetadataType> allParameters =
        model.getAllParameterModels().stream().collect(toMap(IntrospectionUtils::getImplementingName, ParameterModel::getType));
    String modelName = getModelName(model);
    String modelTypeName = getComponentModelTypeName(model);

    if (!isInstantiable(providerClass, reflectionCache)) {
      problemsReporter.addError(new Problem(model, format("The SampleDataProvider [%s] is not instantiable", providerName)));
    }

    for (InjectableParameterInfo parameterInfo : modelProperty.getInjectableParameters()) {

      if (!allParameters.containsKey(parameterInfo.getParameterName())) {
        problemsReporter.addError(new Problem(model,
                                              format("The SampleDataProvider [%s] declares a parameter '%s' which doesn't exist in the %s '%s'",
                                                     providerName, parameterInfo.getParameterName(), modelTypeName, modelName)));
      } else {
        MetadataType metadataType = allParameters.get(parameterInfo.getParameterName());
        Class<?> expectedType = getType(metadataType)
            .orElseThrow(() -> new IllegalStateException(format("Unable to get Class for parameter: %s",
                                                                parameterInfo.getParameterName())));
        Class<?> gotType = getType(parameterInfo.getType())
            .orElseThrow(() -> new IllegalStateException(format("Unable to get Class for parameter: %s",
                                                                parameterInfo.getParameterName())));

        if (!expectedType.equals(gotType)) {
          problemsReporter.addError(new Problem(model,
                                                format("The SampleDataProvider [%s] defines a parameter '%s' of type '%s' but in the %s '%s' is of type '%s'",
                                                       providerName, parameterInfo.getParameterName(), gotType, modelTypeName,
                                                       modelName, expectedType)));
        }
      }
    }

    if (modelProperty.usesConnection() && !model.requiresConnection()) {
      problemsReporter.addError(new Problem(model,
                                            format("The SampleDataProvider [%s] defines that it requires a connection, but is used in the %s '%s' which is connection less",
                                                   providerName, modelTypeName, modelName)));
    }

    if (modelProperty.usesConfig() && !modelHasConfig) {
      problemsReporter.addError(new Problem(model,
                                            format("The SampleDataProvider [%s] defines that it requires a config, but is used in the %s '%s' which is config less",
                                                   providerName, modelTypeName, modelName)));
    }
  }

  private void validateGenerics(ConnectableComponentModel model, ProblemsReporter problemsReporter,
                                Class<? extends SampleDataProvider> providerClass) {
    String providerGenerics = asGenericSignature(getInterfaceGenerics(providerClass, SampleDataProvider.class));
    String outputGenerics = asGenericSignature(getOutputTypes(model, providerClass.getClassLoader()));

    if (!Objects.equals(providerGenerics, outputGenerics)) {
      problemsReporter.addError(new Problem(model, format(
                                                          "SampleDataProvider [%s] was expecting to define '%s' generics signature but '%s' was found instead",
                                                          providerClass.getName(), outputGenerics, providerGenerics)));
    }
  }

  private List<Type> getOutputTypes(ConnectableComponentModel model, ClassLoader classLoader) {
    return asList(JavaTypeUtils.getType(model.getOutput().getType(), classLoader),
                  JavaTypeUtils.getType(model.getOutputAttributes().getType(), classLoader));
  }

  private String asGenericSignature(List<Type> types) {
    return "<" + types.stream()
        .map(this::asString)
        .collect(joining(",")) + ">";
  }

  private String asString(Type type) {
    if (type instanceof ParameterizedTypeImpl) {
      ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) type;
      return parameterizedType.getRawType().getName() + asGenericSignature(asList(parameterizedType.getActualTypeArguments()));
    } else {
      return type.getTypeName();
    }
  }

  private static class SampleDataProviderInfo {

    private SampleDataProviderModel sampleDataProviderModel;
    private ConnectableComponentModel ownerModel;
    private String implementationClassName;

    public SampleDataProviderInfo(SampleDataProviderModel sampleDataProviderModel,
                                  ConnectableComponentModel ownerModel,
                                  String implementationClassName) {
      this.sampleDataProviderModel = sampleDataProviderModel;
      this.ownerModel = ownerModel;
      this.implementationClassName = implementationClassName;
    }

    public SampleDataProviderModel getSampleDataProviderModel() {
      return sampleDataProviderModel;
    }

    public ConnectableComponentModel getOwnerModel() {
      return ownerModel;
    }

    public String getImplementationClassName() {
      return implementationClassName;
    }
  }

  private static final class Delegate {

    private Map<String, SampleDataProviderInfo> implInfo = new HashMap<>();
    private MultiMap<String, String> idToImpl = new MultiMap<>();
    private ProblemsReporter problemsReporter;

    public Delegate(ProblemsReporter problemsReporter) {
      this.problemsReporter = problemsReporter;
    }

    public void addInfo(SampleDataProviderInfo sampleDataProviderInfo) {
      String valueProviderImplementation = sampleDataProviderInfo.getImplementationClassName();
      if (!implInfo.containsKey(valueProviderImplementation)) {
        implInfo.put(valueProviderImplementation, sampleDataProviderInfo);
        idToImpl.put(sampleDataProviderInfo.getSampleDataProviderModel().getProviderId(), valueProviderImplementation);
      }
    }

    public void validateIdsAreUnique() {
      idToImpl.keySet().forEach(providerId -> {
        List<String> implementationIds = idToImpl.getAll(providerId);

        if (implementationIds.size() > 1) {
          String firstImpl = implementationIds.get(0);
          SampleDataProviderInfo sampleDataProviderInfo = implInfo.get(firstImpl);
          problemsReporter.addError(new Problem(sampleDataProviderInfo.getOwnerModel(),
                                                format("The following SampleDataProvider implementations [%s] use the same id [%s]. "
                                                    + "SampleDataProvider ids must be unique.",
                                                       join(", ", implementationIds), providerId)));
        }
      });
    }
  }
}
