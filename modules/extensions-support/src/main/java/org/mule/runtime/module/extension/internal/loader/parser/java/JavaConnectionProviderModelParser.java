/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.CACHED;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.NONE;
import static org.mule.runtime.api.meta.model.connection.ConnectionManagementType.POOLING;
import static org.mule.runtime.extension.privileged.semantic.SemanticTermsHelper.getConnectionTermsFromAnnotations;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getParameterGroupParsers;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext.forConnectionProvider;
import static org.mule.runtime.module.extension.internal.loader.parser.java.connection.JavaConnectionProviderModelParserUtils.isCachedConnectionProvider;
import static org.mule.runtime.module.extension.internal.loader.parser.java.connection.JavaConnectionProviderModelParserUtils.isDefinedThroughSdkApi;
import static org.mule.runtime.module.extension.internal.loader.parser.java.connection.JavaConnectionProviderModelParserUtils.isPoolingConnectionProvider;
import static org.mule.runtime.module.extension.internal.loader.parser.java.connection.SdkCredentialPlacementUtils.from;
import static org.mule.runtime.module.extension.internal.loader.parser.java.lib.JavaExternalLibModelParserUtils.parseExternalLibraryModels;
import static org.mule.runtime.module.extension.internal.loader.parser.java.semantics.SemanticTermsParserUtils.addCustomTerms;
import static org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.JavaStereotypeModelParserUtils.resolveStereotype;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.MinMuleVersionUtils.resolveConnectionProviderMinMuleVersion;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedFieldsStream;

