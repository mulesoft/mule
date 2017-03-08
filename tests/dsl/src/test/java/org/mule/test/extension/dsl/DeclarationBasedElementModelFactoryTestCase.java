/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.BLOCKING;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.COUNT;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.FREQUENCY;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECT_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder.MAX_REDELIVERY_COUNT;
import static org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder.USE_SECURE_HASH;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.app.declaration.ComponentElementDeclaration;
import org.mule.runtime.api.app.declaration.ConfigurationElementDeclaration;
import org.mule.runtime.api.app.declaration.ConnectionElementDeclaration;
import org.mule.runtime.api.app.declaration.OperationElementDeclaration;
import org.mule.runtime.api.app.declaration.SourceElementDeclaration;
import org.mule.runtime.api.app.declaration.fluent.ElementDeclarer;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModel;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import org.junit.Before;
import org.junit.Test;

public class DeclarationBasedElementModelFactoryTestCase extends AbstractElementModelTestCase {

  private ConfigurationElementDeclaration dbConfig;
  private ConfigurationElementDeclaration listenerConfig;
  private ConfigurationElementDeclaration requestConfig;
  private OperationElementDeclaration request;
  private SourceElementDeclaration listener;
  private OperationElementDeclaration bulkInsert;
  private ConnectionElementDeclaration derbyConnection;
  private ConnectionElementDeclaration listenerConnection;
  private ConnectionElementDeclaration requestConnection;

  @Override
  protected String getConfigFile() {
    return "integration-multi-config-dsl-app.xml";
  }

  @Before
  public void createAppDeclaration() {

    ElementDeclarer db = ElementDeclarer.forExtension("Database");
    ElementDeclarer http = ElementDeclarer.forExtension("HTTP");

    derbyConnection = db.newConnection("derby-connection")
        .withParameter("database", "target/muleEmbeddedDB")
        .withParameter("create", "true")
        .getDeclaration();
    dbConfig = db.newConfiguration("config")
        .withRefName("dbConfig")
        .withConnection(derbyConnection)
        .getDeclaration();

    listenerConnection = http.newConnection("listener-connection")
        .withParameter(TLS_PARAMETER_NAME, newObjectValue()
            .withParameter("key-store", newObjectValue()
                .withParameter("path", "ssltest-keystore.jks")
                .withParameter("password", "changeit")
                .withParameter("keyPassword", "changeit")
                .build())
            .build())
        .withParameter("host", "localhost")
        .withParameter("port", "49019")
        .withParameter("protocol", "HTTPS")
        .getDeclaration();

    listenerConfig = http.newConfiguration("listener-config")
        .withRefName("httpListener")
        .withParameter("basePath", "/")
        .withConnection(listenerConnection)
        .getDeclaration();

    requestConnection = http.newConnection("request-connection")
        .withParameter("host", "localhost")
        .withParameter("port", "49020")
        .withParameter("authentication",
                       newObjectValue()
                           .ofType("org.mule.extension.http.api.request.authentication.BasicAuthentication")
                           .withParameter("username", "user")
                           .withParameter("password", "pass")
                           .build())
        .withParameter("clientSocketProperties",
                       newObjectValue()
                           .withParameter("connectionTimeout", "1000")
                           .withParameter("keepAlive", "true")
                           .withParameter("receiveBufferSize", "1024")
                           .withParameter("sendBufferSize", "1024")
                           .withParameter("clientTimeout", "1000")
                           .withParameter("linger", "1000")
                           .build())
        .getDeclaration();

    requestConfig = http.newConfiguration("request-config")
        .withRefName("httpRequester")
        .withConnection(requestConnection)
        .getDeclaration();

    listener = http.newSource("listener")
        .withConfig("httpListener")
        .withParameter("path", "testBuilder")
        .withParameter(REDELIVERY_POLICY_PARAMETER_NAME,
                       newObjectValue()
                           .withParameter(MAX_REDELIVERY_COUNT, "2")
                           .withParameter(USE_SECURE_HASH, "true")
                           .build())
        .withParameter(RECONNECTION_STRATEGY_PARAMETER_NAME,
                       newObjectValue()
                           .ofType(RECONNECT_ALIAS)
                           .withParameter(BLOCKING, "true")
                           .withParameter(COUNT, "1")
                           .withParameter(FREQUENCY, "0")
                           .build())
        .withParameter("response",
                       newObjectValue()
                           .withParameter("headers", "#[mel:['content-type' : 'text/plain']]")
                           .build())
        .getDeclaration();

    bulkInsert = db.newOperation("bulkInsert")
        .withParameter("sql", "INSERT INTO PLANET(POSITION, NAME) VALUES (:position, :name)")
        .withParameter("parameterTypes",
                       newListValue()
                           .withValue(newObjectValue()
                               .withParameter("key", "name")
                               .withParameter("type", "VARCHAR").build())
                           .withValue(newObjectValue()
                               .withParameter("key", "position")
                               .withParameter("type", "INTEGER").build())
                           .build())
        .getDeclaration();

    request = http.newOperation("request")
        .withConfig("httpRequester")
        .withParameter("path", "/nested")
        .withParameter("method", "POST")
        .getDeclaration();

  }

