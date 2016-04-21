/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static java.util.Arrays.asList;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.capability.xml.schema.SchemaGenerator;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
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
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class SchemaGeneratorTestCase extends AbstractMuleTestCase
{

    private SchemaGenerator generator;
    private ExtensionFactory extensionFactory;

    @Parameterized.Parameter(0)
    public Class<?> extensionUnderTest;

    @Parameterized.Parameter(1)
    public String expectedXSD;

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
                {SubTypesMappingConnector.class, "subtypes.xsd"}
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

        ExtensionDeclarer declarer = new AnnotationsBasedDescriber(extensionUnderTest, new StaticVersionResolver(getProductVersion())).describe(new DefaultDescribingContext());
        ExtensionModel extensionModel = extensionFactory.createFrom(declarer);

        XmlModelProperty capability = extensionModel.getModelProperty(XmlModelProperty.class).get();

        String schema = generator.generate(extensionModel, capability);

        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreAttributeOrder(true);

        Diff diff = XMLUnit.compareXML(expectedSchema, schema);
        if (!(diff.similar() && diff.identical()))
        {
            System.out.println(schema);
            DetailedDiff detDiff = new DetailedDiff(diff);
            @SuppressWarnings("rawtypes")
            List differences = detDiff.getAllDifferences();
            StringBuilder diffLines = new StringBuilder();
            for (Object object : differences)
            {
                Difference difference = (Difference) object;
                diffLines.append(difference.toString() + '\n');
            }

            assertEquals(String.format("The Output for extension [%s] schema was not the expected:", extensionUnderTest.getName()), expectedSchema, schema);
        }
    }
}
