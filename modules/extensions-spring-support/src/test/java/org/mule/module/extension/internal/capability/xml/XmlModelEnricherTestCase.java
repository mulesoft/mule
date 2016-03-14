/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.extension.api.annotation.capability.Xml;
import org.mule.extension.api.introspection.declaration.fluent.DeclarationDescriptor;
import org.mule.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.extension.api.introspection.property.XmlModelProperty;
import org.mule.module.extension.internal.DefaultDescribingContext;
import org.mule.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class XmlModelEnricherTestCase extends AbstractMuleTestCase
{

    private static final String SCHEMA_VERSION = "1.0";
    private static final String NAMESPACE = "namespace";
    private static final String SCHEMA_LOCATION = "SCHEMA_LOCATION";
    private static final String DEFAULT_SCHEMA_LOCATION_MASK = "http://www.mulesoft.org/schema/mule/%s";

    private static final String EXTENSION = "Extension";
    private static final String EXTENSION_NAME = "Xml Model " + EXTENSION;
    private static final String EXTENSION_HYPHENAZED_NAME = "xml-model";
    private static final String EXTENSION_VERSION = "3.7";

    private DeclarationDescriptor declarationDescriptor = new DeclarationDescriptor();
    private ModelEnricher modelEnricher = new XmlModelEnricher();

    @Test
    public void enrichWithCustomValues()
    {
        declarationDescriptor.named(EXTENSION_NAME).onVersion(EXTENSION_VERSION);
        XmlModelProperty xmlProperty = enrich(XmlSupport.class);

        assertThat(xmlProperty, is(notNullValue()));
        assertThat(xmlProperty.getSchemaVersion(), is(SCHEMA_VERSION));
        assertThat(xmlProperty.getNamespace(), is(NAMESPACE));
        assertThat(xmlProperty.getSchemaLocation(), is(SCHEMA_LOCATION));
    }

    @Test
    public void enrichWithDefaultValues()
    {
        declarationDescriptor.named(EXTENSION_NAME).onVersion(EXTENSION_VERSION);
        XmlModelProperty xmlProperty = enrich(NoXmlSupport.class);

        assertThat(xmlProperty, is(notNullValue()));
        assertThat(xmlProperty.getSchemaVersion(), is(EXTENSION_VERSION));
        assertThat(xmlProperty.getNamespace(), is(EXTENSION_HYPHENAZED_NAME));
        assertThat(xmlProperty.getSchemaLocation(), equalTo(String.format(DEFAULT_SCHEMA_LOCATION_MASK, EXTENSION_HYPHENAZED_NAME)));
    }

    @Test
    public void enrichWithCustomNamespaceValue()
    {
        declarationDescriptor.named(EXTENSION_NAME).onVersion(EXTENSION_VERSION);
        XmlModelProperty xmlProperty = enrich(DefaultXmlExtension.class);

        assertThat(xmlProperty, is(notNullValue()));
        assertThat(xmlProperty.getSchemaVersion(), is(EXTENSION_VERSION));
        assertThat(xmlProperty.getNamespace(), is(NAMESPACE));
        assertThat(xmlProperty.getSchemaLocation(), equalTo(String.format(DEFAULT_SCHEMA_LOCATION_MASK, NAMESPACE)));
    }

    @Test
    public void enrichWithCustomSchemaLocationValue()
    {
        declarationDescriptor.named(EXTENSION).onVersion(EXTENSION_VERSION);
        XmlModelProperty xmlProperty = enrich(CustomSchemaLocationXmlExtension.class);

        assertThat(xmlProperty, is(notNullValue()));
        assertThat(xmlProperty.getSchemaVersion(), is(EXTENSION_VERSION));
        assertThat(xmlProperty.getNamespace(), is(EXTENSION.toLowerCase()));
        assertThat(xmlProperty.getSchemaLocation(), equalTo(SCHEMA_LOCATION));
    }

    private XmlModelProperty enrich(Class<?> type)
    {
        declarationDescriptor.withModelProperty(new ImplementingTypeModelProperty(type));
        modelEnricher.enrich(new DefaultDescribingContext(declarationDescriptor));
        return declarationDescriptor.getDeclaration().getModelProperty(XmlModelProperty.class).get();
    }

    @Xml(schemaVersion = SCHEMA_VERSION, namespace = NAMESPACE, schemaLocation = SCHEMA_LOCATION)
    private static class XmlSupport
    {

    }

    @Xml(namespace = NAMESPACE)
    private static class DefaultXmlExtension
    {

    }

    @Xml(schemaLocation = SCHEMA_LOCATION)
    private static class CustomSchemaLocationXmlExtension
    {

    }

    private static class NoXmlSupport
    {

    }
}
