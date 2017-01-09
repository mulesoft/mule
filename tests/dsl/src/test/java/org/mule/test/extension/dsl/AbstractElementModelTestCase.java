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
import static org.hamcrest.Matchers.is;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.dsl.config.ArtifactConfiguration;
import org.mule.runtime.api.dsl.config.ComponentConfiguration;
import org.mule.runtime.api.dsl.config.ComponentIdentifier;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.dsl.model.DslElementModel;
import org.mule.runtime.extension.api.dsl.model.DslElementModelResolver;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Optional;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@ArtifactClassLoaderRunnerConfig(
    sharedRuntimeLibs = {"org.apache.derby:derby"},
    plugins = {
        "org.mule.modules:mule-module-sockets",
        "org.mule.modules:mule-module-http-ext",
        "org.mule.modules:mule-module-db",
        "org.mule.modules:mule-module-jms",
        "com.mulesoft.weave:mule-plugin-weave"},
    providedInclusions = "org.mule.modules:mule-module-sockets")
public abstract class AbstractElementModelTestCase extends MuleArtifactFunctionalTestCase {

  protected static final String DB_CONFIG = "dbConfig";
  protected static final String DB_NS = "http://www.mulesoft.org/schema/mule/db";
  protected static final String HTTP_LISTENER_CONFIG = "httpListener";
  protected static final String HTTP_REQUESTER_CONFIG = "httpRequester";
  protected static final String HTTP_NS = "http://www.mulesoft.org/schema/mule/httpn";
  protected static final String COMPONENTS_FLOW = "testFlow";
  protected static final int LISTENER_PATH = 0;
  protected static final int DB_INSERT_PATH = 2;
  protected static final int REQUESTER_PATH = 3;

  protected ApplicationModel applicationModel;
  protected DslElementModelResolver modelResolver;
  protected Document doc;

  @Override
  protected String getConfigFile() {
    return "integration-dsl-app.xml";
  }

  @Before
  public void setup() throws Exception {
    applicationModel = loadApplicationModel();

    Set<ExtensionModel> extensions = muleContext.getExtensionManager().getExtensions();
    modelResolver = DslElementModelResolver.getDefault(extensions);
  }

  // Scaffolding
  protected <T extends NamedObject> DslElementModel<T> resolve(ComponentConfiguration component) {
    Optional<DslElementModel<T>> elementModel = modelResolver.resolve(component);
    assertThat(elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  protected ComponentConfiguration getAppElement(ApplicationModel applicationModel, String name) {
    Optional<ComponentConfiguration> component = applicationModel.findNamedElement(name);
    assertThat(component.isPresent(), is(true));
    return component.get();
  }

  protected <T> DslElementModel<T> getChild(DslElementModel<? extends NamedObject> parent, ComponentConfiguration component) {
    return getChild(parent, component.getIdentifier());
  }

  protected <T> DslElementModel<T> getChild(DslElementModel<? extends NamedObject> parent,
                                            ComponentIdentifier identifier) {
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

  protected ComponentIdentifier newIdentifier(String name, String ns) {
    return ComponentIdentifier.builder().withName(name).withNamespace(ns).build();
  }

  protected void assertHasParameter(ParameterizedModel model, String name) {
    assertThat(model.getAllParameterModels()
        .stream().anyMatch(p -> p.getName().equals(name)), is(true));
  }

  protected void assertAttributeIsPresent(DslElementModel<? extends ParameterizedModel> element, String name) {
    assertHasParameter(element.getModel(), name);
    DslElementModel<NamedObject> databaseParam = getAttribute(element, name);
    assertThat(databaseParam.getDsl().supportsAttributeDeclaration(), is(true));
    assertThat(databaseParam.getDsl().supportsChildDeclaration(), is(false));
  }

  protected void assertElementName(DslElementModel propertiesElement, String name) {
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

  protected void addSchemaLocation(Document document, ExtensionModel extension) {

    XmlDslModel xmlDslModel = extension.getXmlDslModel();
    String location = xmlDslModel.getNamespaceUri() + " " + xmlDslModel.getSchemaLocation();

    Attr schemaLocation = document.getDocumentElement().getAttributeNodeNS("http://www.w3.org/2001/XMLSchema-instance",
                                                                           "schemaLocation");
    if (schemaLocation != null) {
      location = schemaLocation.getValue().concat(" ").concat(location);
    }

    document.getDocumentElement().setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                                                 "xsi:schemaLocation",
                                                 location);
  }

  protected String write() throws TransformerException {
    // write the content into xml file
    TransformerFactory transformerFactory = TransformerFactory.newInstance();

    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

    DOMSource source = new DOMSource(doc);

    // Output for debugging
    StreamResult sout = new StreamResult(System.out);
    transformer.transform(source, sout);

    StringWriter writer = new StringWriter();
    transformer.transform(source, new StreamResult(writer));
    return writer.getBuffer().toString().replaceAll("\n|\r", "");
  }

  protected void initializeMuleApp() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder docBuilder = factory.newDocumentBuilder();

    this.doc = docBuilder.newDocument();
    Element mule = doc.createElement("mule");
    doc.appendChild(mule);
    mule.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "http://www.mulesoft.org/schema/mule/core");
    mule.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation",
                        "http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd");
  }
}
