/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.SpringSchemaBundleResourceFactory.BUNDLE_MASK;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.SpringSchemaBundleResourceFactory.GENERATED_FILE_NAME;
import static org.mule.runtime.config.spring.dsl.api.xml.SchemaConstants.CURRENT_VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockSubTypes;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.DslResourceFactory;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.ResourcesGenerator;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.config.ExtensionNamespaceHandler;
import org.mule.runtime.module.extension.internal.resources.AbstractGeneratedResourceFactoryTestCase;
import org.mule.runtime.module.extension.internal.resources.AnnotationProcessorResourceGenerator;
import org.mule.tck.size.SmallTest;

import java.util.ServiceLoader;

import javax.annotation.processing.ProcessingEnvironment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class XmlGeneratedResourcesTestCase extends AbstractGeneratedResourceFactoryTestCase {

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

  private XmlDslModel xmlDslModel;

  private SpringHandlerBundleResourceFactory springHandlerFactory = new SpringHandlerBundleResourceFactory();
  private SpringSchemaBundleResourceFactory springSchemaBundleResourceFactory = new SpringSchemaBundleResourceFactory();
  private SchemaXmlResourceFactory schemaXmlResourceFactory = new SchemaXmlResourceFactory();

  @Before
  public void before() {
    xmlDslModel = XmlDslModel.builder()
        .setSchemaVersion(EXTENSION_VERSION)
        .setPrefix(EXTENSION_NAME)
        .setNamespace(UNESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION)
        .setSchemaLocation(String.format("%s/%s/%s", UNESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, CURRENT_VERSION, SCHEMA_NAME))
        .setXsdFileName(SCHEMA_NAME)
        .build();

    when(extensionModel.getXmlDslModel()).thenReturn(xmlDslModel);
    mockSubTypes(extensionModel);
    when(extensionModel.getImportedTypes()).thenReturn(emptySet());

    generator = new AnnotationProcessorResourceGenerator(asList(springHandlerFactory, springSchemaBundleResourceFactory,
                                                                schemaXmlResourceFactory),
                                                         processingEnvironment);

    when(extensionModel.getName()).thenReturn(EXTENSION_NAME);
    when(extensionModel.getVersion()).thenReturn(EXTENSION_VERSION);
  }

  @Override
  protected Class<? extends GeneratedResourceFactory>[] getResourceFactoryTypes() {
    return new Class[] {SpringHandlerBundleResourceFactory.class, SchemaXmlResourceFactory.class,
        SpringSchemaBundleResourceFactory.class};
  }

  @Test
  public void spiDiscovery() throws Exception {
    ServiceLoader<DslResourceFactory> services = ServiceLoader.load(DslResourceFactory.class);
    assertThat(stream(getResourceFactoryTypes()).allMatch(factoryClass -> {
      for (GeneratedResourceFactory factory : services) {
        if (factoryClass.isAssignableFrom(factory.getClass())) {
          return true;
        }
      }
      return false;
    }), is(true));

  }

  @Test
  public void generateSchema() throws Exception {
    GeneratedResource resource = schemaXmlResourceFactory.generateResource(extensionModel).get();
    assertThat(isBlank(new String(resource.getContent())), is(false));
  }

  @Test
  public void springHandlers() throws Exception {
    GeneratedResource resource = springHandlerFactory.generateResource(extensionModel).get();

    assertThat(SpringHandlerBundleResourceFactory.GENERATED_FILE_NAME, equalTo(resource.getPath()));
    assertThat(new String(resource.getContent()),
               equalTo(String.format(SpringHandlerBundleResourceFactory.BUNDLE_MASK, ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION,
                                     ExtensionNamespaceHandler.class.getName())));
  }

  @Test
  public void springSchemas() throws Exception {
    GeneratedResource resource = springSchemaBundleResourceFactory.generateResource(extensionModel).get();
    assertThat(resource.getPath(), equalTo(GENERATED_FILE_NAME));

    StringBuilder expected = new StringBuilder();
    expected.append(String.format(BUNDLE_MASK, ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, EXTENSION_VERSION, SCHEMA_NAME,
                                  SCHEMA_NAME));
    expected.append(String.format(BUNDLE_MASK, ESCAPED_LOCATION_PREFIX + SCHEMA_LOCATION, "current", SCHEMA_NAME, SCHEMA_NAME));


    assertThat(new String(resource.getContent()), equalTo(expected.toString()));
  }
}
