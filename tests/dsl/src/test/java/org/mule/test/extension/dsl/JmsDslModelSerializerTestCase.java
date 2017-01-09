/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.mule.runtime.core.util.IOUtils.getResourceAsString;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.extension.api.dsl.converter.XmlDslElementModelConverter;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

public class JmsDslModelSerializerTestCase extends AbstractElementModelTestCase {

  public static final String SEND_PAYLOAD_FLOW = "send-payload";
  public static final String BRIDGE_FLOW = "bridge";
  public static final String BRIDGE_RECEIVER_FLOW = "bridge-receiver";
  private Element sendPayload;
  private Element bridge;
  private Element receiver;
  private String expectedAppXml;

  @Override
  protected String getConfigFile() {
    return "jms-dsl-app.xml";
  }

  @Before
  public void createDocument() throws Exception {
    initializeMuleApp();

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
  public void serialize() throws Exception {
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

    muleContext.getExtensionManager().getExtensions().forEach(e -> addSchemaLocation(doc, e));

    String serializationResult = write();

    compareXML(expectedAppXml, serializationResult);
  }

}