  @Test
  public void resolveSimpleConfigWithFlatConnection() throws Exception {
    DslElementModel<ConfigurationModel> configElement = resolve(dbConfig);
    assertElementName(configElement, "config");


    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, derbyConnection.getName());

    assertElementName(connectionElement, "derby-connection");
    assertHasParameter(connectionElement.getModel(), "database");
    assertAttributeIsPresent(connectionElement, "database");
    assertHasParameter(connectionElement.getModel(), "create");
    assertAttributeIsPresent(connectionElement, "create");

    assertThat(configElement.getConfiguration().isPresent(), is(true));
    assertThat(configElement.getIdentifier().isPresent(), is(true));

    assertThat(configElement.findElement(newIdentifier("oracle-connection", DB_NS)).isPresent(), is(false));
    assertThat(connectionElement.findElement(newIdentifier("connection-properties", DB_NS)).isPresent(), is(false));
  }

  @Test
  public void resolveConnectionNoExtraParameters() throws Exception {
    DslElementModel<ConfigurationModel> configElement = resolve(dbConfig);
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, derbyConnection.getName());

    assertHasParameter(connectionElement.getModel(), "customDataTypes");
    assertThat(connectionElement.findElement("customDataTypes").isPresent(), is(false));
  }

  @Test
  public void resolutionFailsForNonTopLevelElement() throws Exception {
    assertThat(modelResolver.create(derbyConnection).isPresent(), is(false));
  }

  @Test
  public void resolveConfigNoExtraContainedElements() throws Exception {
    DslElementModel<ConfigurationModel> configElement = resolve(listenerConfig);

    assertThat(configElement.findElement(newIdentifier("request-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConfigWithParameters() throws Exception {
    DslElementModel<ConfigurationModel> configElement = resolve(listenerConfig);

    assertElementName(configElement, "listener-config");
    assertHasParameter(configElement.getModel(), "basePath");
    assertAttributeIsPresent(configElement, "basePath");

    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, listenerConnection.getName());

    assertElementName(connectionElement, "listener-connection");
    assertAttributeIsPresent(connectionElement, "host");
    assertAttributeIsPresent(connectionElement, "port");

    assertThat(configElement.findElement(newIdentifier("request-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConnectionWithSubtypes() throws Exception {
    DslElementModel<ConfigurationModel> configElement = resolve(requestConfig);
    assertElementName(configElement, "request-config");

    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, requestConnection.getName());

    assertElementName(connectionElement, "request-connection");
    assertHasParameter(connectionElement.getModel(), "host");
    assertAttributeIsPresent(connectionElement, "host");
    assertHasParameter(connectionElement.getModel(), "port");
    assertAttributeIsPresent(connectionElement, "port");

    DslElementModel<ParameterModel> authenticationWrapperElement = getChild(connectionElement, "authentication");
    assertElementName(authenticationWrapperElement, "authentication");

    DslElementModel<ObjectType> basicAuthElement = getChild(connectionElement, newIdentifier("basic-authentication", HTTP_NS));
    assertElementName(basicAuthElement, "basic-authentication");
    assertThat(basicAuthElement.getDsl().isWrapped(), is(false));
    assertThat(basicAuthElement.getDsl().supportsAttributeDeclaration(), is(false));

    assertThat(configElement.findElement(newIdentifier("listener-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConnectionWithImportedTypes() throws Exception {
    DslElementModel<ConfigurationModel> configElement = resolve(requestConfig);
    assertElementName(configElement, "request-config");

    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, requestConnection.getName());

    assertElementName(connectionElement, "request-connection");
    assertHasParameter(connectionElement.getModel(), "host");
    assertAttributeIsPresent(connectionElement, "host");
    assertHasParameter(connectionElement.getModel(), "port");
    assertAttributeIsPresent(connectionElement, "port");

    DslElementModel<ParameterModel> wrapperElement = getChild(connectionElement, "clientSocketProperties");
    assertElementName(wrapperElement, "client-socket-properties");

    DslElementModel<ObjectType> propertiesElement = getChild(wrapperElement,
                                                             newIdentifier("tcp-client-socket-properties",
                                                                           "http://www.mulesoft.org/schema/mule/sockets"));

    assertElementName(propertiesElement, "tcp-client-socket-properties");
    assertThat(propertiesElement.getDsl().isWrapped(), is(true));
    assertThat(propertiesElement.getDsl().supportsAttributeDeclaration(), is(false));

    assertThat(configElement.findElement(newIdentifier("listener-connection", DB_NS)).isPresent(),
               is(false));
  }

  @Test
  public void flowElementsResolution() throws Exception {
    assertListenerSourceWithMessageBuilder(listener);
    assertBulkInsertOperationWithNestedList(bulkInsert);
    assertRequestOperationWithFlatParameters(request);
  }

  private void assertRequestOperationWithFlatParameters(ComponentElementDeclaration requester) {
    DslElementModel<OperationModel> requesterElement = resolve(requester);
    assertHasParameter(requesterElement.getModel(), "path");
    assertThat(requesterElement.findElement("path").isPresent(), is(true));
    assertHasParameter(requesterElement.getModel(), "method");
    assertThat(requesterElement.findElement("method").isPresent(), is(true));
  }

  private void assertBulkInsertOperationWithNestedList(ComponentElementDeclaration dbInsert) {
    DslElementModel<OperationModel> dbElement = resolve(dbInsert);

    DslElementModel<ParameterModel> sqlElement = getChild(dbElement, "sql");
    assertElementName(sqlElement, "sql");

    DslElementModel<ParameterModel> parameterTypesElement = getChild(dbElement, "parameterTypes");
    assertElementName(parameterTypesElement, "parameter-types");


    ComponentConfiguration parameterOne = parameterTypesElement.getConfiguration().get().getNestedComponents().get(0);
    assertThat(parameterOne.getParameters().get("key"), is("name"));
    DslElementModel<ObjectType> elementOne = parameterTypesElement.getContainedElements().get(0);
    assertElementName(elementOne, parameterOne.getIdentifier().getName());

    ComponentConfiguration parameterTwo = parameterTypesElement.getConfiguration().get().getNestedComponents().get(1);
    assertThat(parameterTwo.getParameters().get("key"), is("position"));
    DslElementModel<ObjectType> elementTwo = parameterTypesElement.getContainedElements().get(1);
    assertElementName(elementTwo, parameterTwo.getIdentifier().getName());
  }

  private void assertListenerSourceWithMessageBuilder(ComponentElementDeclaration listener) {
    DslElementModel<SourceModel> listenerElement = resolve(listener);

    assertHasParameter(listenerElement.getModel(), "path");

    DslElementModel<ParameterModel> responseBuilderElement = getChild(listenerElement, "response");
    assertElementName(responseBuilderElement, "response");

    assertThat(responseBuilderElement.getDsl().getChild("headers").isPresent(), is(true));
  }

}
