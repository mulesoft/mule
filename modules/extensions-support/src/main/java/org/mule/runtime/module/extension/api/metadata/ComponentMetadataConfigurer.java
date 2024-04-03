/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.metadata;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.metadata.internal.NullMetadataResolverSupplier.NULL_METADATA_RESOLVER;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.getCategoryName;

import static java.util.Collections.unmodifiableMap;
import static java.util.Optional.ofNullable;

import org.mule.api.annotation.Experimental;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclarer;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.metadata.internal.DefaultMetadataResolverFactory;
import org.mule.runtime.metadata.internal.NullMetadataResolverFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.metadata.chain.AllOfRoutesOutputTypeResolver;
import org.mule.runtime.module.extension.internal.metadata.chain.OneOfRoutesOutputTypeResolver;
import org.mule.runtime.module.extension.internal.metadata.chain.PassThroughChainOutputTypeResolver;
import org.mule.sdk.api.annotation.MinMuleVersion;
import org.mule.sdk.api.metadata.resolving.ChainInputTypeResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Declarative API for configuring DataSense related resolvers at the declarer or declaration level with minimal exposure
 * to the implementation details.
 *
 * <b>NOTE:</b> Experimental feature. Backwards compatibility is not guaranteed.
 *
 * @since 4.7.0
 */
@MinMuleVersion("4.7.0")
@Experimental
public final class ComponentMetadataConfigurer {

  private TypeKeysResolver keysResolver = NULL_METADATA_RESOLVER;
  private String keyParameterName;
  private MetadataType keyParameterType;
  private boolean hasPartialKeyResolver = false;

  private OutputTypeResolver outputTypeResolver = NULL_METADATA_RESOLVER;
  private AttributesTypeResolver attributesTypeResolver = NULL_METADATA_RESOLVER;

  private final Map<String, Supplier<? extends InputTypeResolver>> inputResolvers = new HashMap<>();
  private final Map<String, String> inputResolverNames = new HashMap<>();
  private String firstSeenInputResolverCategory = null;

  private ChainInputTypeResolver chainInputTypeResolver;
  private final Map<String, ChainInputTypeResolver> routesChainInputTypesResolvers = new HashMap<>();

  /**
   * Configures the given {@code declaration} with resolvers that implement the {@code Null-Object} design pattern. That is,
   * the declaration will get enriched with resolver instances that yield default (non necessarily useful) values.
   *
   * @param declaration a component's declaration object
   */
  public static <T extends ParameterizedDeclaration> void configureNullMetadata(ParameterizedDeclaration<T> declaration) {
    declaration.addModelProperty(new MetadataResolverFactoryModelProperty(() -> new NullMetadataResolverFactory()));
  }

  /**
   * Configures the given {@code declarer} with resolvers that implement the {@code Null-Object} design pattern. That is,
   * the declaration will get enriched with resolver instances that yield default (non necessarily useful) values.
   *
   * @param declarer a component's declarer object
   */
  public static <T extends ParameterizedDeclarer, D extends ParameterizedDeclaration> void configureNullMetadata(ParameterizedDeclarer<T, D> declarer) {
    configureNullMetadata(declarer.getDeclaration());
  }

  /**
   * Sets an {@link OutputTypeResolver}
   *
   * @param outputTypeResolver the configured resolver
   * @return {@code this} instance
   * @throws IllegalArgumentException if {@code outputTypeResolver} is {@code null}
   */
  public ComponentMetadataConfigurer setOutputTypeResolver(OutputTypeResolver outputTypeResolver) {
    checkArgument(outputTypeResolver != null, "outputTypeResolver cannot be null");
    this.outputTypeResolver = outputTypeResolver;

    return this;
  }

  /**
   * Sets a {@link AttributesTypeResolver}
   *
   * @param attributesTypeResolver the configured resolver
   * @return {@code this} instance
   * @throws IllegalArgumentException if {@code attributesTypeResolver} is {@code null}
   */
  public ComponentMetadataConfigurer setAttributesTypeResolver(AttributesTypeResolver attributesTypeResolver) {
    checkArgument(attributesTypeResolver != null, "attributesTypeResolver cannot be null");
    this.attributesTypeResolver = attributesTypeResolver;

    return this;
  }

