/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.dsl.model.ApplicationElement;
import org.mule.runtime.api.dsl.model.ApplicationElementIdentifier;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.extension.api.dsl.model.DslElementModel;
import org.mule.runtime.extension.api.dsl.model.DslElementModelResolver;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

@ArtifactClassLoaderRunnerConfig(
    sharedRuntimeLibs = {"org.apache.derby:derby"},
    plugins = {
        "org.mule.modules:mule-module-sockets",
        "org.mule.modules:mule-module-http-ext",
        "org.mule.modules:mule-module-db",
        "com.mulesoft.weave:mule-plugin-weave"},
    providedInclusions = "org.mule.modules:mule-module-sockets")
public class DslElementModelResolverTestCase extends MuleArtifactFunctionalTestCase {

  private static final int LISTENER_PATH = 0;
  private static final int DB_INSERT_PATH = 1;
  private static final int REQUESTER_PATH = 2;
  private static final String DB_NS = "http://www.mulesoft.org/schema/mule/db";
  private static final String HTTP_NS = "http://www.mulesoft.org/schema/mule/httpn";

  private ApplicationModel applicationModel;
  private DslElementModelResolver modelResolver;

  @Override
  protected String getConfigFile() {
    return "dsl-app.xml";
  }

  @Before
  public void setup() throws Exception {
    applicationModel = loadApplicationModel();

    Set<ExtensionModel> extensions = muleContext.getExtensionManager().getExtensions();
    modelResolver = DslElementModelResolver.getDefault(extensions);
  }

  @Test
  public void resolveSimpleConfigWithFlatConnection() throws Exception {
    ApplicationElement config = getApplicationElement(applicationModel, "dbConfig");
    DslElementModel<ConfigurationModel> configElement = resolve(config);
    assertElementName(configElement, "config");

    ApplicationElement connection = config.getInnerComponents().get(0);
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertElementName(connectionElement, "derby-connection");
    assertHasParameter(connectionElement.getModel(), "database");
    assertAttributeIsPresent(connectionElement, "database");
    assertHasParameter(connectionElement.getModel(), "create");
    assertAttributeIsPresent(connectionElement, "create");

    assertThat(configElement.getApplicationElement().isPresent(), is(true));
    assertThat(configElement.getApplicationElement().get(), is(equalTo(config)));
    assertThat(configElement.getIdentifier().isPresent(), is(true));
    assertThat(configElement.getIdentifier().get(), is(equalTo(config.getIdentifier())));

    assertThat(configElement.findElement(newIdentifier("oracle-connection", DB_NS)).isPresent(), is(false));
    assertThat(connectionElement.findElement(newIdentifier("connection-properties", DB_NS)).isPresent(), is(false));
  }

  @Test
  public void resolveConnectionNoExtraParameters() throws Exception {
    ApplicationElement config = getApplicationElement(applicationModel, "dbConfig");
    ApplicationElement connection = config.getInnerComponents().get(0);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertHasParameter(connectionElement.getModel(), "customDataTypes");
    assertThat(connectionElement.findElement("customDataTypes").isPresent(), is(false));
  }

  @Test
  public void resolutionFailsForNonTopLevelElement() throws Exception {
    ApplicationElement config = getApplicationElement(applicationModel, "dbConfig");
    ApplicationElement connection = config.getInnerComponents().get(0);

    assertThat(modelResolver.resolve(connection).isPresent(), is(false));
  }

