/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newArtifact;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newFlow;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.api.app.declaration.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.core.util.IOUtils.getResourceAsString;
import static org.mule.runtime.extension.api.ExtensionConstants.DISABLE_CONNECTION_VALIDATION_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.RECONNECTION_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.REDELIVERY_POLICY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.TLS_PARAMETER_NAME;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.BLOCKING;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.COUNT;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.FREQUENCY;
import static org.mule.runtime.extension.api.declaration.type.ReconnectionStrategyTypeBuilder.RECONNECT_ALIAS;
import static org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder.MAX_REDELIVERY_COUNT;
import static org.mule.runtime.extension.api.declaration.type.RedeliveryPolicyTypeBuilder.USE_SECURE_HASH;
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_STREAM_ALIAS;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.app.declaration.FlowElementDeclaration;
import org.mule.runtime.api.app.declaration.fluent.ElementDeclarer;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModel;
import org.mule.runtime.config.spring.dsl.model.XmlDslElementModelConverter;

import java.io.IOException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class DeclarationBasedDslElementModelSerializerTestCase extends AbstractElementModelTestCase {

  private String expectedAppXml;
  private ArtifactDeclaration applicationDeclaration;

  @Before
  public void setupArtifact() throws Exception {
    createAppDeclaration();
    createAppDocument();
  }

  @Before
  public void loadExpectedResult() throws IOException {
    expectedAppXml = getResourceAsString(getConfigFile(), getClass());
  }

  @Override
  protected String getConfigFile() {
    return "integration-multi-config-dsl-app.xml";
  }

  private void createAppDeclaration() {

    ElementDeclarer db = ElementDeclarer.forExtension("Database");
    ElementDeclarer http = ElementDeclarer.forExtension("HTTP");
    ElementDeclarer sockets = ElementDeclarer.forExtension("Sockets");

    applicationDeclaration = newArtifact()
        .withConfig(db.newConfiguration("config")
            .withRefName("dbConfig")
            .withConnection(db.newConnection("derby-connection")
                .withParameter("poolingProfile", newObjectValue()
                    .withParameter("maxPoolSize", "10")
                    .build())
                .withParameter("connectionProperties", newObjectValue()
                    .withParameter("first", "propertyOne")
                    .withParameter("second", "propertyTwo")
                    .build())
                .withParameter("database", "target/muleEmbeddedDB")
                .withParameter("create", "true")
                .getDeclaration())
            .getDeclaration())
        .withConfig(http.newConfiguration("listener-config")
            .withRefName("httpListener")
            .withParameter("basePath", "/")
            .withConnection(http.newConnection("listener-connection")
                .withParameter(DISABLE_CONNECTION_VALIDATION_PARAMETER_NAME, "true")
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
                .getDeclaration())
            .getDeclaration())
        .withConfig(http.newConfiguration("request-config")
            .withRefName("httpRequester")
            .withConnection(http.newConnection("request-connection")
                .withParameter("host", "localhost")
                .withParameter("port", "49020")
                .withParameter("authentication",
                               newObjectValue()
                                   .ofType(
                                           "org.mule.extension.http.api.request.authentication.BasicAuthentication")
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
                .getDeclaration())
            .getDeclaration())
        .withFlow(newFlow("testFlow")
            .withInitialState("stopped")
            .withComponent(http.newSource("listener")
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
                .getDeclaration())
            .withComponent(db.newOperation("bulkInsert")
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
                .getDeclaration())
            .withComponent(http.newOperation("request")
                .withConfig("httpRequester")
                .withParameter("path", "/nested")
                .withParameter("method", "POST")
                .getDeclaration())
            .withComponent(db.newOperation("insert")
                .withConfig("dbConfig")
                .withParameter("sql", "INSERT INTO PLANET(POSITION, NAME, DESCRIPTION) VALUES (777, 'Pluto', :description)")
                .withParameter("parameterTypes",
                               newListValue()
                                   .withValue(newObjectValue()
                                       .withParameter("key", "description")
                                       .withParameter("type", "CLOB").build())
                                   .build())
                .withParameter("inputParameters", "#[mel:['description' : payload]]")
                .getDeclaration())
            .withComponent(sockets.newOperation("sendAndReceive")
                .withParameter(TARGET_PARAMETER_NAME, "myVar")
                .withParameter(STREAMING_STRATEGY_PARAMETER_NAME,
                               newObjectValue()
                                   .ofType(REPEATABLE_IN_MEMORY_STREAM_ALIAS)
                                   .withParameter("bufferSizeIncrement", "8")
                                   .withParameter("bufferUnit", "KB")
                                   .withParameter("initialBufferSize", "51")
                                   .withParameter("maxInMemorySize", "1000")
                                   .build())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

  @Test
  public void serialize() throws Exception {
    XmlDslElementModelConverter converter = XmlDslElementModelConverter.getDefault(this.doc);

    applicationDeclaration.getConfigs()
        .forEach(declaration -> {
          Optional<DslElementModel<ParameterizedModel>> e = modelResolver.create(declaration);
          doc.getDocumentElement().appendChild(converter.asXml(e.orElse(null)));
        });

    applicationDeclaration.getFlows()
        .forEach(flowDeclaration -> {
          Element flow = createFlowNode(flowDeclaration);
          flowDeclaration.getComponents()
              .forEach(component -> {
                Optional<DslElementModel<ParameterizedModel>> e = modelResolver.create(component);
                flow.appendChild(converter.asXml(e.orElse(null)));
              });
        });

    String serializationResult = write();

    compareXML(expectedAppXml, serializationResult);
  }

  private Element createFlowNode(FlowElementDeclaration flowDeclaration) {
    Element flow = doc.createElement("flow");
    flow.setAttribute("name", flowDeclaration.getName());
    flow.setAttribute("initialState", flowDeclaration.getInitialState());

    doc.getDocumentElement().appendChild(flow);
    return flow;
  }

}
