/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.mule.runtime.config.api.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.api.dsl.processor.ConfigLine;
import org.mule.runtime.config.api.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.config.internal.dsl.model.ComponentModelReader;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.custommonkey.xmlunit.XMLUnit.*;
import static org.mule.runtime.config.api.XmlConfigurationDocumentLoader.noValidationDocumentLoader;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.ComponentModelReaderHelper.PASSWORD_MASK;


public class ComponentModelReaderHelperTestCase {

  @Test
  public void testSimpleApp() throws Exception {
    String applicationXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xsi:schemaLocation=\"\n" +
        "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\">\n" +
        "\n" +
        "    <flow name=\"test\">\n" +
        "        <logger category=\"SOMETHING\" level=\"WARN\" message=\"logging info\"/>\n" +
        "    </flow>\n" +
        "\n" +
        "</mule>";
    compareXML(applicationXml, applicationXml);
  }

  @Test
  public void testAppWithCData() throws Exception {
    String applicationXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xmlns:ee=\"http://www.mulesoft.org/schema/mule/ee/core\"\n" +
        "      xsi:schemaLocation=\"\n" +
        "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n" +
        "       http://www.mulesoft.org/schema/mule/ee/core http://www.mulesoft.org/schema/mule/ee/core/current/mule-ee.xsd\">\n"
        +
        "\n" +
        "    <flow name=\"test\">\n" +
        "            <ee:transform>\n" +
        "                <ee:message>\n" +
        "                    <ee:set-payload><![CDATA[\n" +
        "                    %dw 2.0\n" +
        "                    output application/json encoding='UTF-8'\n" +
        "                    ---\n" +
        "                    {\n" +
        "                        'name' : 'Rick',\n" +
        "                        'lastname' : 'Sanchez'\n" +
        "                    }\n" +
        "                    ]]></ee:set-payload>\n" +
        "                </ee:message>\n" +
        "            </ee:transform>" +
        "    </flow>\n" +
        "\n" +
        "</mule>";

    compareXML(applicationXml, applicationXml);
  }

  @Test
  public void testAppWithPasswordInOperation() throws Exception {
    String format = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xmlns:echo=\"http://www.mulesoft.org/schema/mule/module-echo\"\n" +
        "      xsi:schemaLocation=\"\n" +
        "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n" +
        "       http://www.mulesoft.org/schema/mule/module-echo http://www.mulesoft.org/schema/mule/module-echo/current/mule-module-echo.xsd\">\n"
        +
        "\n" +
        "    <flow name=\"test\">\n" +
        "        <echo:data-type value=\"#[payload]\" password=\"%s\" />\n" +
        "    </flow>\n" +
        "\n" +
        "</mule>";
    String applicationXml = String.format(format, "THIS IS THE PASSWORD ATTRIBUTE");
    String expectedXml = String.format(format, PASSWORD_MASK);
    compareXML(applicationXml, expectedXml);
  }

  @Test
  public void testAppWithPasswordInGlobalElement() throws Exception {
    String format = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<mule xmlns=\"http://www.mulesoft.org/schema/mule/core\"\n" +
        "      xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
        "      xmlns:echo=\"http://www.mulesoft.org/schema/mule/module-echo\"\n" +
        "      xsi:schemaLocation=\"\n" +
        "       http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd\n" +
        "       http://www.mulesoft.org/schema/mule/module-echo http://www.mulesoft.org/schema/mule/module-echo/current/mule-module-echo.xsd\">\n"
        +
        "\n" +
        "    <echo:config name=\"echoConfig\" username=\"Rick\" password=\"%s\">\n" +
        "        <echo:connection anotherUsername=\"Morty\" password=\"%s\"/>\n" +
        "    </echo:config>\n" +
        "    <flow name=\"test\">\n" +
        "        <echo:data-type config-ref=\"echoConfig\" value=\"#[payload]\"/>\n" +
        "    </flow>" +
        "\n" +
        "</mule>";
    String applicationXml = String.format(format, "Sanchez", "Smith");
    String expectedXml = String.format(format, PASSWORD_MASK, PASSWORD_MASK);
    compareXML(applicationXml, expectedXml);
  }

  private void compareXML(String inputXml, String expectedXml) throws Exception {
    ComponentModel componentModel = getComponentModel(inputXml);
    String actualXml = ComponentModelReaderHelper.toXml(componentModel);

    setNormalizeWhitespace(true);
    setIgnoreWhitespace(true);
    setIgnoreComments(false);
    setIgnoreAttributeOrder(false);

    Diff diff = XMLUnit.compareXML(expectedXml, actualXml);
    if (!(diff.similar() && diff.identical())) {
      System.out.println(actualXml);
      DetailedDiff detDiff = new DetailedDiff(diff);
      @SuppressWarnings("rawtypes")
      List differences = detDiff.getAllDifferences();
      StringBuilder diffLines = new StringBuilder();
      for (Object object : differences) {
        Difference difference = (Difference) object;
        diffLines.append(difference.toString() + '\n');
      }
      throw new IllegalArgumentException("Actual XML differs from expected: \n" + diffLines.toString());
    }
  }

  private ComponentModel getComponentModel(String applicationXml) {
    String filename = "file-app-config-name.xml";
    InputStream inputStream = toInputStream(applicationXml, UTF_8);
    XmlConfigurationDocumentLoader xmlConfigurationDocumentLoader = noValidationDocumentLoader();
    Document moduleDocument = xmlConfigurationDocumentLoader.loadDocument(emptySet(), filename, inputStream);

    XmlApplicationParser xmlApplicationParser = XmlApplicationParser.createFromExtensionModels(emptySet());
    Optional<ConfigLine> parseModule = xmlApplicationParser.parse(moduleDocument.getDocumentElement());
    if (!parseModule.isPresent()) {
      throw new IllegalArgumentException("There was an issue trying to read the stream of the test");
    }
    final ConfigLine configLine = parseModule.get();
    final ConfigurationPropertiesResolver externalPropertiesResolver = new ConfigurationPropertiesResolver() {

      @Override
      public Object resolveValue(String value) {
        return value;
      }

      @Override
      public Object resolvePlaceholderKeyValue(String placeholderKey) {
        return placeholderKey;
      }
    };
    final ComponentModelReader componentModelReader = new ComponentModelReader(externalPropertiesResolver);
    return componentModelReader.extractComponentDefinitionModel(configLine, filename);
  }
}
