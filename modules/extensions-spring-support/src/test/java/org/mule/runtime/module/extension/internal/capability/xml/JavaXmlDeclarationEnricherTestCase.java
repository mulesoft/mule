/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaXmlDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class JavaXmlDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private static final String NAMESPACE = "namespace";
  private static final String NAMESPACE_LOCATION = "NAMESPACE_LOCATION";
  private static final String DEFAULT_NAMESPACE_LOCATION_MASK = "http://www.mulesoft.org/schema/mule/%s";
  private static final String DEFAULT_SCHEMA_LOCATION_MASK = "%s/current/%s";
  private static final String XSD_FILENAME_MASK = "mule-%s.xsd";

  private static final String EXTENSION = "Extension";
  private static final String EXTENSION_NAME = "Xml Model " + EXTENSION;
  private static final String EXTENSION_HYPHENAZED_NAME = "xml-model";
  private static final String EXTENSION_VERSION = "3.7";

  private ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer();
  private DeclarationEnricher declarationEnricher = new JavaXmlDeclarationEnricher();

  @Test
  public void enrichWithCustomValues() {
    extensionDeclarer.named(EXTENSION_NAME).onVersion(EXTENSION_VERSION);
    XmlDslModel dslModel = enrich(XmlSupport.class);

    assertThat(dslModel, is(notNullValue()));
    assertThat(dslModel.getSchemaVersion(), is(EXTENSION_VERSION));
    assertThat(dslModel.getPrefix(), is(NAMESPACE));
    assertThat(dslModel.getNamespace(), is(NAMESPACE_LOCATION));
    assertThat(dslModel.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, NAMESPACE)));
    assertThat(dslModel.getSchemaLocation(),
               is(String.format(DEFAULT_SCHEMA_LOCATION_MASK, NAMESPACE_LOCATION, String.format(XSD_FILENAME_MASK, NAMESPACE))));
  }

  @Test
  public void enrichWithDefaultValues() {
    extensionDeclarer.named(EXTENSION_NAME).onVersion(EXTENSION_VERSION);
    XmlDslModel dslModel = enrich(NoXmlSupport.class);

    assertThat(dslModel, is(notNullValue()));
    assertThat(dslModel.getSchemaVersion(), is(EXTENSION_VERSION));
    assertThat(dslModel.getPrefix(), is(EXTENSION_HYPHENAZED_NAME));
    assertThat(dslModel.getNamespace(), equalTo(String.format(DEFAULT_NAMESPACE_LOCATION_MASK, EXTENSION_HYPHENAZED_NAME)));
    assertThat(dslModel.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, EXTENSION_HYPHENAZED_NAME)));
    assertThat(dslModel.getSchemaLocation(),
               is(String.format(DEFAULT_SCHEMA_LOCATION_MASK,
                                String.format(DEFAULT_NAMESPACE_LOCATION_MASK, EXTENSION_HYPHENAZED_NAME),
                                String.format(XSD_FILENAME_MASK, EXTENSION_HYPHENAZED_NAME))));
  }

  @Test
  public void enrichWithCustomNamespaceValue() {
    extensionDeclarer.named(EXTENSION_NAME).onVersion(EXTENSION_VERSION);
    XmlDslModel dslModel = enrich(DefaultXmlExtension.class);

    assertThat(dslModel, is(notNullValue()));
    assertThat(dslModel.getSchemaVersion(), is(EXTENSION_VERSION));
    assertThat(dslModel.getPrefix(), is(NAMESPACE));
    assertThat(dslModel.getNamespace(), equalTo(String.format(DEFAULT_NAMESPACE_LOCATION_MASK, NAMESPACE)));
    assertThat(dslModel.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, NAMESPACE)));
    assertThat(dslModel.getSchemaLocation(),
               is(String.format(DEFAULT_SCHEMA_LOCATION_MASK, String.format(DEFAULT_NAMESPACE_LOCATION_MASK, NAMESPACE),
                                String.format(XSD_FILENAME_MASK, NAMESPACE))));
  }

  @Test
  public void enrichWithCustomSchemaLocationValue() {
    extensionDeclarer.named(EXTENSION).onVersion(EXTENSION_VERSION);
    XmlDslModel dslModel = enrich(CustomSchemaLocationXmlExtension.class);

    assertThat(dslModel, is(notNullValue()));
    assertThat(dslModel.getSchemaVersion(), is(EXTENSION_VERSION));
    assertThat(dslModel.getPrefix(), is(EXTENSION.toLowerCase()));
    assertThat(dslModel.getNamespace(), equalTo(NAMESPACE_LOCATION));
    assertThat(dslModel.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, hyphenize(EXTENSION))));
    assertThat(dslModel.getSchemaLocation(), is(String.format(DEFAULT_SCHEMA_LOCATION_MASK, NAMESPACE_LOCATION,
                                                              String.format(XSD_FILENAME_MASK, hyphenize(EXTENSION)))));
  }

  private XmlDslModel enrich(Class<?> type) {
    extensionDeclarer.withModelProperty(new ImplementingTypeModelProperty(type));
    declarationEnricher
        .enrich(new DefaultExtensionLoadingContext(extensionDeclarer, type.getClassLoader(), getDefault(emptySet())));
    return extensionDeclarer.getDeclaration().getXmlDslModel();
  }

  @Xml(prefix = NAMESPACE, namespace = NAMESPACE_LOCATION)
  private static class XmlSupport {

  }

  @Xml(prefix = NAMESPACE)
  private static class DefaultXmlExtension {

  }

  @Xml(namespace = NAMESPACE_LOCATION)
  private static class CustomSchemaLocationXmlExtension {

  }

  private static class NoXmlSupport {

  }
}