  /**
   * Sets a {@link TypeKeysResolver}
   *
   * @param keysResolver         the configured resolver
   * @param keyParameterName     the name of the parameter acting as the metadata key
   * @param keyParameterType     the type of the parameter referenced by the {@code keyParameterName} argument
   * @param isPartialKeyResolver whether this resolver is a partial key resolver
   * @return {@code this} instance
   * @throws IllegalArgumentException if {@code keysResolver} or {@code keyParameterType} are {@code null}, or {@code keyParameterName} is blank
   */
  public ComponentMetadataConfigurer setKeysResolver(TypeKeysResolver keysResolver,
                                                     String keyParameterName,
                                                     MetadataType keyParameterType,
                                                     boolean isPartialKeyResolver) {
    checkArgument(keysResolver != null, "keysResolver cannot be null");
    checkArgument(!isBlank(keyParameterName), "keyParameterName cannot be blank");
    checkArgument(keyParameterType != null, "keyParameterType cannot be null");

    this.keysResolver = keysResolver;
    this.keyParameterName = keyParameterName;
    this.keyParameterType = keyParameterType;
    this.hasPartialKeyResolver = isPartialKeyResolver;

    return this;
  }

  /**
   * Sets a {@link ChainInputTypeResolver}. Only use when configuring a scope component.
   *
   * @param chainInputTypeResolver the configured resolver
   * @return {@code this} instance
   */
  public ComponentMetadataConfigurer setChainInputTypeResolver(ChainInputTypeResolver chainInputTypeResolver) {
    this.chainInputTypeResolver = chainInputTypeResolver;
    return this;
  }

  /**
   * Adds an {@link InputTypeResolver} for a specific input parameter
   *
   * @param parameterName the resolved parameter name
   * @param resolver      the configured resolver
   * @return {@code this} instance
   * @throws IllegalArgumentException if {@code parameterName} is blank or {@code resolver} is {@code null}
   */
  public ComponentMetadataConfigurer addInputResolver(String parameterName, InputTypeResolver resolver) {
    checkArgument(!isBlank(parameterName), "parameterName cannot be blank");
    checkArgument(resolver != null, "resolver cannot be null");

    inputResolvers.put(parameterName, () -> resolver);
    inputResolverNames.put(parameterName, resolver.getResolverName());
    if (firstSeenInputResolverCategory == null) {
      firstSeenInputResolverCategory = resolver.getCategoryName();
    }
    return this;
  }

  /**
   * Invokes {@link #addInputResolver(String, InputTypeResolver)} per each entry in {@code resolvers}
   *
   * @param resolvers the resolvers to add.
   * @return {@code this} instance
   * @throws IllegalArgumentException under any of the circumstances that {@link #addInputResolver(String, InputTypeResolver)} would
   */
  public ComponentMetadataConfigurer addInputResolvers(Map<String, InputTypeResolver> resolvers) {
    checkArgument(resolvers != null, "resolvers cannot be null");
    resolvers.forEach(this::addInputResolver);

    return this;
  }

  /**
   * Adds a {@link ChainInputTypeResolver} for a specific route. Only use when configuring router components
   *
   * @param routeName the route name
   * @param resolver  the resolver being set
   * @return {@code this} instance
   * @throws IllegalArgumentException if {@code routeName} is blank or {@code resolver} is {@code null}
   */
  public ComponentMetadataConfigurer addRouteChainInputResolver(String routeName, ChainInputTypeResolver resolver) {
    checkArgument(!isBlank(routeName), "routeName cannot be blank");
    checkArgument(resolver != null, "resolver cannot be null");

    routesChainInputTypesResolvers.put(routeName, resolver);
    return this;
  }

