/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.CACHED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.connectivity.internal.platform.schema.SemanticTermsHelper.getConnectionTermsFromAnnotations;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext.forConnectionProvider;
import static org.mule.runtime.module.extension.internal.loader.parser.java.lib.JavaExternalLibModelParserUtils.parseExternalLibraryModels;
import static org.mule.runtime.module.extension.internal.loader.parser.java.semantics.SemanticTermsParserUtils.addCustomTerms;
import static org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.JavaStereotypeModelParserUtils.resolveStereotype;

import org.mule.runtime.api.connection.CachedConnectionProvider;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.ClientCredentials;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.DefaultConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.sdk.api.annotation.semantics.connectivity.ExcludeFromConnectivitySchema;
import org.mule.sdk.api.connectivity.NoConnectivityTest;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ConnectionProviderModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaConnectionProviderModelParser implements ConnectionProviderModelParser {

  private final JavaExtensionModelParser extensionModelParser;
  private final ConnectionProviderElement element;
  private final List<ModelProperty> additionalModelProperties = new LinkedList<>();
  private final ClassLoader extensionClassLoader;

  public JavaConnectionProviderModelParser(JavaExtensionModelParser extensionModelParser,
                                           ExtensionElement extensionElement,
                                           ConnectionProviderElement element) {
    this.extensionModelParser = extensionModelParser;
    this.element = element;
    extensionClassLoader = extensionElement.getDeclaringClass()
        .map(Class::getClassLoader)
        .orElse(ExtensionModel.class.getClassLoader());

    collectAdditionalModelProperties();
  }

  @Override
  public String getName() {
    if (element.getName().equals(element.getAlias())) {
      return DEFAULT_CONNECTION_PROVIDER_NAME;
    }

    return element.getAlias();
  }

  @Override
  public String getDescription() {
    return element.getDescription();
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupModelParsers() {
    return getParameterGroupParsers(element.getParameters(), forConnectionProvider(getName()));
  }

  @Override
  public List<ExternalLibraryModel> getExternalLibraryModels() {
    return parseExternalLibraryModels(element);
  }

  @Override
  public ConnectionManagementType getConnectionManagementType() {
    ConnectionManagementType managementType = NONE;
    if (element.isAssignableTo(PoolingConnectionProvider.class)) {
      managementType = POOLING;
    } else if (element.isAssignableTo(CachedConnectionProvider.class)) {
      managementType = CACHED;
    }

    return managementType;
  }

  @Override
  public Optional<ConnectionProviderFactoryModelProperty> getConnectionProviderFactoryModelProperty() {
    return element.getDeclaringClass()
        .map(declaringClass -> new ConnectionProviderFactoryModelProperty(new DefaultConnectionProviderFactory(
                                                                                                               declaringClass,
                                                                                                               extensionClassLoader)));
  }

  @Override
  public boolean supportsConnectivityTesting() {
    return !element.isAssignableTo(NoConnectivityTest.class);
  }

  @Override
  public boolean isExcludedFromConnectivitySchema() {
    return element.isAnnotatedWith(ExcludeFromConnectivitySchema.class);
  }

  @Override
  public Optional<OAuthModelProperty> getOAuthModelProperty() {
    List<OAuthGrantType> grantTypes = new LinkedList<>();
    element.getAnnotation(AuthorizationCode.class)
        .ifPresent(a -> grantTypes.add(new AuthorizationCodeGrantType(a.accessTokenUrl(),
                                                                      a.authorizationUrl(),
                                                                      a.accessTokenExpr(),
                                                                      a.expirationExpr(),
                                                                      a.refreshTokenExpr(),
                                                                      a.defaultScopes(),
                                                                      a.credentialsPlacement(),
                                                                      a.includeRedirectUriInRefreshTokenRequest())));

    element.getAnnotation(ClientCredentials.class).ifPresent(a -> grantTypes.add(new ClientCredentialsGrantType(a.tokenUrl(),
                                                                                                                a.accessTokenExpr(),
                                                                                                                a.expirationExpr(),
                                                                                                                a.defaultScopes(),
                                                                                                                a.credentialsPlacement()))

    );

    return grantTypes.isEmpty() ? empty() : of(new OAuthModelProperty(grantTypes));
  }

  @Override
  public Optional<StereotypeModel> getStereotype(StereotypeModelFactory factory) {
    return resolveStereotype(element, "Connection Provider", getName(), factory);
  }

  @Override
  public List<ModelProperty> getAdditionalModelProperties() {
    return additionalModelProperties;
  }

  private void collectAdditionalModelProperties() {
    List<Type> providerGenerics = element.getSuperTypeGenerics(ConnectionProvider.class);

    if (providerGenerics.size() != 1) {
      // TODO: MULE-9220: Add a syntax validator for this
      throw new IllegalConnectionProviderModelDefinitionException(
                                                                  format("Connection provider class '%s' was expected to have 1 generic type "
                                                                      + "(for the connection type) but %d were found",
                                                                         element.getName(), providerGenerics.size()));
    }

    additionalModelProperties.add(new ConnectionTypeModelProperty(providerGenerics.get(0)));
    element.getDeclaringClass().ifPresent(clazz -> additionalModelProperties.add(new ImplementingTypeModelProperty(clazz)));
    additionalModelProperties.add(new ExtensionTypeDescriptorModelProperty(element));
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return JavaExtensionModelParserUtils.getDeprecationModel(element);
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return JavaExtensionModelParserUtils.getDisplayModel(element, "connection provider", element.getName());
  }

  @Override
  public Set<String> getSemanticTerms() {
    Set<String> terms = new LinkedHashSet<>();
    terms.addAll(getConnectionTermsFromAnnotations(element::isAnnotatedWith));
    addCustomTerms(element, terms);

    return terms;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof JavaConnectionProviderModelParser) {
      return element.equals(((JavaConnectionProviderModelParser) o).element);
    }

    return false;
  }

  @Override
  public int hashCode() {
    return hash(element);
  }
}
