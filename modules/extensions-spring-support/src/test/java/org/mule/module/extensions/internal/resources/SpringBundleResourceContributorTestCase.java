/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.resources;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.registry.ServiceRegistry;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.introspection.capability.XmlCapability;
import org.mule.extensions.resources.GenerableResource;
import org.mule.extensions.resources.ResourcesGenerator;
import org.mule.module.extensions.internal.capability.xml.ImmutableXmlCapability;
import org.mule.module.extensions.internal.capability.xml.SpringBundleResourceContributor;
import org.mule.module.extensions.internal.config.ExtensionsNamespaceHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashSet;
import java.util.Set;

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
    private static final String UNSCAPED_LOCATION_PREFIX = "http://";
    private static final String ESCAPED_LOCATION_PREFIX = "http\\://";
    private static final String SCHEMA_NAME = "mule-extension-extension.xsd";

    @Mock
    private Extension extension;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ServiceRegistry serviceRegistry;

    private ResourcesGenerator generator;

    private XmlCapability xmlCapability;

    private SpringBundleResourceContributor contributor;

    @Before
    public void before()
    {
        xmlCapability = new ImmutableXmlCapability(EXTENSION_VERSION, "test", UNSCAPED_LOCATION_PREFIX + SCHEMA_LOCATION);
        Set<XmlCapability> capabilities = new HashSet<>();
        capabilities.add(xmlCapability);
        when(extension.getCapabilities(XmlCapability.class)).thenReturn(capabilities);

        generator = new AnnotationProcessorResourceGenerator(mock(ProcessingEnvironment.class), serviceRegistry);

        when(extension.getName()).thenReturn(EXTENSION_NAME);
        when(extension.getVersion()).thenReturn(EXTENSION_VERSION);

        contributor = new SpringBundleResourceContributor();
    }

    @Test
    public void generateSchema()
    {
        contributor.contribute(extension, generator);

        GenerableResource resource = generator.getOrCreateResource(SCHEMA_NAME);
        assertNotNull(resource);
        assertFalse(StringUtils.isBlank(resource.getContentBuilder().toString()));
    }

    @Test
    public void springHandlers()
    {
        contributor.contribute(extension, generator);

        GenerableResource resource = generator.getOrCreateResource("spring.handlers");
        assertNotNull(resource);
        assertEquals(String.format("%s=%s", ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, ExtensionsNamespaceHandler.class.getName()), resource.getContentBuilder().toString());
    }

    @Test
    public void springSchemas()
    {
        contributor.contribute(extension, generator);

        GenerableResource resource = generator.getOrCreateResource("spring.schemas");
        assertNotNull(resource);

        StringBuilder expected = new StringBuilder();
        expected.append(String.format("%s/%s/%s=META-INF/%s\n", ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, EXTENSION_VERSION, SCHEMA_NAME, SCHEMA_NAME));
        expected.append(String.format("%s/current/%s=META-INF/%s\n", ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, SCHEMA_NAME, SCHEMA_NAME));

        assertEquals(expected.toString(), resource.getContentBuilder().toString());
    }
}
