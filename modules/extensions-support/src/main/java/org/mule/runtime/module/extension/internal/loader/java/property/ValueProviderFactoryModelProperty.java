/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.Preconditions.checkNotNull;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.value.ValueProviderFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Private {@link ModelProperty} which communicates the {@link ValueProvider} of a parameter or parameter
 * model which contains a {@link ValueProviderModel} indicating that provides a {@link Set} of {@link Value values}
 *
 * @since 4.0
 */
public final class ValueProviderFactoryModelProperty implements ModelProperty {

  private final Field connectionField;
  private final Field configField;
  private final Class<? extends ValueProvider> valuesProvider;
  private final List<InjectableParameterInfo> injectableParameters;

  /**
   * @param valueProvider        the {@link ValueProvider} class.
   * @param injectableParameters the parameters that should be injected inside the {@link ValueProvider} to be able
   *                             to resolve the {@link Value values}
   * @param connectionField      the field inside the {@link ValueProvider} which is considered as a connection
   * @param configField          the field inside the {@link ValueProvider} which is considered as a configuration
   */
  private ValueProviderFactoryModelProperty(Class<? extends ValueProvider> valueProvider,
                                            List<InjectableParameterInfo> injectableParameters,
                                            Field connectionField,
                                            Field configField) {
    checkNotNull(valueProvider, "Values Provider Class parameter can't be null");
    checkNotNull(injectableParameters, "injectableParameters parameter can't be null");

    this.valuesProvider = valueProvider;
    this.injectableParameters = injectableParameters;
    this.connectionField = connectionField;
    this.configField = configField;
  }

  /**
   * Creates a new builder to be able to easily build a {@link ValueProviderFactoryModelProperty}
   * @param valuesProvider the {@link Class} of a {@link ValueProvider} implementation
   *
   * @return a new {@link ValueProviderFactoryModelPropertyBuilder}
   */
  public static ValueProviderFactoryModelPropertyBuilder builder(Class<? extends ValueProvider> valuesProvider) {
    return new ValueProviderFactoryModelPropertyBuilder(valuesProvider);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "ValueProviderFactory";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isPublic() {
    return false;
  }

  /**
   * @return the class of the {@link ValueProvider} implementation
   */
  public Class<? extends ValueProvider> getValueProvider() {
    return valuesProvider;
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
   * @return The {@link List} of parameters that requires to be inyected into the {@link ValueProvider}
   */
  public List<InjectableParameterInfo> getInjectableParameters() {
    return injectableParameters;
  }

  /**
   * @return Indicates if the {@link ValueProvider} requires a connection to resolve the {@link Value values}
   */
  public boolean usesConnection() {
    return connectionField != null;
  }

  /**
   * @return Indicates if the {@link ValueProvider} requires a configuration to resolve the {@link Value values}
   */
  public boolean usesConfig() {
    return configField != null;
  }

  public ValueProviderFactory createFactory(ParameterValueResolver parameterValueResolver, Supplier<Object> connectionSupplier,
                                            Supplier<Object> configurationSupplier, MuleContext muleContext) {
    return new ValueProviderFactory(this, parameterValueResolver, connectionSupplier, configurationSupplier, connectionField,
                                    configField, muleContext);
  }

  /**
   * Builder to easily a {@link ValueProviderFactoryModelProperty}
   *
   * @since 4.0
   */
  public static class ValueProviderFactoryModelPropertyBuilder {

    private final Class<? extends ValueProvider> dynamicOptionsResolver;
    private final List<InjectableParameterInfo> injectableParameters;
    private Field connectionField;
    private Field configField;

    ValueProviderFactoryModelPropertyBuilder(Class<? extends ValueProvider> dynamicOptionsResolver) {
      this.dynamicOptionsResolver = dynamicOptionsResolver;
      this.injectableParameters = new ArrayList<>();
    }

    public ValueProviderFactoryModelPropertyBuilder withInjectableParameter(String name, MetadataType metadataType,
                                                                            boolean isRequired) {
      injectableParameters.add(new InjectableParameterInfo(name, metadataType, isRequired));
      return this;
    }

    public ValueProviderFactoryModelProperty build() {
      return new ValueProviderFactoryModelProperty(dynamicOptionsResolver, injectableParameters,
                                                   connectionField, configField);
    }

    public void withConnection(Field connectionField) {
      this.connectionField = connectionField;
    }

    public void withConfig(Field configField) {
      this.configField = configField;
    }
  }

  public static class InjectableParameterInfo {

    String parameterName;
    MetadataType type;
    boolean required;

    InjectableParameterInfo(String parameterName, MetadataType type, boolean required) {
      this.parameterName = parameterName;
      this.type = type;
      this.required = required;
    }

    public String getParameterName() {
      return parameterName;
    }

    public void setParameterName(String parameterName) {
      this.parameterName = parameterName;
    }

    public MetadataType getType() {
      return type;
    }

    public void setType(MetadataType type) {
      this.type = type;
    }

    public boolean isRequired() {
      return required;
    }

    public void setRequired(boolean required) {
      this.required = required;
    }
  }
}
