/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModel;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class ConfigurationBasedElementModelFactoryTestCase extends AbstractElementModelTestCase {

  @Before
  public void initApp() throws Exception {
    applicationModel = loadApplicationModel();
  }

  @Override
  protected String getConfigFile() {
    return "integration-multi-config-dsl-app.xml";
  }

  @Test
  public void defaultValueResolution() throws Exception {
    ComponentConfiguration config = getAppElement(applicationModel, DB_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, config.getNestedComponents().get(0));

    assertElementName(connectionElement, "derby-connection");
    assertHasParameter(connectionElement.getModel(), "database");
    assertAttributeIsPresent(connectionElement, "database");

    assertHasParameter(connectionElement.getModel(), "create");
    assertAttributeIsPresent(connectionElement, "create");
    assertValue(connectionElement.findElement("create").get(), "true");

    assertHasParameter(connectionElement.getModel(), "subsubProtocol");
    assertAttributeIsPresent(connectionElement, "subsubProtocol");
    assertValue(connectionElement.findElement("subsubProtocol").get(), "directory");
  }

  @Test
  public void resolveConnectionWithMapParams() throws Exception {
    ComponentConfiguration config = getAppElement(applicationModel, DB_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);
    assertElementName(configElement, "config");

    assertThat(configElement.getConfiguration().isPresent(), is(true));
    assertThat(configElement.getConfiguration().get(), is(equalTo(config)));
    assertThat(configElement.getIdentifier().isPresent(), is(true));
    assertThat(configElement.getIdentifier().get(), is(equalTo(config.getIdentifier())));

    assertThat(configElement.findElement(newIdentifier("oracle-connection", DB_NS)).isPresent(), is(false));

    ComponentConfiguration connection = config.getNestedComponents().get(0);
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertElementName(connectionElement, "derby-connection");
    assertHasParameter(connectionElement.getModel(), "database");
    assertAttributeIsPresent(connectionElement, "database");
    assertHasParameter(connectionElement.getModel(), "create");
    assertAttributeIsPresent(connectionElement, "create");
    assertThat(connectionElement.findElement(newIdentifier("connection-properties", DB_NS)).isPresent(), is(true));

    ComponentConfiguration pooling = connection.getNestedComponents().get(0);
    DslElementModel<ConnectionProviderModel> poolingElement = getChild(connectionElement, pooling);

    assertValue(poolingElement.findElement("maxPoolSize").get(), "10");
    assertValue(poolingElement.findElement("minPoolSize").get(), "0");

    ComponentConfiguration properties = connection.getNestedComponents().get(1);
    DslElementModel<ConnectionProviderModel> propertiesElement = getChild(connectionElement, properties);

    assertThat(propertiesElement.getContainedElements().size(), is(2));
    Optional<DslElementModel> firstKey = propertiesElement.getContainedElements().get(0).findElement(KEY_ATTRIBUTE_NAME);
    assertValue(firstKey.get(), "first");
    Optional<DslElementModel> firstVal = propertiesElement.getContainedElements().get(0).findElement(VALUE_ATTRIBUTE_NAME);
    assertValue(firstVal.get(), "propertyOne");
    Optional<DslElementModel> secondKey = propertiesElement.getContainedElements().get(1).findElement(KEY_ATTRIBUTE_NAME);
    assertValue(secondKey.get(), "second");
    Optional<DslElementModel> secondVal = propertiesElement.getContainedElements().get(1).findElement(VALUE_ATTRIBUTE_NAME);
    assertValue(secondVal.get(), "propertyTwo");
  }

  @Test
  public void resolveConnectionNoExtraParameters() throws Exception {
    ComponentConfiguration config = getAppElement(applicationModel, DB_CONFIG);
    ComponentConfiguration connection = config.getNestedComponents().get(0);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertHasParameter(connectionElement.getModel(), "customDataTypes");
    assertThat(connectionElement.findElement("customDataTypes").isPresent(), is(false));
  }

  @Test
  public void resolutionFailsForNonTopLevelElement() throws Exception {
    ComponentConfiguration config = getAppElement(applicationModel, DB_CONFIG);
    ComponentConfiguration connection = config.getNestedComponents().get(0);

    assertThat(modelResolver.create(connection).isPresent(), is(false));
  }

  @Test
  public void resolveInfrastructureParametersAsElements() throws Exception {
    ComponentConfiguration config = getAppElement(applicationModel, HTTP_LISTENER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);
    assertThat(configElement.findElement(TLS_PARAMETER_NAME).isPresent(), is(true));
    assertThat(configElement.findElement(TLS_PARAMETER_NAME).get().getConfiguration().isPresent(), is(true));

    ComponentConfiguration listener = getAppElement(applicationModel, COMPONENTS_FLOW).getNestedComponents().get(LISTENER_PATH);
    DslElementModel<SourceModel> listenerElement = resolve(listener);
    assertThat(listenerElement.findElement(RECONNECTION_STRATEGY_PARAMETER_NAME).isPresent(), is(true));
    assertThat(listenerElement.findElement(RECONNECTION_STRATEGY_PARAMETER_NAME).get().getConfiguration().isPresent(), is(true));
  }

  @Test
  public void resolveConfigNoExtraContainedElements() throws Exception {
    ComponentConfiguration config = getAppElement(applicationModel, HTTP_LISTENER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    assertThat(configElement.findElement(newIdentifier("request-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConfigWithParameters() throws Exception {
    ComponentConfiguration config = getAppElement(applicationModel, HTTP_LISTENER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    assertElementName(configElement, "listener-config");
    assertHasParameter(configElement.getModel(), "basePath");
    assertAttributeIsPresent(configElement, "basePath");

    ComponentConfiguration connection = config.getNestedComponents().get(0);
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertElementName(connectionElement, "listener-connection");
    assertAttributeIsPresent(connectionElement, "host");
    assertAttributeIsPresent(connectionElement, "port");

    assertThat(configElement.findElement(newIdentifier("request-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void resolveConnectionWithSubtypes() throws Exception {
    ComponentConfiguration config = getAppElement(applicationModel, HTTP_REQUESTER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);

    assertElementName(configElement, "request-config");

    ComponentConfiguration connection = config.getNestedComponents().get(0);
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertElementName(connectionElement, "request-connection");
    assertHasParameter(connectionElement.getModel(), "host");
    assertAttributeIsPresent(connectionElement, "host");
    assertHasParameter(connectionElement.getModel(), "port");
    assertAttributeIsPresent(connectionElement, "port");

    ComponentConfiguration authenticationWrapper = connection.getNestedComponents().get(0);
    DslElementModel<ParameterModel> authenticationWrapperElement = getChild(connectionElement, authenticationWrapper);
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
    ComponentConfiguration config = getAppElement(applicationModel, HTTP_REQUESTER_CONFIG);
    DslElementModel<ConfigurationModel> configElement = resolve(config);
    assertElementName(configElement, "request-config");

    ComponentConfiguration connection = config.getNestedComponents().get(0);
    DslElementModel<ConnectionProviderModel> connectionElement = getChild(configElement, connection);

    assertElementName(connectionElement, "request-connection");
    assertHasParameter(connectionElement.getModel(), "host");
    assertAttributeIsPresent(connectionElement, "host");
    assertHasParameter(connectionElement.getModel(), "port");
    assertAttributeIsPresent(connectionElement, "port");

    ComponentConfiguration propertiesWrapper = connection.getNestedComponents().get(1);
    DslElementModel<ParameterModel> wrapperElement = getChild(connectionElement, propertiesWrapper);
    assertElementName(wrapperElement, "client-socket-properties");

    ComponentConfiguration properties = propertiesWrapper.getNestedComponents().get(0);
    DslElementModel<ObjectType> propertiesElement = getChild(wrapperElement, properties);

    assertElementName(propertiesElement, "tcp-client-socket-properties");
    assertThat(propertiesElement.getDsl().isWrapped(), is(true));
    assertThat(propertiesElement.getDsl().supportsAttributeDeclaration(), is(false));

    assertValue(configElement.findElement("receiveBufferSize").get(), "1024");
    assertValue(configElement.findElement("sendTcpNoDelay").get(), "true");

    assertThat(configElement.findElement(newIdentifier("listener-connection", HTTP_NS)).isPresent(),
               is(false));
  }

  @Test
  public void flowElementsResolution() throws Exception {
    ComponentConfiguration flow = getAppElement(applicationModel, COMPONENTS_FLOW);

    ComponentConfiguration listener = flow.getNestedComponents().get(LISTENER_PATH);
    assertListenerSourceWithMessageBuilder(listener);

    ComponentConfiguration dbBulkInsert = flow.getNestedComponents().get(DB_BULK_INSERT_PATH);
    assertBulkInsertOperationWithNestedList(dbBulkInsert);

    ComponentConfiguration requester = flow.getNestedComponents().get(REQUESTER_PATH);
    assertRequestOperationWithFlatParameters(requester);

    ComponentConfiguration dbInsert = flow.getNestedComponents().get(DB_INSERT_PATH);
    assertInsertOperationWithMaps(dbInsert);
  }

  private void assertInsertOperationWithMaps(ComponentConfiguration dbInsert) {
    DslElementModel<OperationModel> dbElement = resolve(dbInsert);

    assertThat(dbElement.getContainedElements().size(), is(7));

    ComponentConfiguration sql = dbInsert.getNestedComponents().get(0);
    DslElementModel<ParameterModel> sqlElement = getChild(dbElement, sql);
    assertElementName(sqlElement, "sql");

    ComponentConfiguration parameterTypes = dbInsert.getNestedComponents().get(1);
    DslElementModel<ParameterModel> parameterTypesElement = getChild(dbElement, parameterTypes);
    assertElementName(parameterTypesElement, "parameter-types");

    DslElementModel<ObjectType> elementOne = parameterTypesElement.getContainedElements().get(0);
    assertElementName(elementOne, "parameter-type");
    assertValue(elementOne.findElement("key").get(), "description");
    assertValue(elementOne.findElement("type").get(), "CLOB");

    assertValue(dbElement.findElement(newIdentifier("input-parameters", DB_NS)).get(), "#[mel:['description' : payload]]");
  }

  protected void assertRequestOperationWithFlatParameters(ComponentConfiguration requester) {
    DslElementModel<OperationModel> requesterElement = resolve(requester);
    assertHasParameter(requesterElement.getModel(), "path");
    assertThat(requesterElement.findElement("path").isPresent(), is(true));
    assertHasParameter(requesterElement.getModel(), "method");
    assertThat(requesterElement.findElement("method").isPresent(), is(true));
  }

  protected void assertBulkInsertOperationWithNestedList(ComponentConfiguration dbInsert) {
    DslElementModel<OperationModel> bulkInsertElement = resolve(dbInsert);

    assertThat(bulkInsertElement.getContainedElements().size(), is(6));

    assertValue(bulkInsertElement.findElement("parameterValues").get(), "#[payload]");

    ComponentConfiguration sql = dbInsert.getNestedComponents().get(0);
    DslElementModel<ParameterModel> sqlElement = getChild(bulkInsertElement, sql);
    assertElementName(sqlElement, "sql");
    assertValue(sqlElement, "INSERT INTO PLANET(POSITION, NAME) VALUES (:position, :name)");

    ComponentConfiguration parameterTypes = dbInsert.getNestedComponents().get(1);
    DslElementModel<ParameterModel> parameterTypesElement = getChild(bulkInsertElement, parameterTypes);
    assertElementName(parameterTypesElement, "parameter-types");

    ComponentConfiguration parameterOne = parameterTypes.getNestedComponents().get(0);
    assertThat(parameterOne.getParameters().get("key"), is("name"));
    DslElementModel<ObjectType> elementOne = parameterTypesElement.getContainedElements().get(0);
    assertElementName(elementOne, parameterOne.getIdentifier().getName());
    assertValue(elementOne.findElement("key").get(), "name");
    assertValue(elementOne.findElement("type").get(), "VARCHAR");

    ComponentConfiguration parameterTwo = parameterTypes.getNestedComponents().get(1);
    assertThat(parameterTwo.getParameters().get("key"), is("position"));
    DslElementModel<ObjectType> elementTwo = parameterTypesElement.getContainedElements().get(1);
    assertElementName(elementTwo, parameterTwo.getIdentifier().getName());
    assertValue(elementTwo.findElement("key").get(), "position");
    assertValue(elementTwo.findElement("type").get(), "INTEGER");
  }

  protected void assertListenerSourceWithMessageBuilder(ComponentConfiguration listener) {
    DslElementModel<SourceModel> listenerElement = resolve(listener);

    assertHasParameter(listenerElement.getModel(), "path");
    ComponentConfiguration responseBuilder = listener.getNestedComponents().get(1);

    DslElementModel<ParameterModel> responseBuilderElement = getChild(listenerElement, responseBuilder);
    assertElementName(responseBuilderElement, "response");

    assertThat(responseBuilderElement.getDsl().getChild("headers").isPresent(), is(true));
    assertValue(responseBuilderElement.findElement(newIdentifier("headers", HTTP_NS)).get(),
                "#[mel:['content-type' : 'text/plain']]");

    assertValue(listenerElement.findElement("path").get(), "testBuilder");
  }

}
