/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.extension.api.introspection.property.XmlModelProperty;
import org.mule.extension.api.resources.GeneratedResource;
import org.mule.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.module.extension.internal.capability.xml.SpringBundleResourceContributor;
import org.mule.runtime.module.extension.internal.config.ExtensionNamespaceHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class SpringBundleResourceContributorTestCase extends AbstractMuleTestCase
{

    private static final String EXTENSION_NAME = "extension";
    private static final String EXTENSION_VERSION = "version";
    private static final String SCHEMA_LOCATION = "mulesoft.com/extension";
    private static final String UNESCAPED_LOCATION_PREFIX = "http://";
    private static final String ESCAPED_LOCATION_PREFIX = "http\\://";
    private static final String SCHEMA_NAME = "mule-extension.xsd";

    @Mock
    private ExtensionModel extensionModel;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ServiceRegistry serviceRegistry;

    private ResourcesGenerator generator;

    private XmlModelProperty xmlModelProperty;

    private SpringBundleResourceContributor contributor;

    @Before
    public void before()
    {
        xmlModelProperty = new XmlModelProperty(EXTENSION_VERSION, EXTENSION_NAME, UNESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION);
        when(extensionModel.getModelProperty(XmlModelProperty.class)).thenReturn(Optional.of(xmlModelProperty));
        when(extensionModel.getModelProperty(SubTypesModelProperty.class)).thenReturn(Optional.empty());

        generator = new AnnotationProcessorResourceGenerator(mock(ProcessingEnvironment.class), serviceRegistry);

        when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
        when(extensionModel.getVersion()).thenReturn(EXTENSION_VERSION);

        contributor = new SpringBundleResourceContributor();
    }

    @Test
    public void generateSchema()
    {
        contributor.contribute(extensionModel, generator);

        GeneratedResource resource = generator.get(SCHEMA_NAME);
        assertNotNull(resource);
        assertFalse(StringUtils.isBlank(resource.getContentBuilder().toString()));
    }

    @Test
    public void springHandlers()
    {
        contributor.contribute(extensionModel, generator);

        GeneratedResource resource = generator.get("spring.handlers");
        assertNotNull(resource);
        assertEquals(String.format("%s=%s", ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, ExtensionNamespaceHandler.class.getName()), resource.getContentBuilder().toString());
    }

    @Test
    public void springSchemas()
    {
        contributor.contribute(extensionModel, generator);

        GeneratedResource resource = generator.get("spring.schemas");
        assertNotNull(resource);

        StringBuilder expected = new StringBuilder();
        expected.append(String.format("%s/%s/%s=META-INF/%s\n", ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, EXTENSION_VERSION, SCHEMA_NAME, SCHEMA_NAME));
        expected.append(String.format("%s/current/%s=META-INF/%s\n", ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, SCHEMA_NAME, SCHEMA_NAME));

        assertEquals(expected.toString(), resource.getContentBuilder().toString());
    }
}
