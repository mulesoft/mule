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
import org.mule.runtime.extension.api.annotation.capability.Xml;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import static org.mule.runtime.module.extension.internal.util.NameUtils.hyphenize;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class XmlModelEnricherTestCase extends AbstractMuleTestCase
{

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
    private ModelEnricher modelEnricher = new XmlModelEnricher();

    @Test
    public void enrichWithCustomValues()
    {
        extensionDeclarer.named(EXTENSION_NAME).onVersion(EXTENSION_VERSION);
        XmlModelProperty xmlProperty = enrich(XmlSupport.class);

        assertThat(xmlProperty, is(notNullValue()));
        assertThat(xmlProperty.getSchemaVersion(), is(EXTENSION_VERSION));
        assertThat(xmlProperty.getNamespace(), is(NAMESPACE));
        assertThat(xmlProperty.getNamespaceUri(), is(NAMESPACE_LOCATION));
        assertThat(xmlProperty.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, NAMESPACE)));
        assertThat(xmlProperty.getSchemaLocation(), is(String.format(DEFAULT_SCHEMA_LOCATION_MASK,
                                                                     NAMESPACE_LOCATION,
                                                                     String.format(XSD_FILENAME_MASK, NAMESPACE))));
    }

    @Test
    public void enrichWithDefaultValues()
    {
        extensionDeclarer.named(EXTENSION_NAME).onVersion(EXTENSION_VERSION);
        XmlModelProperty xmlProperty = enrich(NoXmlSupport.class);

        assertThat(xmlProperty, is(notNullValue()));
        assertThat(xmlProperty.getSchemaVersion(), is(EXTENSION_VERSION));
        assertThat(xmlProperty.getNamespace(), is(EXTENSION_HYPHENAZED_NAME));
        assertThat(xmlProperty.getNamespaceUri(), equalTo(String.format(DEFAULT_NAMESPACE_LOCATION_MASK, EXTENSION_HYPHENAZED_NAME)));
        assertThat(xmlProperty.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, EXTENSION_HYPHENAZED_NAME)));
        assertThat(xmlProperty.getSchemaLocation(), is(String.format(DEFAULT_SCHEMA_LOCATION_MASK,
                                                                     String.format(DEFAULT_NAMESPACE_LOCATION_MASK, EXTENSION_HYPHENAZED_NAME),
                                                                     String.format(XSD_FILENAME_MASK, EXTENSION_HYPHENAZED_NAME))));
    }

    @Test
    public void enrichWithCustomNamespaceValue()
    {
        extensionDeclarer.named(EXTENSION_NAME).onVersion(EXTENSION_VERSION);
        XmlModelProperty xmlProperty = enrich(DefaultXmlExtension.class);

        assertThat(xmlProperty, is(notNullValue()));
        assertThat(xmlProperty.getSchemaVersion(), is(EXTENSION_VERSION));
        assertThat(xmlProperty.getNamespace(), is(NAMESPACE));
        assertThat(xmlProperty.getNamespaceUri(), equalTo(String.format(DEFAULT_NAMESPACE_LOCATION_MASK, NAMESPACE)));
        assertThat(xmlProperty.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, NAMESPACE)));
        assertThat(xmlProperty.getSchemaLocation(), is(String.format(DEFAULT_SCHEMA_LOCATION_MASK,
                                                                     String.format(DEFAULT_NAMESPACE_LOCATION_MASK, NAMESPACE),
                                                                     String.format(XSD_FILENAME_MASK, NAMESPACE))));
    }

    @Test
    public void enrichWithCustomSchemaLocationValue()
    {
        extensionDeclarer.named(EXTENSION).onVersion(EXTENSION_VERSION);
        XmlModelProperty xmlProperty = enrich(CustomSchemaLocationXmlExtension.class);

        assertThat(xmlProperty, is(notNullValue()));
        assertThat(xmlProperty.getSchemaVersion(), is(EXTENSION_VERSION));
        assertThat(xmlProperty.getNamespace(), is(EXTENSION.toLowerCase()));
        assertThat(xmlProperty.getNamespaceUri(), equalTo(NAMESPACE_LOCATION));
        assertThat(xmlProperty.getXsdFileName(), is(String.format(XSD_FILENAME_MASK, hyphenize(EXTENSION))));
        assertThat(xmlProperty.getSchemaLocation(), is(String.format(DEFAULT_SCHEMA_LOCATION_MASK,
                                                                     NAMESPACE_LOCATION,
                                                                     String.format(XSD_FILENAME_MASK, hyphenize(EXTENSION)))));
    }

    private XmlModelProperty enrich(Class<?> type)
    {
        extensionDeclarer.withModelProperty(new ImplementingTypeModelProperty(type));
        modelEnricher.enrich(new DefaultDescribingContext(extensionDeclarer));
        return extensionDeclarer.getExtensionDeclaration().getModelProperty(XmlModelProperty.class).get();
    }

    @Xml(namespace = NAMESPACE, namespaceLocation = NAMESPACE_LOCATION)
    private static class XmlSupport
    {

    }

    @Xml(namespace = NAMESPACE)
    private static class DefaultXmlExtension
    {

    }

    @Xml(namespaceLocation = NAMESPACE_LOCATION)
    private static class CustomSchemaLocationXmlExtension
    {

    }

    private static class NoXmlSupport
    {

    }
}
