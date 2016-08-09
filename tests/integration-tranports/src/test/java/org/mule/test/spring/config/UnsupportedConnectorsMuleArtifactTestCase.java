/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.common.MuleArtifact;
import org.mule.common.MuleArtifactFactoryException;
import org.mule.common.Testable;
import org.mule.common.config.XmlConfigurationCallback;
import org.mule.common.config.XmlConfigurationMuleArtifactFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;
import java.util.Map;
import java.util.ServiceLoader;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 */
public class UnsupportedConnectorsMuleArtifactTestCase extends AbstractMuleTestCase {

  private XmlConfigurationMuleArtifactFactory lookupArtifact;
  private MuleArtifact artifact = null;

  @Before
  public void before() {
    lookupArtifact = lookupArtifact();
  }

  @After
  public void after() {
    if (artifact != null) {
      lookupArtifact.returnArtifact(artifact);
    }
  }

  @Test
  public void unsupportedConnectorsHttp() throws SAXException, IOException, MuleArtifactFactoryException {
    // HTTP
    checkUnsupportedConnector("<http:connector name=\"HttpConnector\" xmlns:http=\"http://www.mulesoft.org/schema/mule/transport/http\"/>");
  }

  @Test
  public void unsupportedConnectorsPollingHttp() throws SAXException, IOException, MuleArtifactFactoryException {
    // Polling HTTP
    checkUnsupportedConnector("<http:polling-connector name=\"PollingHttpConnector\"\n"
        + "        pollingFrequency=\"30000\" reuseAddress=\"true\" xmlns:http=\"http://www.mulesoft.org/schema/mule/transport/http\"/>");
  }

  @Test
  public void unsupportedConnectorsHttps() throws SAXException, IOException, MuleArtifactFactoryException {
    // HTTPS
    checkUnsupportedConnector("<https:connector name=\"httpConnector\" xmlns:https=\"http://www.mulesoft.org/schema/mule/transport/https\">\n"
        + "        <https:tls-key-store path=\"~/ce/tests/integration/src/test/resources/muletest.keystore\" keyPassword=\"mulepassword\" storePassword=\"mulepassword\"/>\n"
        + "</https:connector>");
  }

  @Test
  public void unsupportedConnectorsJms() throws SAXException, IOException, MuleArtifactFactoryException {
    // JMS
    checkUnsupportedConnector("<jms:activemq-connector name=\"Active_MQ\" brokerURL=\"vm://localhost\" "
        + "validateConnections=\"true\" xmlns:jms=\"http://www.mulesoft.org/schema/mule/transport/jms\"/>");
  }

  @Test
  public void unsupportedConnectorsVm() throws SAXException, IOException, MuleArtifactFactoryException {
    // VM
    checkUnsupportedConnector("<vm:connector name=\"memory\" "
        + "xmlns:vm=\"http://www.mulesoft.org/schema/mule/transport/vm\"/>");
  }

  private void checkUnsupportedConnector(String connectorConfig) throws IOException, SAXException, MuleArtifactFactoryException {
    Document document = XMLUnit.buildControlDocument(connectorConfig);
    artifact = lookupArtifact.getArtifact(document.getDocumentElement(), getXmlConfigurationCallbackForUnsupportedConnector());

    assertThat(artifact, not(nullValue()));
    assertThat(artifact.hasCapability(Testable.class), is(false));
    assertThat(artifact.getCapability(Testable.class), nullValue());
  }

  private XmlConfigurationCallback getXmlConfigurationCallbackForUnsupportedConnector() {
    return new XmlConfigurationCallback() {

      @Override
      public Element getGlobalElement(String s) {
        return null;
      }

      @Override
      public String getSchemaLocation(String s) {
        if (s != null) {
          int connectorNameStart = s.lastIndexOf("/") + 1;
          s = s + "/current/mule-transport-" + s.substring(connectorNameStart) + ".xsd";
          return s;
        } else
          return null;
      }

      @Override
      public Element[] getPropertyPlaceholders() {
        return new Element[0];
      }

      @Override
      public Map<String, String> getEnvironmentProperties() {
        return null;
      }
    };

  }

  protected static XmlConfigurationMuleArtifactFactory lookupArtifact() {
    return ServiceLoader.load(XmlConfigurationMuleArtifactFactory.class).iterator().next();
  }
}
