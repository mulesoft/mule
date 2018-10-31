package org.mule.runtime.module.extension.internal.loader.enricher;

import org.junit.Test;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.declaration.fluent.*;
import org.mule.runtime.extension.api.annotation.*;
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

import java.util.List;

import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;

public class DeprecatedDeclarationEnricherTestCase {

  private DeprecatedModelDeclarationEnricher enricher = new DeprecatedModelDeclarationEnricher();

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

  @Deprecated(message = "This extension is deprecated")
  @Extension(name = "Deprecated")
  @ExpressionFunctions(DeprecatedExtensionFunctions.class)
  @Configurations({DeprecatedConfiguration.class, NonDeprecatedConfiguration.class})
  public static class DeprecatedExtension {

  }

  @Configuration
  @Sources(NonDeprecatedSource.class)
  @Operations({DeprecatedExtensionOperations.class})
  @Deprecated(message = "This configuration is deprecated.")
  public static class DeprecatedConfiguration {

  }

  @Configuration
  @Sources({DeprecatedSource.class})
  @ConnectionProviders({DeprecatedConnectionProvider.class, NonDeprecatedConnectionProvider.class})
  public static class NonDeprecatedConfiguration {

  }


  @Deprecated(message = "You should use the new connection provider")
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

    @Deprecated(message = "Use nonDeprecatedOperation instead.")
    public String deprecatedOperation(String echo) {
      return echo;
    }

    public String nonDeprecatedOperation(String echo,
                                         @Deprecated(message = "This parameter was made redundant") String badParameter) {
      return echo;
    }

    @Deprecated(message = "Use nonDeprecatedScope instead.")
    public void deprecatedScope(Chain chain, CompletionCallback<Void, Void> cb) {}

    public void nonDeprecatedScope(Chain chain, CompletionCallback<Void, Void> cb) {}

    @Deprecated(message = "Use nonDeprecatedRouter instead.")
    public void deprecatedRouter(Route route, RouterCompletionCallback callback) {}

    public void nonDeprecatedRouter(Route route, RouterCompletionCallback callback) {}

  }

  public static class DeprecatedExtensionFunctions {

    @Deprecated(message = "Use nonDeprecatedFunction instead.")
    public String deprecatedFunction(String echo) {
      return echo;
    }

    public String nonDeprecatedFunction(String echo,
                                        @Deprecated(message = "This parameter was made redundant") String badParameter) {
      return echo;
    }

  }

  @Deprecated(message = "Use NonDeprecatedSource instead")
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

}
