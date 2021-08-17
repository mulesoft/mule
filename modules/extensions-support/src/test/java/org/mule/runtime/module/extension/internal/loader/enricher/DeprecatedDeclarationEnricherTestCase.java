/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.FunctionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.ExpressionFunctions;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.extension.api.runtime.route.Route;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

import org.junit.Test;

public class DeprecatedDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private DeprecationModelDeclarationEnricher enricher = new DeprecationModelDeclarationEnricher();

  @Test
  public void nonDeprecatedOperationTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    OperationDeclaration operationDeclaration =
        (OperationDeclaration) getNamedDeclaration(extensionDeclaration.getOperations(), "nonDeprecatedOperation");
    assertFalse(operationDeclaration.getDeprecation().isPresent());
  }

  @Test
  public void deprecatedOperationTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    OperationDeclaration operationDeclaration =
        (OperationDeclaration) getNamedDeclaration(extensionDeclaration.getOperations(), "deprecatedOperation");
    assertTrue(operationDeclaration.getDeprecation().isPresent());
    assertThat(operationDeclaration.getDeprecation().get().getMessage(), is("Use nonDeprecatedOperation instead."));
    assertThat(operationDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.2.0"));
    assertFalse(operationDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void nonDeprecatedSourceTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    SourceDeclaration sourceDeclaration =
        (SourceDeclaration) getNamedDeclaration(extensionDeclaration.getMessageSources(), "NonDeprecatedSource");
    assertFalse(sourceDeclaration.getDeprecation().isPresent());
  }

  @Test
  public void deprecatedSourceTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    SourceDeclaration sourceDeclaration =
        (SourceDeclaration) getNamedDeclaration(extensionDeclaration.getMessageSources(), "DeprecatedSource");
    assertTrue(sourceDeclaration.getDeprecation().isPresent());
    assertThat(sourceDeclaration.getDeprecation().get().getMessage(), is("Use NonDeprecatedSource instead"));
    assertThat(sourceDeclaration.getDeprecation().get().getDeprecatedSince(), is("2.4.0"));
    assertTrue(sourceDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
    assertThat(sourceDeclaration.getDeprecation().get().getToRemoveIn().get(), is("3.0.0"));
  }

  @Test
  public void nonDeprecatedParameterTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    OperationDeclaration operationDeclaration =
        (OperationDeclaration) getNamedDeclaration(extensionDeclaration.getOperations(), "nonDeprecatedOperation");
    ParameterDeclaration parameterDeclaration =
        (ParameterDeclaration) getNamedDeclaration(operationDeclaration.getAllParameters(), "echo");
    assertFalse(parameterDeclaration.getDeprecation().isPresent());
  }

  @Test
  public void deprecatedParameterTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    OperationDeclaration operationDeclaration =
        (OperationDeclaration) getNamedDeclaration(extensionDeclaration.getOperations(), "nonDeprecatedOperation");
    ParameterDeclaration parameterDeclaration =
        (ParameterDeclaration) getNamedDeclaration(operationDeclaration.getAllParameters(), "badParameter");
    assertTrue(parameterDeclaration.getDeprecation().isPresent());
    assertThat(parameterDeclaration.getDeprecation().get().getMessage(), is("This parameter was made redundant"));
    assertThat(parameterDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.1.0"));
    assertFalse(parameterDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void nonDeprecatedScopeTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    OperationDeclaration operationDeclaration =
        (OperationDeclaration) getNamedDeclaration(extensionDeclaration.getOperations(), "nonDeprecatedScope");
    assertFalse(operationDeclaration.getDeprecation().isPresent());
  }

  @Test
  public void deprecatedScopeTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    OperationDeclaration operationDeclaration =
        (OperationDeclaration) getNamedDeclaration(extensionDeclaration.getOperations(), "deprecatedScope");
    assertTrue(operationDeclaration.getDeprecation().isPresent());
    assertThat(operationDeclaration.getDeprecation().get().getMessage(), is("Use nonDeprecatedScope instead."));
    assertThat(operationDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.7.0"));
    assertFalse(operationDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void nonDeprecatedRouterTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    ConstructDeclaration constructDeclaration =
        (ConstructDeclaration) getNamedDeclaration(extensionDeclaration.getConstructs(), "nonDeprecatedRouter");
    assertFalse(constructDeclaration.getDeprecation().isPresent());
  }

  @Test
  public void deprecatedRouterTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    ConstructDeclaration constructDeclaration =
        (ConstructDeclaration) getNamedDeclaration(extensionDeclaration.getConstructs(), "deprecatedRouter");
    assertTrue(constructDeclaration.getDeprecation().isPresent());
    assertThat(constructDeclaration.getDeprecation().get().getMessage(), is("Use nonDeprecatedRouter instead."));
    assertThat(constructDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.4.0"));
    assertTrue(constructDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
    assertThat(constructDeclaration.getDeprecation().get().getToRemoveIn().get(), is("2.0.0"));
  }

  @Test
  public void nonDeprecatedFunctionTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    FunctionDeclaration functionDeclaration =
        (FunctionDeclaration) getNamedDeclaration(extensionDeclaration.getFunctions(), "nonDeprecatedFunction");
    assertFalse(functionDeclaration.getDeprecation().isPresent());
  }

  @Test
  public void deprecatedFunctionTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    FunctionDeclaration functionDeclaration =
        (FunctionDeclaration) getNamedDeclaration(extensionDeclaration.getFunctions(), "deprecatedFunction");
    assertTrue(functionDeclaration.getDeprecation().isPresent());
    assertThat(functionDeclaration.getDeprecation().get().getMessage(), is("Use nonDeprecatedFunction instead."));
    assertThat(functionDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.4.0"));
    assertFalse(functionDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void nonDeprecatedExtensionTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(NonDeprecatedExtension.class);
    assertFalse(extensionDeclaration.getDeprecation().isPresent());
  }

  @Test
  public void deprecatedExtensionTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    assertTrue(extensionDeclaration.getDeprecation().isPresent());
    assertThat(extensionDeclaration.getDeprecation().get().getMessage(), is("This extension is deprecated"));
    assertThat(extensionDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.2.0"));
    assertFalse(extensionDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void nonDeprecatedConfigurationTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    ConfigurationDeclaration configurationDeclaration = extensionDeclaration.getConfigurations().get(1);
    assertFalse(configurationDeclaration.getDeprecation().isPresent());
  }

  @Test
  public void deprecatedConfigurationTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    ConfigurationDeclaration configurationDeclaration = extensionDeclaration.getConfigurations().get(0);
    assertTrue(configurationDeclaration.getDeprecation().isPresent());
    assertThat(configurationDeclaration.getDeprecation().get().getMessage(), is("This configuration is deprecated."));
    assertThat(configurationDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.3.0"));
    assertFalse(configurationDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void nonDeprecatedConnectionProviderTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    ConfigurationDeclaration configurationDeclaration = extensionDeclaration.getConfigurations().get(1);
    ConnectionProviderDeclaration connectionProviderDeclaration = configurationDeclaration.getConnectionProviders().get(1);
    assertFalse(connectionProviderDeclaration.getDeprecation().isPresent());
  }

  @Test
  public void deprecatedConnectionProviderTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(DeprecatedExtension.class);
    ConfigurationDeclaration configurationDeclaration = extensionDeclaration.getConfigurations().get(1);
    ConnectionProviderDeclaration connectionProviderDeclaration = configurationDeclaration.getConnectionProviders().get(0);
    assertTrue(connectionProviderDeclaration.getDeprecation().isPresent());
    assertThat(connectionProviderDeclaration.getDeprecation().get().getMessage(),
               is("You should use the new connection provider"));
    assertThat(connectionProviderDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.4.0"));
    assertTrue(connectionProviderDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
    assertThat(connectionProviderDeclaration.getDeprecation().get().getToRemoveIn().get(), is("2.0.0"));
  }

  @Test
  public void sdkDeprecatedOperationTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(SdkDeprecatedExtension.class);
    OperationDeclaration operationDeclaration =
        (OperationDeclaration) getNamedDeclaration(extensionDeclaration.getOperations(), "sdkDeprecatedOperation");
    assertTrue(operationDeclaration.getDeprecation().isPresent());
    assertThat(operationDeclaration.getDeprecation().get().getMessage(), is("Use sdkNonDeprecatedOperation instead."));
    assertThat(operationDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.2.0"));
    assertFalse(operationDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void sdkDeprecatedSourceTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(SdkDeprecatedExtension.class);
    SourceDeclaration sourceDeclaration =
        (SourceDeclaration) getNamedDeclaration(extensionDeclaration.getMessageSources(), "SdkDeprecatedSource");
    assertTrue(sourceDeclaration.getDeprecation().isPresent());
    assertThat(sourceDeclaration.getDeprecation().get().getMessage(), is("Use SdkNonDeprecatedSource instead"));
    assertThat(sourceDeclaration.getDeprecation().get().getDeprecatedSince(), is("2.4.0"));
    assertTrue(sourceDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
    assertThat(sourceDeclaration.getDeprecation().get().getToRemoveIn().get(), is("3.0.0"));
  }

  @Test
  public void sdkDeprecatedParameterTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(SdkDeprecatedExtension.class);
    OperationDeclaration operationDeclaration =
        (OperationDeclaration) getNamedDeclaration(extensionDeclaration.getOperations(), "sdkNonDeprecatedOperation");
    ParameterDeclaration parameterDeclaration =
        (ParameterDeclaration) getNamedDeclaration(operationDeclaration.getAllParameters(), "badParameter");
    assertTrue(parameterDeclaration.getDeprecation().isPresent());
    assertThat(parameterDeclaration.getDeprecation().get().getMessage(), is("This parameter was made redundant"));
    assertThat(parameterDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.1.0"));
    assertFalse(parameterDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void sdkDeprecatedScopeTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(SdkDeprecatedExtension.class);
    OperationDeclaration operationDeclaration =
        (OperationDeclaration) getNamedDeclaration(extensionDeclaration.getOperations(), "sdkDeprecatedScope");
    assertTrue(operationDeclaration.getDeprecation().isPresent());
    assertThat(operationDeclaration.getDeprecation().get().getMessage(), is("Use sdkNonDeprecatedScope instead."));
    assertThat(operationDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.7.0"));
    assertFalse(operationDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void sdkDeprecatedRouterTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(SdkDeprecatedExtension.class);
    ConstructDeclaration constructDeclaration =
        (ConstructDeclaration) getNamedDeclaration(extensionDeclaration.getConstructs(), "sdkDeprecatedRouter");
    assertTrue(constructDeclaration.getDeprecation().isPresent());
    assertThat(constructDeclaration.getDeprecation().get().getMessage(), is("Use sdkNonDeprecatedRouter instead."));
    assertThat(constructDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.4.0"));
    assertTrue(constructDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
    assertThat(constructDeclaration.getDeprecation().get().getToRemoveIn().get(), is("2.0.0"));
  }

  @Test
  public void sdkDeprecatedFunctionTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(SdkDeprecatedExtension.class);
    FunctionDeclaration functionDeclaration =
        (FunctionDeclaration) getNamedDeclaration(extensionDeclaration.getFunctions(), "sdkDeprecatedFunction");
    assertTrue(functionDeclaration.getDeprecation().isPresent());
    assertThat(functionDeclaration.getDeprecation().get().getMessage(), is("Use sdkNonDeprecatedFunction instead."));
    assertThat(functionDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.4.0"));
    assertFalse(functionDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void sdkDeprecatedExtensionTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(SdkDeprecatedExtension.class);
    assertTrue(extensionDeclaration.getDeprecation().isPresent());
    assertThat(extensionDeclaration.getDeprecation().get().getMessage(), is("This extension is deprecated"));
    assertThat(extensionDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.2.0"));
    assertFalse(extensionDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void sdkDeprecatedConfigurationTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(SdkDeprecatedExtension.class);
    ConfigurationDeclaration configurationDeclaration = extensionDeclaration.getConfigurations().get(0);
    assertTrue(configurationDeclaration.getDeprecation().isPresent());
    assertThat(configurationDeclaration.getDeprecation().get().getMessage(), is("This configuration is deprecated."));
    assertThat(configurationDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.3.0"));
    assertFalse(configurationDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
  }

  @Test
  public void sdkDeprecatedConnectionProviderTestCase() {
    ExtensionDeclaration extensionDeclaration = getEnrichedExtensionDeclaration(SdkDeprecatedExtension.class);
    ConfigurationDeclaration configurationDeclaration = extensionDeclaration.getConfigurations().get(1);
    ConnectionProviderDeclaration connectionProviderDeclaration = configurationDeclaration.getConnectionProviders().get(0);
    assertTrue(connectionProviderDeclaration.getDeprecation().isPresent());
    assertThat(connectionProviderDeclaration.getDeprecation().get().getMessage(),
               is("You should use the new connection provider"));
    assertThat(connectionProviderDeclaration.getDeprecation().get().getDeprecatedSince(), is("1.4.0"));
    assertTrue(connectionProviderDeclaration.getDeprecation().get().getToRemoveIn().isPresent());
    assertThat(connectionProviderDeclaration.getDeprecation().get().getToRemoveIn().get(), is("2.0.0"));
  }

  private ExtensionDeclaration getEnrichedExtensionDeclaration(Class<?> extensionClass) {
    ExtensionDeclarer declarer = new DefaultJavaModelLoaderDelegate(extensionClass, "1.0.0-dev")
        .declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    enricher.enrich(new DefaultExtensionLoadingContext(declarer, this.getClass().getClassLoader(), getDefault(emptySet())));
    return declarer.getDeclaration();
  }

  private NamedDeclaration getNamedDeclaration(List<? extends NamedDeclaration> namedDeclarationList, String name) {
    for (NamedDeclaration namedDeclaration : namedDeclarationList) {
      if (namedDeclaration.getName().equals(name)) {
        return namedDeclaration;
      }
    }
    fail("No namedDeclaration with name " + name + " was found in list.");
    return null;
  }

  @Deprecated(message = "This extension is deprecated", since = "1.2.0")
  @Extension(name = "Deprecated")
  @ExpressionFunctions(DeprecatedExtensionFunctions.class)
  @Configurations({DeprecatedConfiguration.class, NonDeprecatedConfiguration.class})
  public static class DeprecatedExtension {

  }

  @Configuration
  @Sources(NonDeprecatedSource.class)
  @Operations({DeprecatedExtensionOperations.class})
  @Deprecated(message = "This configuration is deprecated.", since = "1.3.0")
  public static class DeprecatedConfiguration {

  }

  @Configuration
  @Sources({DeprecatedSource.class})
  @ConnectionProviders({DeprecatedConnectionProvider.class, NonDeprecatedConnectionProvider.class})
  public static class NonDeprecatedConfiguration {

  }


  @Deprecated(message = "You should use the new connection provider", since = "1.4.0", toRemoveIn = "2.0.0")
  public static class DeprecatedConnectionProvider implements ConnectionProvider<Connection> {

    @Override
    public Connection connect() throws ConnectionException {
      return new Connection("Your house");
    }

    @Override
    public void disconnect(Connection connection) {
      // No-op
    }

    @Override
    public ConnectionValidationResult validate(Connection connection) {
      return null;
    }
  }

  public static class NonDeprecatedConnectionProvider implements ConnectionProvider<Connection> {

    @Override
    public Connection connect() throws ConnectionException {
      return new Connection("Your new house");
    }

    @Override
    public void disconnect(Connection connection) {
      // No-op
    }

    @Override
    public ConnectionValidationResult validate(Connection connection) {
      return null;
    }
  }

  public static class Connection {

    private String address;

    public Connection(String address) {
      this.address = address;
    }

    public void setAddress(String address) {
      this.address = address;
    }

    public String getAddress() {
      return address;
    }
  }

  public static class DeprecatedExtensionOperations {

    @Deprecated(message = "Use nonDeprecatedOperation instead.", since = "1.2.0")
    public String deprecatedOperation(String echo) {
      return echo;
    }

    public String nonDeprecatedOperation(String echo,
                                         @Deprecated(message = "This parameter was made redundant",
                                             since = "1.1.0") String badParameter) {
      return echo;
    }

    @Deprecated(message = "Use nonDeprecatedScope instead.", since = "1.7.0")
    public void deprecatedScope(Chain chain, CompletionCallback<Void, Void> cb) {}

    public void nonDeprecatedScope(Chain chain, CompletionCallback<Void, Void> cb) {}

    @Deprecated(message = "Use nonDeprecatedRouter instead.", since = "1.4.0", toRemoveIn = "2.0.0")
    public void deprecatedRouter(Route route, RouterCompletionCallback callback) {}

    public void nonDeprecatedRouter(Route route, RouterCompletionCallback callback) {}

  }

  public static class DeprecatedExtensionFunctions {

    @Deprecated(message = "Use nonDeprecatedFunction instead.", since = "1.4.0")
    public String deprecatedFunction(String echo) {
      return echo;
    }

    public String nonDeprecatedFunction(String echo,
                                        @Deprecated(message = "This parameter was made redundant",
                                            since = "1.8.0") String badParameter) {
      return echo;
    }

  }

  @Deprecated(message = "Use NonDeprecatedSource instead", since = "2.4.0", toRemoveIn = "3.0.0")
  public static class DeprecatedSource extends Source<String, String> {

    @Override
    public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  public static class NonDeprecatedSource extends Source<String, String> {

    @Override
    public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

  @Extension(name = "Not Deprecated")
  public static class NonDeprecatedExtension {

  }

  @org.mule.sdk.api.annotation.deprecated.Deprecated(message = "This extension is deprecated", since = "1.2.0")
  @Extension(name = "Deprecated")
  @ExpressionFunctions(SdkDeprecatedExtensionFunctions.class)
  @Configurations({SdkDeprecatedConfiguration.class, SdkNonDeprecatedConfiguration.class})
  public static class SdkDeprecatedExtension {

  }

  @Configuration
  @Sources(NonDeprecatedSource.class)
  @Operations({SdkDeprecatedExtensionOperations.class})
  @org.mule.sdk.api.annotation.deprecated.Deprecated(message = "This configuration is deprecated.", since = "1.3.0")
  public static class SdkDeprecatedConfiguration {
  }

  @Configuration
  @Sources({SdkDeprecatedSource.class})
  @ConnectionProviders({SdkDeprecatedConnectionProvider.class, NonDeprecatedConnectionProvider.class})
  public static class SdkNonDeprecatedConfiguration {

  }

  @org.mule.sdk.api.annotation.deprecated.Deprecated(message = "You should use the new connection provider", since = "1.4.0",
      toRemoveIn = "2.0.0")
  public static class SdkDeprecatedConnectionProvider implements ConnectionProvider<Connection> {

    @Override
    public Connection connect() throws ConnectionException {
      return new Connection("Your house");
    }

    @Override
    public void disconnect(Connection connection) {
      // No-op
    }

    @Override
    public ConnectionValidationResult validate(Connection connection) {
      return null;
    }
  }

  public static class SdkDeprecatedExtensionOperations {

    @org.mule.sdk.api.annotation.deprecated.Deprecated(message = "Use sdkNonDeprecatedOperation instead.", since = "1.2.0")
    public String sdkDeprecatedOperation(String echo) {
      return echo;
    }

    public String sdkNonDeprecatedOperation(String echo,
                                            @Deprecated(message = "This parameter was made redundant",
                                                since = "1.1.0") String badParameter) {
      return echo;
    }

    @org.mule.sdk.api.annotation.deprecated.Deprecated(message = "Use sdkNonDeprecatedScope instead.", since = "1.7.0")
    public void sdkDeprecatedScope(Chain chain, CompletionCallback<Void, Void> cb) {}

    public void sdkNonDeprecatedScope(Chain chain, CompletionCallback<Void, Void> cb) {}

    @org.mule.sdk.api.annotation.deprecated.Deprecated(message = "Use sdkNonDeprecatedRouter instead.", since = "1.4.0",
        toRemoveIn = "2.0.0")
    public void sdkDeprecatedRouter(Route route, RouterCompletionCallback callback) {}

    public void sdkNonDeprecatedRouter(Route route, RouterCompletionCallback callback) {}

  }

  public static class SdkDeprecatedExtensionFunctions {

    @org.mule.sdk.api.annotation.deprecated.Deprecated(message = "Use sdkNonDeprecatedFunction instead.", since = "1.4.0")
    public String sdkDeprecatedFunction(String echo) {
      return echo;
    }

    public String sdkNonDeprecatedFunction(String echo,
                                           @Deprecated(message = "This parameter was made redundant",
                                               since = "1.8.0") String badParameter) {
      return echo;
    }

  }

  @Deprecated(message = "Use SdkNonDeprecatedSource instead", since = "2.4.0", toRemoveIn = "3.0.0")
  public static class SdkDeprecatedSource extends Source<String, String> {

    @Override
    public void onStart(SourceCallback<String, String> sourceCallback) throws MuleException {}

    @Override
    public void onStop() {}
  }

}
