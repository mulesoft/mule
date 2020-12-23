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
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionModelValidator;
import org.mule.runtime.extension.api.loader.Problem;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.module.extension.internal.loader.java.property.InjectableParameterInfo;
import org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.data.sample.SampleDataProvider;
import org.mule.sdk.api.runtime.streaming.PagingProvider;

import java.lang.reflect.Type;
import java.util.Collection;
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

    validateGenerics(model, providerClass, problemsReporter);

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

  private void validateGenerics(ConnectableComponentModel model,
                                Class<? extends SampleDataProvider> providerClass,
                                ProblemsReporter problemsReporter) {

    Pair<Type, Type> providerGenericTypes = getProviderGenerics(providerClass);
    if (providerGenericTypes.getFirst() == null) {
      problemsReporter.addError(new Problem(model, format("SampleDataProvider [%s] does not specify generics definition", providerClass.getName())));
      return;
    }

    Pair<Type, Type> outputGenericTypes = getOutputTypes(model, providerClass.getClassLoader());
    if (!validateIfPaged(model, providerClass, outputGenericTypes, providerGenericTypes, problemsReporter)) {
      String providerGenerics = asGenericSignature(getInterfaceGenerics(providerClass, SampleDataProvider.class));
      String outputGenerics = asGenericSignature(outputGenericTypes);

      if (!Objects.equals(providerGenerics, outputGenerics)) {
        problemsReporter.addError(new Problem(model, format(
                "SampleDataProvider [%s] is used at component '%s' which outputs a Result%s, but the provider generic signature is '%s'",
                providerClass.getName(), model.getName(), outputGenerics,
                providerGenerics)));
      }
    }

  }

  private Pair<Type, Type> getProviderGenerics(Class<? extends SampleDataProvider> providerClass) {
    List<Type> generics = getInterfaceGenerics(providerClass, SampleDataProvider.class);
    return generics.isEmpty() ? new Pair<>(null, null) : new Pair<>(generics.get(0), generics.get(1));
  }

  private Pair<Type, Type> getOutputTypes(ConnectableComponentModel model, ClassLoader classLoader) {
    return new Pair<>(JavaTypeUtils.getType(model.getOutput().getType(), classLoader),
                  JavaTypeUtils.getType(model.getOutputAttributes().getType(), classLoader));
  }

  private String asGenericSignature(List<Type> types) {
    return "<" + types.stream()
        .map(this::asString)
        .collect(joining(",")) + ">";
  }

  private String asGenericSignature(Pair<Type, Type> types) {
    return "<" + asString(types.getFirst()) + "," + asString(types.getSecond()) + ">";
  }

  private boolean validateIfPaged(ConnectableComponentModel component,
                                  Class<? extends SampleDataProvider> providerClass,
                                  Pair<Type, Type> outputGenericTypes,
                               Pair<Type, Type> sampleDataProviderGenericTypes,
                                  ProblemsReporter reporter) {
    if (!isAssignableFrom(PagingProvider.class, outputGenericTypes.getFirst())) {
      return false;
    }

    final List<Type> pagingProviderGenerics = getInterfaceGenerics(outputGenericTypes.getFirst(), PagingProvider.class);
    final Type pageItemsType = pagingProviderGenerics.size() < 2 || pagingProviderGenerics.get(1) == null
            ? Object.class
            : pagingProviderGenerics.get(1);

    Type sampleDataPayloadType = sampleDataProviderGenericTypes.getFirst();

      if (!isAssignableFrom(Collection.class, sampleDataPayloadType)) {
                reporter.addError(new Problem(component, format(
                        "SampleDataProvider [%s] is used on component '%s' which is paged. The SampleDataProvider is thus expected to provide a payload of type 'Collection<%s>'. '%s' was found instead",
                        providerClass.getName(), component.getName(), asString(pageItemsType), asString(sampleDataPayloadType))));
        return true;
      }

      List<Type> sampleDataCollectionGeneric = getInterfaceGenerics(sampleDataPayloadType, Collection.class);
      if (sampleDataCollectionGeneric.isEmpty() || sampleDataCollectionGeneric.get(0) == null) {
        reporter.addError(new Problem(component, format(
                "SampleDataProvider [%s] is used on component '%s' which is paged. The SampleDataProvider is thus expected to provide a payload of type 'Collection<%s>', but an unbounded Collection was found instead. Please provide a generic",
                providerClass.getName(), component.getName(), asString(pageItemsType))));
        return true;
      }

      final Type sampleProviderCollectionType = sampleDataCollectionGeneric.get(0);

      if (!pageItemsType.equals(sampleProviderCollectionType)) {
        reporter.addError(new Problem(component, format(
                "SampleDataProvider [%s] is used on component '%s' which is paged. The SampleDataProvider is thus expected to provide a payload of type 'Collection<%s>', but an unbounded Collection was found instead. Please provide a generic",
                providerClass.getName(), component.getName(), asString(pageItemsType))));
        return true;
      }

      // validate attributes
      return true;
  }

//  private Pair<Type, Type> getPagingProviderGenerics(OperationModel model) {
//    model.getModelProperty(ImplementingMethodModelProperty.class)
//            .map(mp -> mp.getMethod().getGenericReturnType())
//  }


  private boolean isVoid(Type componentType) {
    return Void.class.equals(componentType) || void.class.equals(componentType);
  }

  private boolean isAssignableFrom(Class base, Type target) {
    if (target instanceof Class) {
      return base.isAssignableFrom((Class) target);
    } else if (target instanceof ParameterizedTypeImpl) {
      return base.isAssignableFrom(((ParameterizedTypeImpl) target).getRawType());
    }

    return false;
  }

  private String asString(Type type) {
    if (type instanceof ParameterizedTypeImpl) {
      ParameterizedTypeImpl parameterizedType = (ParameterizedTypeImpl) type;
      return parameterizedType.getRawType().getName() + asGenericSignature(asList(parameterizedType.getActualTypeArguments()));
    } else if (type == null) {
      return Object.class.getName();
    } else if (isVoid(type)) {
      return void.class.getName();
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