import static java.lang.String.format;
import static java.util.Objects.hash;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.AuthorizationCode;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.ClientCredentials;
import org.mule.runtime.extension.api.annotation.connectivity.oauth.OAuthCallbackValue;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthGrantType;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthModelProperty;
import org.mule.runtime.extension.api.exception.IllegalConnectionProviderModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.DefaultConnectionProviderFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ConnectionTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.oauth.OAuthCallbackValuesModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion;
import org.mule.sdk.api.annotation.semantics.connectivity.ExcludeFromConnectivitySchema;
import org.mule.sdk.api.connectivity.NoConnectivityTest;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ConnectionProviderModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaConnectionProviderModelParser implements ConnectionProviderModelParser {

  private static final String CONNECTION_PROVIDER_NAME = "connection provider";

  private final JavaExtensionModelParser extensionModelParser;
  private final ConnectionProviderElement element;
  private final ExtensionLoadingContext loadingContext;
  private final List<ModelProperty> additionalModelProperties = new LinkedList<>();
  private final ClassLoader extensionClassLoader;

  public JavaConnectionProviderModelParser(JavaExtensionModelParser extensionModelParser,
                                           ExtensionElement extensionElement,
                                           ConnectionProviderElement element,
                                           ExtensionLoadingContext loadingContext) {
    this.extensionModelParser = extensionModelParser;
    this.element = element;
    this.loadingContext = loadingContext;
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
    return getParameterGroupParsers(element.getParameters(), forConnectionProvider(getName(), loadingContext));
  }

  @Override
  public List<ExternalLibraryModel> getExternalLibraryModels() {
    return parseExternalLibraryModels(element);
  }

  @Override
  public ConnectionManagementType getConnectionManagementType() {
    ConnectionManagementType managementType = NONE;
    if (isPoolingConnectionProvider(element)) {
      managementType = POOLING;
    } else if (isCachedConnectionProvider(element)) {
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
    return !element.isAssignableTo(NoConnectivityTest.class)
        && !element.isAssignableTo(org.mule.runtime.extension.api.connectivity.NoConnectivityTest.class);
  }

  @Override
  public boolean isExcludedFromConnectivitySchema() {
    return element.isAnnotatedWith(ExcludeFromConnectivitySchema.class);
  }

  @Override
  public Optional<OAuthModelProperty> getOAuthModelProperty() {
    List<OAuthGrantType> grantTypes = new LinkedList<>();
    mapReduceSingleAnnotation(element, CONNECTION_PROVIDER_NAME, element.getName(), AuthorizationCode.class,
                              org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode.class,
                              this::getAuthorizationCodeGrantType,
                              this::getAuthorizationCodeGrantTypeFromSdk)
        .ifPresent(grantTypes::add);

    mapReduceSingleAnnotation(element, CONNECTION_PROVIDER_NAME, element.getName(), ClientCredentials.class,
                              org.mule.sdk.api.annotation.connectivity.oauth.ClientCredentials.class,
                              this::getClientCredentialsGrantType,
                              this::getClientCredentialsGrantTypeFromSdk)
        .ifPresent(grantTypes::add);

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
    Class<?> baseInterface = isDefinedThroughSdkApi(element)
        ? org.mule.sdk.api.connectivity.ConnectionProvider.class
        : ConnectionProvider.class;

    List<Type> providerGenerics = element.getSuperTypeGenerics(baseInterface);

    if (providerGenerics.size() != 1) {
      // TODO: MULE-9220: Add a syntax validator for this
      throw new IllegalConnectionProviderModelDefinitionException(
                                                                  format("Connection provider class '%s' was expected to have 1 generic type "
                                                                      + "(for the connection type) but %d were found",
                                                                         element.getName(), providerGenerics.size()));
    }

    additionalModelProperties.add(new ConnectionTypeModelProperty(providerGenerics.get(0)));
    element.getDeclaringClass().ifPresent(clazz -> additionalModelProperties.add(new ImplementingTypeModelProperty(clazz)));
    element.getDeclaringClass()
        .ifPresent(clazz -> getOAuthCallbackValuesModelProperty(clazz).ifPresent(additionalModelProperties::add));
    additionalModelProperties.add(new ExtensionTypeDescriptorModelProperty(element));
  }

  private Optional<OAuthCallbackValuesModelProperty> getOAuthCallbackValuesModelProperty(Class<?> clazz) {
    Map<Field, String> values = getAnnotatedFieldsStream(clazz, OAuthCallbackValue.class)
        .collect(toMap(identity(), field -> field.getAnnotation(OAuthCallbackValue.class).expression()));

    values.putAll(getAnnotatedFieldsStream(clazz, org.mule.sdk.api.annotation.connectivity.oauth.OAuthCallbackValue.class)
        .collect(toMap(identity(), field -> field
            .getAnnotation(org.mule.sdk.api.annotation.connectivity.oauth.OAuthCallbackValue.class).expression())));

    if (!values.isEmpty()) {
      return of(new OAuthCallbackValuesModelProperty(values));
    }
    return empty();
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
  public Optional<ResolvedMinMuleVersion> getResolvedMinMuleVersion() {
    return of(resolveConnectionProviderMinMuleVersion(element));
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

  private AuthorizationCodeGrantType getAuthorizationCodeGrantType(AnnotationValueFetcher<AuthorizationCode> authorizationCodeAnnotationValueFetcher) {
    return new AuthorizationCodeGrantType(
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(AuthorizationCode::accessTokenUrl),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(AuthorizationCode::authorizationUrl),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(AuthorizationCode::accessTokenExpr),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(AuthorizationCode::expirationExpr),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(AuthorizationCode::refreshTokenExpr),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(AuthorizationCode::defaultScopes),
                                          authorizationCodeAnnotationValueFetcher
                                              .getEnumValue(AuthorizationCode::credentialsPlacement),
                                          authorizationCodeAnnotationValueFetcher
                                              .getBooleanValue(AuthorizationCode::includeRedirectUriInRefreshTokenRequest));
  }

  private AuthorizationCodeGrantType getAuthorizationCodeGrantTypeFromSdk(AnnotationValueFetcher<org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode> authorizationCodeAnnotationValueFetcher) {
    return new AuthorizationCodeGrantType(
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode::accessTokenUrl),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode::authorizationUrl),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode::accessTokenExpr),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode::expirationExpr),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode::refreshTokenExpr),
                                          authorizationCodeAnnotationValueFetcher
                                              .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode::defaultScopes),
                                          authorizationCodeAnnotationValueFetcher
                                              .getEnumValue(authorizationCode -> from(authorizationCode
                                                  .credentialsPlacement())),
                                          authorizationCodeAnnotationValueFetcher
                                              .getBooleanValue(org.mule.sdk.api.annotation.connectivity.oauth.AuthorizationCode::includeRedirectUriInRefreshTokenRequest));
  }

  private ClientCredentialsGrantType getClientCredentialsGrantType(AnnotationValueFetcher<ClientCredentials> clientCredentialsAnnotationValueFetcher) {
    return new ClientCredentialsGrantType(clientCredentialsAnnotationValueFetcher
        .getStringValue(ClientCredentials::tokenUrl),
                                          clientCredentialsAnnotationValueFetcher
                                              .getStringValue(ClientCredentials::accessTokenExpr),
                                          clientCredentialsAnnotationValueFetcher
                                              .getStringValue(ClientCredentials::expirationExpr),
                                          clientCredentialsAnnotationValueFetcher
                                              .getStringValue(ClientCredentials::defaultScopes),
                                          clientCredentialsAnnotationValueFetcher
                                              .getEnumValue(ClientCredentials::credentialsPlacement));
  }

  private ClientCredentialsGrantType getClientCredentialsGrantTypeFromSdk(AnnotationValueFetcher<org.mule.sdk.api.annotation.connectivity.oauth.ClientCredentials> clientCredentialsAnnotationValueFetcher) {
    return new ClientCredentialsGrantType(clientCredentialsAnnotationValueFetcher
        .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.ClientCredentials::tokenUrl),
                                          clientCredentialsAnnotationValueFetcher
                                              .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.ClientCredentials::accessTokenExpr),
                                          clientCredentialsAnnotationValueFetcher
                                              .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.ClientCredentials::expirationExpr),
                                          clientCredentialsAnnotationValueFetcher
                                              .getStringValue(org.mule.sdk.api.annotation.connectivity.oauth.ClientCredentials::defaultScopes),
                                          clientCredentialsAnnotationValueFetcher
                                              .getEnumValue(clientCredentials -> from(clientCredentials
                                                  .credentialsPlacement())));
  }

}
