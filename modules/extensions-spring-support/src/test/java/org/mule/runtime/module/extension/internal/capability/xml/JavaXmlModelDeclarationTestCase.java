/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.declarerFor;

import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class JavaXmlModelDeclarationTestCase extends AbstractMuleTestCase {

  private static final String NAMESPACE = "namespace";
  private static final String NAMESPACE_LOCATION = "NAMESPACE_LOCATION";
  private static final String DEFAULT_NAMESPACE_LOCATION_MASK = "http://www.mulesoft.org/schema/mule/%s";
  private static final String DEFAULT_SCHEMA_LOCATION_MASK = "%s/current/%s";
  private static final String XSD_FILENAME_MASK = "mule-%s.xsd";

  private static final String EXTENSION = "Extension";
  private static final String EXTENSION_NAME = "Xml Model " + EXTENSION;
  private static final String EXTENSION_HYPHENAZED_NAME = "xml-model";
  private static final String EXTENSION_VERSION = "3.7";

  @Test
  public void withCustomValues() {
    ExtensionDeclaration extensionDeclaration = getExtensionDeclaration(XmlSupport.class);
    XmlDslModel dslModel = extensionDeclaration.getXmlDslModel();

    assertThat(dslModel, is(notNullValue()));
    assertThat(dslModel.getSchemaVersion(), is(EXTENSION_VERSION));
    assertThat(dslModel.getPrefix(), is(NAMESPACE));
    assertThat(dslModel.getNamespace(), is(NAMESPACE_LOCATION));
    assertThat(dslModel.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, NAMESPACE)));
    assertThat(dslModel.getSchemaLocation(),
               is(String.format(DEFAULT_SCHEMA_LOCATION_MASK, NAMESPACE_LOCATION, String.format(XSD_FILENAME_MASK, NAMESPACE))));
  }

  @Test
  public void withDefaultValues() {
    ExtensionDeclaration extensionDeclaration = getExtensionDeclaration(NoXmlSupport.class);
    XmlDslModel dslModel = extensionDeclaration.getXmlDslModel();

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
  public void withCustomNamespaceValue() {
    ExtensionDeclaration extensionDeclaration = getExtensionDeclaration(DefaultXmlExtension.class);
    XmlDslModel dslModel = extensionDeclaration.getXmlDslModel();

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
  public void withCustomSchemaLocationValue() {
    ExtensionDeclaration extensionDeclaration = getExtensionDeclaration(CustomSchemaLocationXmlExtension.class);
    XmlDslModel dslModel = extensionDeclaration.getXmlDslModel();

    assertThat(dslModel, is(notNullValue()));
    assertThat(dslModel.getSchemaVersion(), is(EXTENSION_VERSION));
    assertThat(dslModel.getPrefix(), is(EXTENSION.toLowerCase()));
    assertThat(dslModel.getNamespace(), equalTo(NAMESPACE_LOCATION));
    assertThat(dslModel.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, hyphenize(EXTENSION))));
    assertThat(dslModel.getSchemaLocation(), is(String.format(DEFAULT_SCHEMA_LOCATION_MASK, NAMESPACE_LOCATION,
                                                              String.format(XSD_FILENAME_MASK, hyphenize(EXTENSION)))));
  }

  @Test
  public void withCustomValuesWithSdkAnnotation() {
    ExtensionDeclaration extensionDeclaration = getExtensionDeclaration(XmlSupportUsingSdkApi.class);
    XmlDslModel dslModel = extensionDeclaration.getXmlDslModel();

    assertThat(dslModel, is(notNullValue()));
    assertThat(dslModel.getSchemaVersion(), is(EXTENSION_VERSION));
    assertThat(dslModel.getPrefix(), is(NAMESPACE));
    assertThat(dslModel.getNamespace(), is(NAMESPACE_LOCATION));
    assertThat(dslModel.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, NAMESPACE)));
    assertThat(dslModel.getSchemaLocation(),
               is(String.format(DEFAULT_SCHEMA_LOCATION_MASK, NAMESPACE_LOCATION, String.format(XSD_FILENAME_MASK, NAMESPACE))));
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void withSdkXmlAndLegacyXmlAnnotation() {
    getExtensionDeclaration(InvalidXmlSupport.class);
  }

  private ExtensionDeclaration getExtensionDeclaration(Class<?> extensionClass) {
    return declarerFor(extensionClass, EXTENSION_VERSION).getDeclaration();
  }

  @Extension(name = EXTENSION_NAME)
  @Xml(prefix = NAMESPACE, namespace = NAMESPACE_LOCATION)
  public static class XmlSupport {

  }

  @Extension(name = EXTENSION_NAME)
  @Xml(prefix = NAMESPACE)
  public static class DefaultXmlExtension {

  }
  @Extension(name = EXTENSION)
  @Xml(namespace = NAMESPACE_LOCATION)
  public static class CustomSchemaLocationXmlExtension {

  }

  @Extension(name = EXTENSION_NAME)
  public static class NoXmlSupport {

  }

  @Extension(name = EXTENSION_NAME)
  @org.mule.sdk.api.annotation.dsl.xml.Xml(prefix = NAMESPACE, namespace = NAMESPACE_LOCATION)
  public static class XmlSupportUsingSdkApi {

  }

  @Extension(name = EXTENSION_NAME)
  @Xml(prefix = NAMESPACE, namespace = NAMESPACE_LOCATION)
  @org.mule.sdk.api.annotation.dsl.xml.Xml(prefix = NAMESPACE, namespace = NAMESPACE_LOCATION)
  public static class InvalidXmlSupport {

  }
}
