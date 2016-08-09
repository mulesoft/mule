/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.common.MuleArtifact;
import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.Testable;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.runtime.config.spring.SpringXmlConfigurationMuleArtifactFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;

import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.io.DOMWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class MessageSourceMuleArtifactTestCase extends AbstractMuleTestCase {

  private static final String TEST_SCHEMA_URL = "http://www.mulesoft.org/schema/mule/test";

  private SpringXmlConfigurationMuleArtifactFactory factory;
  private MuleArtifact artifact;

  @Before
  public void before() {
    factory = new SpringXmlConfigurationMuleArtifactFactory();
  }

  @After
  public void after() {
    if (factory != null) {
      factory.returnArtifact(artifact);
    }
  }

  @Test
  public void createsMessageSourceArtifact() throws MuleArtifactFactoryException, DocumentException {
    XmlConfigurationCallback callback = mock(XmlConfigurationCallback.class);
    HashMap<String, String> map = new HashMap<>();
    map.put("test", "test1");
    when(callback.getEnvironmentProperties()).thenReturn(map);
    when(callback.getPropertyPlaceholders()).thenReturn(new Element[] {});
    when(callback.getSchemaLocation(TEST_SCHEMA_URL))
        .thenReturn("http://www.mulesoft.org/schema/mule/test/current/mule-test.xsd");
    Element element = createElement("component", TEST_SCHEMA_URL, "test");
    element.setAttribute("throwException", "true");

    artifact = factory.getArtifactForMessageProcessor(element, callback);

    assertThat(artifact.hasCapability(Testable.class), is(false));
  }

  private Element createElement(String name, String namespace, String prefix) throws DocumentException {
    org.dom4j.Element dom4jElement = DocumentHelper.createElement(new QName(name, new Namespace(prefix, namespace)));
    org.dom4j.Document dom4jDocument = dom4jElement.getDocument();
    if (dom4jDocument == null) {
      dom4jDocument = DocumentHelper.createDocument();
      dom4jDocument.setRootElement(dom4jElement);
    }

    final DOMWriter writer = new DOMWriter();
    Document w3cDocument = writer.write(dom4jDocument);
    Element w3cElement = w3cDocument.getDocumentElement();


    return w3cElement;
  }
}
