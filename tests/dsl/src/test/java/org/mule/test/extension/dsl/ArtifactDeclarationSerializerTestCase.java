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
import static org.mule.runtime.extension.api.ExtensionConstants.POOLING_PROFILE_PARAMETER_NAME;
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
import static org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder.REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extensions.jms.api.connection.caching.NoCachingConfiguration;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.app.declaration.fluent.ElementDeclarer;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.spring.dsl.api.ArtifactDeclarationXmlSerializer;
import org.mule.runtime.config.spring.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.test.runner.RunnerDelegateTo;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class ArtifactDeclarationSerializerTestCase extends AbstractElementModelTestCase {

  private String expectedAppXml;

  @Parameterized.Parameter(0)
  public String configFile;

  @Parameterized.Parameter(1)
  public ArtifactDeclaration applicationDeclaration;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {

    return Arrays.asList(new Object[][] {
        {"full-artifact-config-dsl-app.xml", createFullArtifactDeclaration()},
        {"multi-flow-dsl-app.xml", createMultiFlowArtifactDeclaration()}
    });
  }

  @Before
  public void loadExpectedResult() throws IOException {
    expectedAppXml = getResourceAsString(configFile, getClass());
  }

  @Before
  public void setup() throws Exception {
    Set<ExtensionModel> extensions = muleContext.getExtensionManager().getExtensions();
    String core = IOUtils
        .toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/core-extension-model.json"));
    ExtensionModel coreModel = new ExtensionModelJsonSerializer().deserialize(core);

    dslContext = DslResolvingContext.getDefault(ImmutableSet.<ExtensionModel>builder()
        .addAll(extensions).add(coreModel).build());
    modelResolver = DslElementModelFactory.getDefault(dslContext);
  }

  @Test
  public void serialize() throws Exception {
    String serializationResult = ArtifactDeclarationXmlSerializer.getDefault(dslContext).serialize(applicationDeclaration);
    compareXML(expectedAppXml, serializationResult);
  }

  @Test
  public void loadAndserialize() throws Exception {
    InputStream configIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);
    ArtifactDeclarationXmlSerializer serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);

    ArtifactDeclaration artifact = serializer.deserialize(configFile, configIs);

    String serializationResult = serializer.serialize(artifact);
    compareXML(expectedAppXml, serializationResult);
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  private static ArtifactDeclaration createMultiFlowArtifactDeclaration() {
    ElementDeclarer jms = ElementDeclarer.forExtension("JMS");
    ElementDeclarer core = ElementDeclarer.forExtension("Mule Core");

    return newArtifact().withConfig(jms.newConfiguration("config")
        .withRefName("config")
        .withConnection(jms.newConnection("active-mq-connection")
            .withParameter("disableValidation", "true")
            .withParameter("cachingStrategy",
                           newObjectValue()
                               .ofType(NoCachingConfiguration.class.getName())
                               .build())
            .getDeclaration())
        .getDeclaration())
        .withFlow(newFlow("send-payload")
            .withComponent(jms.newOperation("publish")
                .withConfig("config")
                .withParameter("destination", "#[initialDestination]")
                .withParameter("messageBuilder",
                               newObjectValue()
                                   .withParameter("body", "#[payload]")
                                   .withParameter("properties", "#[{(initialProperty): propertyValue}]")
                                   .build())
                .getDeclaration())
            .getDeclaration())
        .withFlow(newFlow("bridge")
            .withComponent(jms.newOperation("consume")
                .withConfig("config")
                .withParameter("destination", "#[initialDestination]")
                .withParameter("maximumWait", "1000")
                .getDeclaration())
            .withComponent(core.newScope("foreach")
                .withRoute(core.newRoute("body")
                    .withComponent(jms.newOperation("publish")
                        .withConfig("config")
                        .withParameter("destination", "#[finalDestination]")
                        .withParameter("messageBuilder",
                                       newObjectValue()
                                           .withParameter("jmsxProperties",
                                                          "#[attributes.properties.jmsxProperties]")
                                           .withParameter("body",
                                                          "#[bridgePrefix ++ payload]")
                                           .withParameter("properties",
                                                          "#[attributes.properties.userProperties]")
                                           .build())
                        .getDeclaration())
                    .withComponent(core.newOperation("logger")
                        .withParameter("message", "Message Sent")
                        .getDeclaration())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .withFlow(newFlow("bridge-receiver")
            .withComponent(jms.newOperation("consume").withConfig("config")
                .withParameter("destination", "#[finalDestination]")
                .withParameter("maximumWait", "1000")
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

  private static ArtifactDeclaration createFullArtifactDeclaration() {

    ElementDeclarer db = ElementDeclarer.forExtension("Database");
    ElementDeclarer http = ElementDeclarer.forExtension("HTTP");
    ElementDeclarer sockets = ElementDeclarer.forExtension("Sockets");
    ElementDeclarer core = ElementDeclarer.forExtension("Mule Core");
    ElementDeclarer wsc = ElementDeclarer.forExtension("Web Service Consumer");

    return newArtifact()
        .withGlobalParameter(db.newGlobalParameter("query")
            .withRefName("selectQuery")
            .withValue(newObjectValue()
                .ofType(QueryDefinition.class.getName())
                .withParameter("sql", "select * from PLANET where name = :name")
                .withParameter("inputParameters", "#[mel:['name' : payload]]")
                .build())
            .getDeclaration())
        .withConfig(db.newConfiguration("config")
            .withRefName("dbConfig")
            .withConnection(db.newConnection("derby-connection")
                .withParameter(POOLING_PROFILE_PARAMETER_NAME, newObjectValue()
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
            .withParameter("initialState", "stopped")
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
                                   .withParameter("headers", "#[{{'content-type' : 'text/plain'}}]")
                                   .withParameter("body", "#[{'my': 'map'}]")
                                   .build())
                .getDeclaration())
            .withComponent(core.newRouter("choice")
                .withRoute(core.newRoute("when")
                    .withParameter("expression", "#[true]")
                    .withComponent(db.newOperation("bulkInsert")
                        .withParameter("sql",
                                       "INSERT INTO PLANET(POSITION, NAME) VALUES (:position, :name)")
                        .withParameter("parameterTypes",
                                       newListValue()
                                           .withValue(newObjectValue()
                                               .withParameter("key", "name")
                                               .withParameter("type", "VARCHAR")
                                               .build())
                                           .withValue(newObjectValue()
                                               .withParameter("key", "position")
                                               .withParameter("type", "INTEGER")
                                               .build())
                                           .build())
                        .getDeclaration())
                    .getDeclaration())
                .withRoute(core.newRoute("otherwise")
                    .withComponent(core.newScope("foreach")
                        .withParameter("collection", "#[myCollection]")
                        .withRoute(core.newRoute("body")
                            .withComponent(core.newOperation("logger")
                                .withParameter("message", "#[payload]")
                                .getDeclaration())
                            .getDeclaration())
                        .getDeclaration())
                    .getDeclaration())
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
                .withParameter("sql",
                               "INSERT INTO PLANET(POSITION, NAME, DESCRIPTION) VALUES (777, 'Pluto', :description)")
                .withParameter("parameterTypes",
                               newListValue()
                                   .withValue(newObjectValue()
                                       .withParameter("key", "description")
                                       .withParameter("type", "CLOB").build())
                                   .build())
                .withParameter("inputParameters", "#[{{'description' : payload}}]")
                .getDeclaration())
            .withComponent(sockets.newOperation("sendAndReceive")
                .withParameter(TARGET_PARAMETER_NAME, "myVar")
                .withParameter(STREAMING_STRATEGY_PARAMETER_NAME,
                               newObjectValue()
                                   .ofType(REPEATABLE_IN_MEMORY_BYTES_STREAM_ALIAS)
                                   .withParameter("bufferSizeIncrement", "8")
                                   .withParameter("bufferUnit", "KB")
                                   .withParameter("initialBufferSize", "51")
                                   .withParameter("maxInMemorySize", "1000")
                                   .build())
                .getDeclaration())
            .withComponent(wsc.newOperation("consume")
                .withParameter("operation", "GetCitiesByCountry")
                .withParameter("message", newObjectValue()
                    .withParameter("attachments", "#[{}]")
                    .withParameter("headers",
                                   "#[{\"headers\": {con#headerIn: \"Header In Value\",con#headerInOut: \"Header In Out Value\"}]")
                    .withParameter("body", "#[payload]")
                    .build())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

}
