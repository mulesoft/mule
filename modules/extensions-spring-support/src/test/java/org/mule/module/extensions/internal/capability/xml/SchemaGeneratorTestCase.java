/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.capability.xml;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.ExtensionFactory;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.extensions.introspection.declaration.Construct;
import org.mule.module.extensions.HeisenbergExtension;
import org.mule.module.extensions.internal.capability.xml.schema.SchemaGenerator;
import org.mule.module.extensions.internal.introspection.AnnotationsBasedDescriber;
import org.mule.module.extensions.internal.introspection.DefaultExtensionFactory;
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

        Construct construct = new AnnotationsBasedDescriber(HeisenbergExtension.class).describe().getRootConstruct();
        Extension extension = extensionFactory.createFrom(construct);

        XmlCapability capability = extension.getCapabilities(XmlCapability.class).iterator().next();

        String schema = generator.generate(extension, capability);
        System.out.println(schema);

        XMLUnit.setIgnoreWhitespace(true);
        assertThat(XMLUnit.compareXML(expectedSchema, schema).similar(), is(true));
    }
}
