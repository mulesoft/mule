/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.metadata;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.metadata.internal.DefaultMetadataResolverFactory;
import org.mule.runtime.metadata.internal.NullMetadataResolverFactory;
import org.mule.runtime.metadata.internal.NullMetadataResolverSupplier;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.metadata.chain.AllOfRoutesOutputTypeResolver;
import org.mule.runtime.module.extension.internal.metadata.chain.OneOfRoutesOutputTypeResolver;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ComponentMetadataConfigurer {

  private Supplier<? extends OutputTypeResolver> outputTypeResolver = NullMetadataResolverSupplier.INSTANCE;
  private Supplier<? extends AttributesTypeResolver> attributesTypeResolver = NullMetadataResolverSupplier.INSTANCE;
  private Supplier<? extends TypeKeysResolver> keysResolverSupplier = NullMetadataResolverSupplier.INSTANCE;
  private ChainInputTypeResolver chainInputTypeResolver;
  private final Map<String, Supplier<? extends InputTypeResolver>> inputResolvers = new HashMap<>();
  private final Map<String, ChainInputTypeResolver> routesChainInputTypesResolvers = new HashMap<>();

  public static <T extends BaseDeclaration> void configureNullMetadata(BaseDeclaration<T> declaration) {
    declaration.addModelProperty(getNullFactoryModelProperty());
  }

  public static <T> void configureNullMetadata(HasModelProperties<T> declarer) {
    declarer.withModelProperty(getNullFactoryModelProperty());
  }

  private static MetadataResolverFactoryModelProperty getNullFactoryModelProperty() {
    return new MetadataResolverFactoryModelProperty(() -> new NullMetadataResolverFactory());
  }

  public ComponentMetadataConfigurer setOutputTypeResolver(Supplier<? extends OutputTypeResolver> outputTypeResolver) {
    checkArgument(outputTypeResolver != null, "outputTypeResolver cannot be null");
    this.outputTypeResolver = outputTypeResolver;

    return this;
  }

  public ComponentMetadataConfigurer setOutputTypeResolver(OutputTypeResolver outputTypeResolver) {
    checkArgument(outputTypeResolver != null, "outputTypeResolver cannot be null");
    this.outputTypeResolver = () -> outputTypeResolver;

    return this;
  }

  public ComponentMetadataConfigurer setAttributesTypeResolver(Supplier<? extends AttributesTypeResolver> attributesTypeResolver) {
    checkArgument(attributesTypeResolver != null, "attributesTypeResolver cannot be null");
    this.attributesTypeResolver = attributesTypeResolver;

    return this;
  }

  public ComponentMetadataConfigurer setAttributesTypeResolver(AttributesTypeResolver attributesTypeResolver) {
    checkArgument(attributesTypeResolver != null, "attributesTypeResolver cannot be null");
    this.attributesTypeResolver = () -> attributesTypeResolver;

    return this;
  }

  public ComponentMetadataConfigurer setKeysResolver(TypeKeysResolver keysResolver) {
    this.keysResolverSupplier = () -> keysResolver;
    return this;
  }

  public ComponentMetadataConfigurer setKeysResolver(Supplier<TypeKeysResolver> keysResolverSupplier) {
    this.keysResolverSupplier = keysResolverSupplier;
    return this;
  }

  public ComponentMetadataConfigurer setChainInputTypeResolver(ChainInputTypeResolver chainInputTypeResolver) {
    this.chainInputTypeResolver = chainInputTypeResolver;
    return this;
  }

  public ComponentMetadataConfigurer addInputResolver(String parameterName, Supplier<? extends InputTypeResolver> resolver) {
    checkArgument(!isBlank(parameterName), "parameterName cannot be blank");
    checkArgument(resolver != null, "resolver cannot be null");

    inputResolvers.put(parameterName, resolver);
    return this;
  }

  public ComponentMetadataConfigurer addInputResolvers(Map<String, Supplier<? extends InputTypeResolver>> resolvers) {
    checkArgument(resolvers != null, "resolvers cannot be null");
    resolvers.forEach(this::addInputResolver);

    return this;
  }

  public ComponentMetadataConfigurer addRouteChainInputResolver(String routeName, ChainInputTypeResolver resolver) {
    checkArgument(!isBlank(routeName), "routeName cannot be blank");
    checkArgument(resolver != null, "resolver cannot be null");

    routesChainInputTypesResolvers.put(routeName, resolver);
    return this;
  }

  public ComponentMetadataConfigurer addRoutesChainInputResolvers(Map<String, ChainInputTypeResolver> resolvers) {
    checkArgument(resolvers != null, "resolvers cannot be null");

    resolvers.forEach(this::addRouteChainInputResolver);
    return this;
  }

  public ComponentMetadataConfigurer asOneOfRouter() {
    setOutputTypeResolver(OneOfRoutesOutputTypeResolver.INSTANCE);
    setAttributesTypeResolver(OneOfRoutesOutputTypeResolver.INSTANCE);

    return this;
  }

  public ComponentMetadataConfigurer asPassthroughScope() {
    setOutputTypeResolver(OneOfRoutesOutputTypeResolver.INSTANCE);
    setAttributesTypeResolver(OneOfRoutesOutputTypeResolver.INSTANCE);

    return this;
  }

  public ComponentMetadataConfigurer asAllOfRouter() {
    setOutputTypeResolver(AllOfRoutesOutputTypeResolver.INSTANCE);
    return this;
  }

  public <T> void configure(HasModelProperties<T> declarer) {
    declarer.withModelProperty(buildFactoryModelProperty());
  }

  public <T extends BaseDeclaration> void configure(BaseDeclaration<T> declaration) {
    declaration.addModelProperty(buildFactoryModelProperty());
  }

  private MetadataResolverFactoryModelProperty buildFactoryModelProperty() {
    return new MetadataResolverFactoryModelProperty(() -> new DefaultMetadataResolverFactory(
                                                                                             keysResolverSupplier,
                                                                                             inputResolvers,
                                                                                             outputTypeResolver,
                                                                                             attributesTypeResolver,
                                                                                             ofNullable(chainInputTypeResolver),
                                                                                             unmodifiableMap(new LinkedHashMap<>(routesChainInputTypesResolvers))));
  }
}