  @Test
  public void resolveConfigNoExtraContainedElements() throws Exception {
    ApplicationElement config = getApplicationElement(applicationModel, "httpListener");
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    assertThat(configElement.findElement(newIdentifier("request-connection", DB_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConfigWithParameters() throws Exception {
    ApplicationElement config = getApplicationElement(applicationModel, "httpListener");
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    assertElementName(configElement, "listener-config");
    assertHasParameter(configElement.getModel(), "basePath");
    assertAttributeIsPresent(configElement, "basePath");

    ApplicationElement connection = config.getInnerComponents().get(0);
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertElementName(connectionElement, "listener-connection");
    assertAttributeIsPresent(connectionElement, "host");
    assertAttributeIsPresent(connectionElement, "port");

    assertThat(configElement.findElement(newIdentifier("request-connection", DB_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConnectionWithSubtypes() throws Exception {
    ApplicationElement config = getApplicationElement(applicationModel, "httpRequester");
    DslElementModel<ConfigurationModel> configElement = resolve(config);
    assertElementName(configElement, "request-config");

    ApplicationElement connection = config.getInnerComponents().get(0);
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertElementName(connectionElement, "request-connection");
    assertHasParameter(connectionElement.getModel(), "host");
    assertAttributeIsPresent(connectionElement, "host");
    assertHasParameter(connectionElement.getModel(), "port");
    assertAttributeIsPresent(connectionElement, "port");

    ApplicationElement authenticationWrapper = connection.getInnerComponents().get(0);
    DslElementModel<ParameterModel> authenticationWrapperElement = getChild(connectionElement, authenticationWrapper);
    assertElementName(authenticationWrapperElement, "authentication");

    DslElementModel<ObjectType> basicAuthElement = getChild(connectionElement, newIdentifier("basic-authentication", HTTP_NS));
    assertElementName(basicAuthElement, "basic-authentication");
    assertThat(basicAuthElement.getDsl().isWrapped(), is(false));
    assertThat(basicAuthElement.getDsl().supportsAttributeDeclaration(), is(false));

    assertThat(configElement.findElement(newIdentifier("listener-connection", DB_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConnectionWithImportedTypes() throws Exception {
    ApplicationElement config = getApplicationElement(applicationModel, "httpRequester");
    DslElementModel<ConfigurationModel> configElement = resolve(config);
    assertElementName(configElement, "request-config");

    ApplicationElement connection = config.getInnerComponents().get(0);
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertElementName(connectionElement, "request-connection");
    assertHasParameter(connectionElement.getModel(), "host");
    assertAttributeIsPresent(connectionElement, "host");
    assertHasParameter(connectionElement.getModel(), "port");
    assertAttributeIsPresent(connectionElement, "port");

    ApplicationElement propertiesWrapper = connection.getInnerComponents().get(1);
    DslElementModel<ParameterModel> wrapperElement = getChild(connectionElement, propertiesWrapper);
    assertElementName(wrapperElement, "client-socket-properties");

    ApplicationElement properties = propertiesWrapper.getInnerComponents().get(0);
    DslElementModel<ObjectType> propertiesElement = getChild(wrapperElement, properties);

    assertElementName(propertiesElement, "tcp-client-socket-properties");
    assertThat(propertiesElement.getDsl().isWrapped(), is(true));
    assertThat(propertiesElement.getDsl().supportsAttributeDeclaration(), is(false));

    assertThat(configElement.findElement(newIdentifier("listener-connection", DB_NS)).isPresent(),
               is(false));
  }

  @Test
  public void flowElementsResolution() throws Exception {
    ApplicationElement flow = getApplicationElement(applicationModel, "components");

    ApplicationElement listener = flow.getInnerComponents().get(LISTENER_PATH);
    assertListenerSourceWithMessageBuilder(listener);

    ApplicationElement dbInsert = flow.getInnerComponents().get(DB_INSERT_PATH);
    assertBulkInsertOperationWithNestedList(dbInsert);

    ApplicationElement requester = flow.getInnerComponents().get(REQUESTER_PATH);
    assertRequestOperationWithFlatParameters(requester);
  }

  private void assertRequestOperationWithFlatParameters(ApplicationElement requester) {
    DslElementModel<OperationModel> requesterElement = resolve(requester);
    assertHasParameter(requesterElement.getModel(), "path");
    assertThat(requesterElement.findElement("path").isPresent(), is(true));
    assertHasParameter(requesterElement.getModel(), "method");
    assertThat(requesterElement.findElement("method").isPresent(), is(true));
  }

  private void assertBulkInsertOperationWithNestedList(ApplicationElement dbInsert) {
    DslElementModel<OperationModel> dbElement = resolve(dbInsert);

    assertThat(dbElement.getInnerElements().size(),
               is(dbInsert.getInnerComponents().size() + dbInsert.getParameters().entrySet().size()));

    ApplicationElement sql = dbInsert.getInnerComponents().get(0);
    DslElementModel<ParameterModel> sqlElement = getChild(dbElement, sql);
    assertElementName(sqlElement, "sql");
    ApplicationElement parameterTypes = dbInsert.getInnerComponents().get(1);
    DslElementModel<ParameterModel> parameterTypesElement = getChild(dbElement, parameterTypes);
    assertElementName(parameterTypesElement, "parameter-types");


    ApplicationElement parameterOne = parameterTypes.getInnerComponents().get(0);
    assertThat(parameterOne.getParameters().get("key"), is("name"));
    DslElementModel<ObjectType> elementOne = parameterTypesElement.getInnerElements().get(0);
    assertElementName(elementOne, parameterOne.getIdentifier().getName());

    ApplicationElement parameterTwo = parameterTypes.getInnerComponents().get(1);
    assertThat(parameterTwo.getParameters().get("key"), is("position"));
    DslElementModel<ObjectType> elementTwo = parameterTypesElement.getInnerElements().get(1);
    assertElementName(elementTwo, parameterTwo.getIdentifier().getName());
  }

  private void assertListenerSourceWithMessageBuilder(ApplicationElement listener) {
    DslElementModel<SourceModel> listenerElement = resolve(listener);

    assertHasParameter(listenerElement.getModel(), "path");
    ApplicationElement responseBuilder = listener.getInnerComponents().get(0);

    DslElementModel<ParameterModel> responseBuilderElement = getChild(listenerElement, responseBuilder);
    assertElementName(responseBuilderElement, "response-builder");

    assertThat(responseBuilderElement.getDsl().getChild("headers").isPresent(), is(true));
  }

  // Scaffolding
  private <T extends NamedObject> DslElementModel<T> resolve(ApplicationElement component) {
    Optional<DslElementModel<T>> elementModel = modelResolver.resolve(component);
    assertThat(elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  private ApplicationElement getApplicationElement(ApplicationModel applicationModel, String name) {
    Optional<ApplicationElement> component = applicationModel.findNamedElement(name);
    assertThat(component.isPresent(), is(true));
    return component.get();
  }

  private <T> DslElementModel<T> getChild(DslElementModel<? extends NamedObject> parent, ApplicationElement component) {
    return getChild(parent, component.getIdentifier());
  }

  private <T> DslElementModel<T> getChild(DslElementModel<? extends NamedObject> parent,
                                          ApplicationElementIdentifier identifier) {
    Optional<DslElementModel<T>> elementModel = parent.findElement(identifier);
    assertThat(format("Failed fetching child '%s' from parent '%s'", identifier.getName(),
                      parent.getModel().getName()),
               elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  private <T> DslElementModel<T> getAttribute(DslElementModel<? extends NamedObject> parent, String component) {
    Optional<DslElementModel<T>> elementModel = parent.findElement(component);
    assertThat(format("Failed fetching attribute '%s' from parent '%s'", component, parent.getModel().getName()),
               elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  private ApplicationElementIdentifier newIdentifier(String name, String ns) {
    return ApplicationElementIdentifier.builder().withName(name).withNamespace(ns).build();
  }

  private void assertHasParameter(ParameterizedModel model, String name) {
    assertThat(model.getAllParameterModels()
        .stream().anyMatch(p -> p.getName().equals(name)), is(true));
  }

  private void assertAttributeIsPresent(DslElementModel<? extends ParameterizedModel> element, String name) {
    assertHasParameter(element.getModel(), name);
    DslElementModel<NamedObject> databaseParam = getAttribute(element, name);
    assertThat(databaseParam.getDsl().supportsAttributeDeclaration(), is(true));
    assertThat(databaseParam.getDsl().supportsChildDeclaration(), is(false));
  }

  private void assertElementName(DslElementModel propertiesElement, String name) {
    assertThat(propertiesElement.getDsl().getElementName(), is(name));
  }

  // Scaffolding
  private ApplicationModel loadApplicationModel() throws Exception {
    InputStream appIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(getConfigFile());
    checkArgument(appIs != null, "The given application was not found as resource");

    Document document = new XmlConfigurationDocumentLoader().loadDocument(
                                                                          of(muleContext.getExtensionManager()), getConfigFile(),
                                                                          appIs);

    ConfigLine configLine = new XmlApplicationParser(new SpiServiceRegistry())
        .parse(document.getDocumentElement())
        .orElseThrow(() -> new Exception("Failed to load config"));

    ArtifactConfig artifactConfig = new ArtifactConfig.Builder()
        .addConfigFile(new ConfigFile(getConfigFile(), singletonList(configLine)))
        .build();

    return new ApplicationModel(artifactConfig, new ArtifactConfiguration(emptyList()));
  }

}
