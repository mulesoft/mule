/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.core.util.IOUtils.getResourceAsString;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModel;
import org.mule.runtime.config.spring.dsl.model.XmlDslElementModelConverter;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class MultiFlowDslModelSerializerTestCase extends AbstractElementModelTestCase {

  private static final String BRIDGE_RECEIVER_FLOW = "bridge-receiver";
  private static final String SEND_PAYLOAD_FLOW = "send-payload";
  private static final String BRIDGE_FLOW = "bridge";
  private Element sendPayload;
  private Element bridge;
  private Element receiver;
  private String expectedAppXml;
  private DslSyntaxResolver jmsDslResolver;

  @Override
  protected String getConfigFile() {
    return "multi-flow-dsl-app.xml";
  }

  @Before
  public void createDocument() throws Exception {
    applicationModel = loadApplicationModel();
    createAppDocument();

    this.sendPayload = createFlow(SEND_PAYLOAD_FLOW);
    this.bridge = createFlow(BRIDGE_FLOW);
    this.receiver = createFlow(BRIDGE_RECEIVER_FLOW);
  }

  private Element createFlow(String name) {
    Element flow = doc.createElement("flow");
    flow.setAttribute("name", name);
    return flow;
  }

  @Before
  public void loadExpectedResult() throws IOException {
    expectedAppXml = getResourceAsString(getConfigFile(), getClass());
  }

  @Test
  public void modelLoaderFromComponentConfiguration() throws Exception {
    ExtensionModel jmsModel = muleContext.getExtensionManager().getExtension("JMS")
        .orElseThrow(() -> new IllegalStateException("Missing Extension JMS"));
    jmsDslResolver = DslSyntaxResolver.getDefault(jmsModel, dslContext);

    DslElementModel<ConfigurationModel> config = resolve(getAppElement(applicationModel, "config"));
    ConfigurationModel jmsConfigModel = jmsModel.getConfigurationModel("config").get();

    assertConfigLoaded(config, jmsConfigModel);
    assertConnectionLoaded(config);

    OperationModel publishModel = jmsConfigModel.getOperationModel("publish").get();
    OperationModel consumeModel = jmsConfigModel.getOperationModel("consume").get();

    assertSendPayloadLoaded(publishModel);
    assertBridgeLoaded(publishModel, consumeModel);
    assertBridgeReceiverLoaded(consumeModel);
  }

  @Test
  public void serializeMultiFlowElementModel() throws Exception {
    XmlDslElementModelConverter converter = XmlDslElementModelConverter.getDefault(this.doc);

    doc.getDocumentElement().appendChild(converter.asXml(resolve(getAppElement(applicationModel, "config"))));

    getAppElement(applicationModel, SEND_PAYLOAD_FLOW).getNestedComponents()
        .forEach(c -> sendPayload.appendChild(converter.asXml(resolve(c))));

    getAppElement(applicationModel, BRIDGE_FLOW).getNestedComponents()
        .forEach(c -> bridge.appendChild(converter.asXml(resolve(c))));

    getAppElement(applicationModel, BRIDGE_RECEIVER_FLOW).getNestedComponents()
        .forEach(c -> receiver.appendChild(converter.asXml(resolve(c))));

    doc.getDocumentElement().appendChild(sendPayload);
    doc.getDocumentElement().appendChild(bridge);
    doc.getDocumentElement().appendChild(receiver);

    String serializationResult = write();
    compareXML(expectedAppXml, serializationResult);
  }

  private void assertSendPayloadLoaded(OperationModel publishModel) {
    List<ComponentConfiguration> sendOperations = getAppElement(applicationModel, SEND_PAYLOAD_FLOW).getNestedComponents();
    assertThat(sendOperations.size(), is(1));

    DslElementModel<OperationModel> publishElement = resolve(sendOperations.get(0));
    assertThat(publishElement.getModel(), is(publishModel));
    assertThat(publishElement.getDsl(), is(jmsDslResolver.resolve(publishModel)));

    // attributes are present in the parent and its model is reachable, but no componentConfiguration is required
    assertThat(publishElement.getConfiguration().get().getParameters().get("destination"), is("#[initialDestination]"));
    assertThat(publishElement.findElement("destination").get().getConfiguration().isPresent(), is(false));
    assertThat(publishElement.findElement("destination").get().getModel(), is(findParameter("destination", publishModel)));

    // child element contains its configuration element along with its content
    DslElementModel<Object> builderElement = publishElement.findElement("messageBuilder").get();
    assertThat(builderElement.getModel(), is(findParameter("messageBuilder", publishModel)));
    Optional<ComponentConfiguration> messageBuilder = builderElement.getConfiguration();
    assertThat(messageBuilder.isPresent(), is(true));

    assertThat(messageBuilder.get().getNestedComponents().size(), is(1));
    assertThat(messageBuilder.get().getNestedComponents().get(0).getValue().get().trim(),
               is("#[{(initialProperty): propertyValue}]"));
  }

  private void assertBridgeLoaded(OperationModel publishModel, OperationModel consumeModel) {
    List<ComponentConfiguration> bridgeOperation = getAppElement(applicationModel, BRIDGE_FLOW).getNestedComponents();
    assertThat(bridgeOperation.size(), is(2));

    DslElementModel<OperationModel> consumeElement = resolve(bridgeOperation.get(0));
    DslElementModel<OperationModel> publishElement = resolve(bridgeOperation.get(1));
    assertThat(consumeElement.getModel(), is(consumeModel));
    assertThat(consumeElement.getDsl(), is(jmsDslResolver.resolve(consumeModel)));

    assertThat(consumeElement.getConfiguration().get().getParameters().get("destination"), is("#[initialDestination]"));
    assertThat(consumeElement.findElement("destination").get().getConfiguration().isPresent(), is(false));
    assertThat(consumeElement.findElement("destination").get().getModel(), is(findParameter("destination", consumeModel)));


    assertThat(publishElement.getModel(), is(publishModel));
    assertThat(publishElement.getDsl(), is(jmsDslResolver.resolve(publishModel)));

    assertThat(publishElement.findElement("destination").get().getModel(), is(findParameter("destination", publishModel)));
    assertThat(publishElement.findElement("destination").get().getConfiguration().isPresent(), is(false));
    assertThat(publishElement.getConfiguration().get().getParameters().get("destination"), is("#[finalDestination]"));

    DslElementModel<Object> builderElement = publishElement.findElement("messageBuilder").get();
    assertThat(builderElement.getModel(), is(findParameter("messageBuilder", publishModel)));
    Optional<ComponentConfiguration> messageBuilder = builderElement.getConfiguration();
    assertThat(messageBuilder.isPresent(), is(true));

  }

  private void assertBridgeReceiverLoaded(OperationModel consumeModel) {
    List<ComponentConfiguration> consumeOperation = getAppElement(applicationModel, BRIDGE_RECEIVER_FLOW).getNestedComponents();
    assertThat(consumeOperation.size(), is(1));

    DslElementModel<OperationModel> consumeElement = resolve(consumeOperation.get(0));
    assertThat(consumeElement.getModel(), is(consumeModel));
    assertThat(consumeElement.getDsl(), is(jmsDslResolver.resolve(consumeModel)));

    assertThat(consumeElement.findElement("destination").get().getModel(), is(findParameter("destination", consumeModel)));
    assertThat(consumeElement.findElement("destination").get().getConfiguration().isPresent(), is(false));
    assertThat(consumeElement.getConfiguration().get().getParameters().get("destination"), is("#[finalDestination]"));

    assertThat(consumeElement.findElement("ackMode").isPresent(), is(false));
  }

  private void assertConfigLoaded(DslElementModel<ConfigurationModel> config, ConfigurationModel jmsConfigModel) {
    assertThat(config.getModel(), is(jmsConfigModel));
    assertThat(config.getDsl(), is(jmsDslResolver.resolve(jmsConfigModel)));
  }

  private void assertConnectionLoaded(DslElementModel<ConfigurationModel> config) {
    assertThat(config.getContainedElements().size(), is(2));
    assertThat(config.findElement("active-mq-connection").isPresent(), is(true));
    assertThat(config.findElement("active-mq-connection").get().getContainedElements().size(), is(2));
  }

  private ParameterModel findParameter(String name, ParameterizedModel model) {
    return model.getAllParameterModels().stream().filter(p -> p.getName().equals(name)).findFirst().get();
  }

  protected String getExpectedSchemaLocation() {
    return "http://www.mulesoft.org/schema/mule/jmsn http://www.mulesoft.org/schema/mule/jmsn/current/mule-jmsn.xsd http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
  }
}
