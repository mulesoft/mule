/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.SpringSchemaBundleResourceFactory.BUNDLE_MASK;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.SpringSchemaBundleResourceFactory.GENERATED_FILE_NAME;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.CURRENT_VERSION;
import org.mule.extension.api.introspection.property.ImportedTypesModelProperty;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.extension.api.introspection.property.XmlModelProperty;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.module.extension.internal.config.ExtensionNamespaceHandler;
import org.mule.runtime.module.extension.internal.resources.AnnotationProcessorResourceGenerator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class XmlGeneratedResourcesTestCase extends AbstractMuleTestCase
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

    @Mock
    private ProcessingEnvironment processingEnvironment;

    private ResourcesGenerator generator;

    private XmlModelProperty xmlModelProperty;

    private SpringHandlerBundleResourceFactory springHandlerFactory = new SpringHandlerBundleResourceFactory();
    private SpringSchemaBundleResourceFactory springSchemaBundleResourceFactory = new SpringSchemaBundleResourceFactory();
    private SchemaResourceFactory schemaResourceFactory = new SchemaResourceFactory();

    @Before
    public void before()
    {
        xmlModelProperty = new XmlModelProperty(EXTENSION_VERSION, EXTENSION_NAME,
                                                UNESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION,
                                                SCHEMA_NAME,
                                                String.format("%s/%s/%s", UNESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION,
                                                              CURRENT_VERSION, SCHEMA_NAME));

        when(extensionModel.getModelProperty(XmlModelProperty.class)).thenReturn(Optional.of(xmlModelProperty));
        when(extensionModel.getModelProperty(SubTypesModelProperty.class)).thenReturn(Optional.empty());
        when(extensionModel.getModelProperty(ImportedTypesModelProperty.class)).thenReturn(Optional.empty());

        generator = new AnnotationProcessorResourceGenerator(asList(springHandlerFactory, springSchemaBundleResourceFactory, schemaResourceFactory), processingEnvironment);

        when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
        when(extensionModel.getVersion()).thenReturn(EXTENSION_VERSION);
    }

    @Test
    public void generateSchema() throws Exception
    {
        GeneratedResource resource = schemaResourceFactory.generateResource(extensionModel).get();
        assertThat(isBlank(new String(resource.getContent())), is(false));
    }

    @Test
    public void springHandlers() throws Exception
    {
        GeneratedResource resource = springHandlerFactory.generateResource(extensionModel).get();

        assertThat(SpringHandlerBundleResourceFactory.GENERATED_FILE_NAME, equalTo(resource.getPath()));
        assertThat(new String(resource.getContent()),
                   equalTo(String.format(SpringHandlerBundleResourceFactory.BUNDLE_MASK, ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, ExtensionNamespaceHandler.class.getName())));
    }

    @Test
    public void springSchemas() throws Exception
    {
        GeneratedResource resource = springSchemaBundleResourceFactory.generateResource(extensionModel).get();
        assertThat(resource.getPath(), equalTo(GENERATED_FILE_NAME));

        StringBuilder expected = new StringBuilder();
        expected.append(String.format(BUNDLE_MASK, ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, EXTENSION_VERSION, SCHEMA_NAME, SCHEMA_NAME));
        expected.append(String.format(BUNDLE_MASK, ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, "current", SCHEMA_NAME, SCHEMA_NAME));


        assertThat(new String(resource.getContent()), equalTo(expected.toString()));
    }
}
