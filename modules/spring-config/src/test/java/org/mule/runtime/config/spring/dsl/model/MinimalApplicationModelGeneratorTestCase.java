/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static java.util.Collections.emptyList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Test;

public class MinimalApplicationModelGeneratorTestCase extends AbstractMuleTestCase {

  private final MuleContext mockMuleContext = mock(MuleContext.class);
  private final ServiceRegistry mockServiceRegistry = mock(ServiceRegistry.class);
  private final XmlApplicationParser xmlApplicationParser = new XmlApplicationParser(mockServiceRegistry);
  private final XmlConfigurationDocumentLoader documentLoader = new XmlConfigurationDocumentLoader();

  @Test
  public void singleElement() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("single-element-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModelByName("errorHandler");
    assertThat(minimalModel.findNamedComponent("errorHandler"), notNullValue());;
  }

  @Test
  public void twoElements() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("two-elements-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModelByName("flow");
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("errorHandler").isPresent(), is(false));
  }

  @Test
  public void dependencyBetweenElements() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("element-dependency-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModelByName("flow");
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("deadLetterQueueFlow").isPresent(), is(true));
  }

  @Test
  public void dependencyBetweenElementsReferencingMp() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("element-dependency-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModelByPath("flow/0");
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("deadLetterQueueFlow").isPresent(), is(true));
  }

  @Test
  public void noDependencyBetweenElementsReferencingMp() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("element-dependency-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModelByPath("deadLetterQueueFlow/0");
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(false));
    assertThat(minimalModel.findNamedComponent("deadLetterQueueFlow").isPresent(), is(true));
  }

  private MinimalApplicationModelGenerator createGeneratorForConfig(String configFileName) throws Exception {
    Optional<ConfigLine> configLine = xmlApplicationParser.parse(documentLoader
        .loadDocument(getClass().getClassLoader().getResourceAsStream("model-generator/" + configFileName)).getDocumentElement());
    ConfigFile configFile = new ConfigFile("test", Arrays.asList(configLine.get()));
    ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry = new ComponentBuildingDefinitionRegistry();
    CoreComponentBuildingDefinitionProvider coreComponentBuildingDefinitionProvider =
        new CoreComponentBuildingDefinitionProvider();
    coreComponentBuildingDefinitionProvider.init(mockMuleContext);
    coreComponentBuildingDefinitionProvider.getComponentBuildingDefinitions()
        .stream().forEach(componentBuildingDefinitionRegistry::register);
    return new MinimalApplicationModelGenerator(new ApplicationModel(new ArtifactConfig.Builder().addConfigFile(configFile)
        .build(), new ArtifactConfiguration(emptyList()), Optional.of(componentBuildingDefinitionRegistry)),
                                                componentBuildingDefinitionRegistry);
  }

}
