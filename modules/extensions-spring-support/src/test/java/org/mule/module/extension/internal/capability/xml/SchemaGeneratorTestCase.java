/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.capability.xml;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.ExtensionFactory;
import org.mule.extension.introspection.capability.XmlCapability;
import org.mule.extension.introspection.declaration.fluent.Descriptor;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.capability.xml.schema.SchemaGenerator;
import org.mule.module.extension.internal.introspection.AnnotationsBasedDescriber;
import org.mule.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.IOUtils;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class SchemaGeneratorTestCase extends AbstractMuleTestCase
{

    private SchemaGenerator generator;
    private ExtensionFactory extensionFactory;

    @Before
    public void before()
    {
        generator = new SchemaGenerator();
        extensionFactory = new DefaultExtensionFactory(mock(ServiceRegistry.class));
    }


    @Test
    public void generate() throws Exception
    {
        String expectedSchema = IOUtils.getResourceAsString("heisenberg.xsd", getClass());

        Descriptor descriptor = new AnnotationsBasedDescriber(HeisenbergExtension.class).describe().getRootDeclaration();
        Extension extension = extensionFactory.createFrom(descriptor);

        XmlCapability capability = extension.getCapabilities(XmlCapability.class).iterator().next();

        String schema = generator.generate(extension, capability);
        System.out.println(schema);

        XMLUnit.setIgnoreWhitespace(true);
        assertThat(XMLUnit.compareXML(expectedSchema, schema).similar(), is(true));
    }
}
