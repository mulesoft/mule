/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.data.sample.SampleDataProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.module.extension.api.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.data.sample.SampleDataProviderFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.sdk.api.data.sample.SampleDataProvider;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Private {@link ModelProperty} which communicates the {@link SampleDataProvider} of a {@link HasOutputModel} model which
 * contains a {@link SampleDataProviderModel}
 *
 * @since 4.4.0
 */
public final class SampleDataProviderFactoryModelProperty implements ModelProperty {

  private final Field connectionField;
  private final Field configField;
  private final Class<? extends SampleDataProvider> sampleDataProvider;
  private final List<InjectableParameterInfo> injectableParameters;

  /**
   * @param sampleDataProvider   the {@link SampleDataProvider} class.
   * @param injectableParameters the parameters that should be injected inside the {@link SampleDataProvider} to be able to
   *                             resolve the sample data
   * @param connectionField      the field inside the {@link SampleDataProvider} which is considered as a connection
   * @param configField          the field inside the {@link SampleDataProvider} which is considered as a configuration
   */
  private SampleDataProviderFactoryModelProperty(Class<? extends SampleDataProvider> sampleDataProvider,
                                                 List<InjectableParameterInfo> injectableParameters,
                                                 Field connectionField,
                                                 Field configField) {
    requireNonNull(sampleDataProvider, "SampleDataProvider Class parameter can't be null");
    requireNonNull(injectableParameters, "injectableParameters parameter can't be null");

    this.sampleDataProvider = sampleDataProvider;
    this.injectableParameters = injectableParameters;
    this.connectionField = connectionField;
    this.configField = configField;
  }

  /**
   * Creates a new builder to be able to easily build a {@link SampleDataProviderFactoryModelProperty}
   *
   * @param sampleDataProvider the {@link Class} of a {@link SampleDataProvider} implementation
   * @return a new {@link SampleDataProviderFactoryModelPropertyBuilder}
   */
  public static SampleDataProviderFactoryModelPropertyBuilder builder(Class<? extends SampleDataProvider> sampleDataProvider) {
    return new SampleDataProviderFactoryModelPropertyBuilder(sampleDataProvider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "SampleDataProviderFactory";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPublic() {
    return false;
  }

  /**
   * @return the class of the {@link SampleDataProvider} implementation
   */
  public <T, A> Class<? extends SampleDataProvider<T, A>> getSampleDataProviderClass() {
    return (Class<? extends SampleDataProvider<T, A>>) sampleDataProvider;
  }

  /**
   * @return The list of parameter names that are considered as required.
   */
  public List<String> getRequiredParameters() {
    return injectableParameters
        .stream()
        .map(InjectableParameterInfo::getParameterName)
        .collect(toList());
  }

  /**
   * @return The {@link List} of parameters that requires to be injected into the {@link SampleDataProvider}
   */
  public List<InjectableParameterInfo> getInjectableParameters() {
    return injectableParameters;
  }

  /**
   * @return Indicates if the {@link SampleDataProvider} requires a connection to resolve the data
   */
  public boolean usesConnection() {
    return connectionField != null;
  }

  /**
   * @return Indicates if the {@link SampleDataProvider} requires a configuration to resolve the data
   */
  public boolean usesConfig() {
    return configField != null;
  }

  public SampleDataProviderFactory createFactory(ParameterValueResolver parameterValueResolver,
                                                 Supplier<Object> connectionSupplier,
                                                 Supplier<Object> configurationSupplier,
                                                 ReflectionCache reflectionCache,
                                                 ExpressionManager expressionManager,
                                                 Injector injector,
                                                 ParameterizedModel parameterizedModel) {
    return new SampleDataProviderFactory(
                                         this,
                                         parameterValueResolver,
                                         connectionSupplier,
                                         configurationSupplier,
                                         connectionField,
                                         configField,
                                         reflectionCache,
                                         expressionManager,
                                         injector,
                                         parameterizedModel);
  }

  /**
   * Builder to easily a {@link SampleDataProviderFactoryModelProperty}
   *
   * @since 4.4.0
   */
  public static class SampleDataProviderFactoryModelPropertyBuilder {

    private final Class<? extends SampleDataProvider> sampleDataProvider;
    private final List<InjectableParameterInfo> injectableParameters;
    private Field connectionField;
    private Field configField;

    public SampleDataProviderFactoryModelPropertyBuilder(Class<? extends SampleDataProvider> sampleDataProvider) {
      this.sampleDataProvider = sampleDataProvider;
      this.injectableParameters = new ArrayList<>();
    }

    public SampleDataProviderFactoryModelPropertyBuilder withInjectableParameter(String name,
                                                                                 MetadataType metadataType,
                                                                                 boolean isRequired) {
      return withInjectableParameter(name, metadataType, isRequired, name);
    }

    public SampleDataProviderFactoryModelPropertyBuilder withInjectableParameter(String name,
                                                                                 MetadataType metadataType,
                                                                                 boolean isRequired,
                                                                                 String extractionExpression) {
      injectableParameters.add(new InjectableParameterInfo(name, metadataType, isRequired, extractionExpression));
      return this;
    }

    public void withConnection(Field connectionField) {
      this.connectionField = connectionField;
    }

    public void withConfig(Field configField) {
      this.configField = configField;
    }

    public SampleDataProviderFactoryModelProperty build() {
      return new SampleDataProviderFactoryModelProperty(
                                                        sampleDataProvider,
                                                        injectableParameters,
                                                        connectionField,
                                                        configField);
    }
  }
}
