/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.app.declaration.ElementDeclaration;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModelFactory;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.component.config.ComponentIdentifier;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Before;
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
  protected static final int DB_BULK_INSERT_PATH = 2;
  protected static final int REQUESTER_PATH = 3;
  protected static final int DB_INSERT_PATH = 4;

  protected DslResolvingContext dslContext;
  protected DslElementModelFactory modelResolver;
  protected ApplicationModel applicationModel;
  protected Document doc;

  @Before
  public void setup() throws Exception {
    dslContext = DslResolvingContext.getDefault(muleContext.getExtensionManager().getExtensions());
    modelResolver = DslElementModelFactory.getDefault(dslContext);
  }

  // Scaffolding
  protected <T extends NamedObject> DslElementModel<T> resolve(ComponentConfiguration component) {
    Optional<DslElementModel<T>> elementModel = modelResolver.create(component);
    assertThat(elementModel.isPresent(), is(true));
    return elementModel.get();
  }

  protected <T extends NamedObject> DslElementModel<T> resolve(ElementDeclaration component) {
    Optional<DslElementModel<T>> elementModel = modelResolver.create(component);
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

  protected <T> DslElementModel<T> getChild(DslElementModel<? extends NamedObject> parent,
                                            String name) {
    Optional<DslElementModel<T>> elementModel = parent.findElement(name);
    assertThat(format("Failed fetching child '%s' from parent '%s'", name,
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
  protected ApplicationModel loadApplicationModel() throws Exception {
    InputStream appIs = Thread.currentThread().getContextClassLoader().getResourceAsStream(getConfigFile());
    checkArgument(appIs != null, "The given application was not found as resource");

    Document document = new XmlConfigurationDocumentLoader()
        .loadDocument(of(muleContext.getExtensionManager()), getConfigFile(), appIs);

    ConfigLine configLine = new XmlApplicationParser(new SpiServiceRegistry())
        .parse(document.getDocumentElement())
        .orElseThrow(() -> new Exception("Failed to load config"));

    ArtifactConfig artifactConfig = new ArtifactConfig.Builder()
        .addConfigFile(new ConfigFile(getConfigFile(), singletonList(configLine)))
        .build();

    return new ApplicationModel(artifactConfig, new ArtifactDeclaration());
  }

  protected String write() throws Exception {
    // write the content into xml file
    TransformerFactory transformerFactory = TransformerFactory.newInstance();

    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

    DOMSource source = new DOMSource(doc);

    StringWriter writer = new StringWriter();
    transformer.transform(source, new StreamResult(writer));
    return writer.getBuffer().toString().replaceAll("\n|\r", "");
  }

  protected void createAppDocument() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    DocumentBuilder docBuilder = factory.newDocumentBuilder();

    this.doc = docBuilder.newDocument();
    Element mule = doc.createElement("mule");
    doc.appendChild(mule);
    mule.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", "http://www.mulesoft.org/schema/mule/core");
    mule.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance",
                        "xsi:schemaLocation", getExpectedSchemaLocation());
  }

  protected String getExpectedSchemaLocation() {
    return "http://www.mulesoft.org/schema/mule/core http://www.mulesoft.org/schema/mule/core/current/mule.xsd http://www.mulesoft.org/schema/mule/tls http://www.mulesoft.org/schema/mule/tls/current/mule-tls.xsd http://www.mulesoft.org/schema/mule/sockets http://www.mulesoft.org/schema/mule/sockets/current/mule-sockets.xsd http://www.mulesoft.org/schema/mule/db http://www.mulesoft.org/schema/mule/db/current/mule-db.xsd http://www.mulesoft.org/schema/mule/httpn http://www.mulesoft.org/schema/mule/httpn/current/mule-httpn.xsd";
  }

  protected void assertValue(DslElementModel elementModel, String value) {
    assertThat(elementModel.getValue().get(), is(value));
  }

}
