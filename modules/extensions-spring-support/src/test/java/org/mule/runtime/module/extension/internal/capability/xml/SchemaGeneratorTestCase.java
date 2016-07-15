/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.xml.dsl.api.property.XmlModelProperty;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.capability.xml.schema.SchemaGenerator;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.enricher.XmlModelEnricher;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.runtime.module.extension.internal.runtime.connector.basic.GlobalInnerPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connector.basic.GlobalPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connector.basic.ListConnector;
import org.mule.runtime.module.extension.internal.runtime.connector.basic.MapConnector;
import org.mule.runtime.module.extension.internal.runtime.connector.basic.StringListConnector;
import org.mule.runtime.module.extension.internal.runtime.connector.basic.TestConnector;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class SchemaGeneratorTestCase extends AbstractMuleTestCase
{

    @Parameterized.Parameter(0)
    public Class<?> extensionUnderTest;
    @Parameterized.Parameter(1)
    public String expectedXSD;
    private SchemaGenerator generator;
    private ExtensionFactory extensionFactory;

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {
                {HeisenbergExtension.class, "heisenberg.xsd"},
                {TestConnector.class, "basic.xsd"},
                {GlobalPojoConnector.class, "global-pojo.xsd"},
                {GlobalInnerPojoConnector.class, "global-inner-pojo.xsd"},
                {MapConnector.class, "map.xsd"},
                {ListConnector.class, "list.xsd"},
                {StringListConnector.class, "string-list.xsd"},
                {VeganExtension.class, "vegan.xsd"},
                {SubTypesMappingConnector.class, "subtypes.xsd"},
                {PetStoreConnector.class, "petstore.xsd"},
                {MetadataExtension.class, "metadata.xsd"}
        });
    }

    @Before
    public void before()
    {
        ClassLoader classLoader = getClass().getClassLoader();
        ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
        when(serviceRegistry.lookupProviders(ModelEnricher.class, classLoader)).thenReturn(asList(new XmlModelEnricher()));

        extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());
        generator = new SchemaGenerator();
    }

    @Test
    public void generate() throws Exception
    {
        String expectedSchema = IOUtils.getResourceAsString(expectedXSD, getClass());

        ExtensionDeclarer declarer = new AnnotationsBasedDescriber(extensionUnderTest, new StaticVersionResolver(getProductVersion())).describe(new DefaultDescribingContext(extensionUnderTest.getClassLoader()));
        ExtensionModel extensionModel = extensionFactory.createFrom(declarer, new DefaultDescribingContext(declarer, getClass().getClassLoader()));
        XmlModelProperty capability = extensionModel.getModelProperty(XmlModelProperty.class).get();

        String schema = generator.generate(extensionModel, capability);
        compareXML(expectedSchema, schema);
    }
}