  /**
   * Invokes {@link #addRouteChainInputResolver(String, ChainInputTypeResolver)} per each entry in {@code resolvers}
   *
   * @param resolvers the resolvers to add.
   * @return {@code this} instance
   * @throws IllegalArgumentException under any of the circumstances that {@link #addRouteChainInputResolver(String, ChainInputTypeResolver) would
   */
  public ComponentMetadataConfigurer addRoutesChainInputResolvers(Map<String, ChainInputTypeResolver> resolvers) {
    checkArgument(resolvers != null, "resolvers cannot be null");

    resolvers.forEach(this::addRouteChainInputResolver);
    return this;
  }

  /**
   * Convenience method to configure routers that will output the result of (any) one of its routes. An example of
   * such a router would be {@code <first-successful>}
   *
   * @return {@code this} instance
   */
  public ComponentMetadataConfigurer asOneOfRouter() {
    setOutputTypeResolver(OneOfRoutesOutputTypeResolver.INSTANCE);
    setAttributesTypeResolver(OneOfRoutesOutputTypeResolver.INSTANCE);

    return this;
  }

  /**
   * Convenience method to configure a scope that outputs the result of its inner chain. An example of such a scope
   * would be {@code <try>}
   *
   * @return {@code this} instance
   */
  public ComponentMetadataConfigurer asPassthroughScope() {
    setOutputTypeResolver(PassThroughChainOutputTypeResolver.INSTANCE);
    setAttributesTypeResolver(PassThroughChainOutputTypeResolver.INSTANCE);

    return this;
  }

  /**
   * Convenience method to configure routers that return the sum of all its routes, in the form of an object which
   * attributes matches the route names (e.g: {@code <scatter-gather>}
   *
   * @return {@code this} instance
   */
  public ComponentMetadataConfigurer asAllOfRouter() {
    setOutputTypeResolver(AllOfRoutesOutputTypeResolver.INSTANCE);
    return this;
  }

  /**
   * Applies the configuration on {@code this} instance on the given {@code declarer}.
   *
   * @param declarer the target declarer
   */
  public <T extends ParameterizedDeclarer, D extends ParameterizedDeclaration> void configure(ParameterizedDeclarer<T, D> declarer) {
    configure(declarer.getDeclaration());
  }

  /**
   * Applies the configuration on {@code this} instance on the given {@code declaration}.
   *
   * @param declaration the target declaration
   */
  public <T extends ComponentDeclaration> void configure(ParameterizedDeclaration<T> declaration) {
    declaration.addModelProperty(buildFactoryModelProperty());
    declaration.addModelProperty(buildResolverInformationModelProperty(declaration));

    if (keysResolver != NULL_METADATA_RESOLVER) {
      declaration.addModelProperty(new MetadataKeyIdModelProperty(keyParameterType, keyParameterName,
          getCategoryName(keysResolver, firstSeenInputResolverCategory,
              outputTypeResolver)));
    }
  }

  private MetadataResolverFactoryModelProperty buildFactoryModelProperty() {
    return new MetadataResolverFactoryModelProperty(() -> new DefaultMetadataResolverFactory(
        () -> keysResolver,
        copy(inputResolvers),
        () -> outputTypeResolver,
        () -> attributesTypeResolver,
        ofNullable(chainInputTypeResolver),
        copy(routesChainInputTypesResolvers)));
  }

  private TypeResolversInformationModelProperty buildResolverInformationModelProperty(ParameterizedDeclaration declaration) {
    String categoryName = getCategoryName(keysResolver, firstSeenInputResolverCategory, outputTypeResolver);
    boolean connected = declaration instanceof ExecutableComponentDeclaration
        ? ((ExecutableComponentDeclaration<?>) declaration).isRequiresConnection()
        : false;

    return new TypeResolversInformationModelProperty(
        categoryName,
        copy(inputResolverNames),
        outputTypeResolver.getResolverName(),
        attributesTypeResolver.getResolverName(),
        keysResolver.getResolverName(),
        connected,
        connected,
        hasPartialKeyResolver);
  }

  private <K, V> Map<K, V> copy(Map<K, V> map) {
    return unmodifiableMap(new HashMap<>(map));
  }
}
