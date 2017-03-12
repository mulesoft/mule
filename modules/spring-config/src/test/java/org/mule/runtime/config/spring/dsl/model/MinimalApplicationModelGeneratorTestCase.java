/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.location.Location.builder;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MinimalApplicationModelGeneratorTestCase extends AbstractMuleTestCase {

  @Rule
  public final ExpectedException expectedException = none();
  private final MuleContext mockMuleContext = mock(MuleContext.class);
  private final ServiceRegistry mockServiceRegistry = mock(ServiceRegistry.class);
  private final XmlApplicationParser xmlApplicationParser = new XmlApplicationParser(mockServiceRegistry);
  private final XmlConfigurationDocumentLoader documentLoader = new XmlConfigurationDocumentLoader();

  @Test
  public void noElements() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("no-elements-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(builder().globalName("flow").build());
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(true));
  }

  @Test
  public void wrongElementIndexOnMinimalModelByPath() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("no-elements-config.xml");
    Location nonExistentComponentLocation = builder().globalName("flow").addProcessorsPart().addIndexPart(3).build();
    expectedException.expect(NoSuchComponentModelException.class);
    expectedException.expectMessage("No object found at location " + nonExistentComponentLocation.toString());
    generator.getMinimalModel(nonExistentComponentLocation);
  }

  @Test
  public void singleElement() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("single-element-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(builder().globalName("errorHandler").build());
    assertThat(minimalModel.findNamedComponent("errorHandler").isPresent(), is(true));
  }

  @Test
  public void twoElements() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("two-elements-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(builder().globalName("flow").build());
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("errorHandler").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("errorHandler").get().isEnabled(), is(false));
  }

  @Test
  public void dependencyBetweenElements() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("element-dependency-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(builder().globalName("flow").build());
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("deadLetterQueueFlow").isPresent(), is(true));
  }

  @Test
  public void resolveDependencies() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("resolve-dependencies-config.xml");
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("flow").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("deadLetterQueueFlow").isPresent(), is(true));

    List<ComponentModel> componentModelList = generator.resolveComponentModelDependencies();
    assertThat(componentModelList, hasSize(2));

    assertThat(componentModelList.get(0).getNameAttribute(), equalTo("flow"));
    assertThat(componentModelList.get(1).getNameAttribute(), equalTo("deadLetterQueueFlow"));
  }

  @Test
  public void resolveCyclicDependencies() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("resolve-dependencies-cyclic-dependency-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(Location.builder().globalName("flowA").build());
    assertThat(minimalModel.findNamedComponent("flowA").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("flowB").isPresent(), is(true));

    List<ComponentModel> componentModelList = generator.resolveComponentModelDependencies();
    assertThat(componentModelList, hasSize(2));

    assertThat(componentModelList, containsInAnyOrder(hasProperty("nameAttribute", equalTo("flowB")),
                                                      hasProperty("nameAttribute", equalTo("flowA"))));
  }

  @Test
  public void dependencyBetweenElementsReferencingMp() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("element-dependency-config.xml");
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("flow").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("deadLetterQueueFlow").isPresent(), is(true));
  }

  @Test
  public void noDependencyBetweenElementsReferencingMp() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("element-dependency-config.xml");
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("deadLetterQueueFlow").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findNamedComponent("flow").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("deadLetterQueueFlow").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("deadLetterQueueFlow").get().isEnabled(), is(true));
  }

  @Test
  public void flowWithSourcePathToMp() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("flow-source-config.xml");
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("flowWithSource").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findNamedComponent("flowWithSource").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("flowWithSource").get().getInnerComponents().size(), is(2));
    assertThat(minimalModel.findNamedComponent("flowWithSource").get().getInnerComponents().get(1).getIdentifier(),
               is(buildFromStringRepresentation("mule:set-payload")));
    assertThat(minimalModel.findNamedComponent("flowWithSource").get().getInnerComponents().get(0).isEnabled(), is(false));
    assertThat(minimalModel.findNamedComponent("flowWithSource").get().getInnerComponents().get(1).isEnabled(), is(true));
  }

  @Test
  public void flowWithSourcePathToSource() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("flow-source-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(builder().globalName("flowWithSource").addSourcePart().build());
    assertThat(minimalModel.findNamedComponent("flowWithSource").isPresent(), is(true));
    assertThat(minimalModel.findNamedComponent("flowWithSource").get().getInnerComponents().size(), is(2));
    assertThat(minimalModel.findNamedComponent("flowWithSource").get().getInnerComponents().get(0).getIdentifier(),
               is(buildFromStringRepresentation("mule:scheduler")));
    assertThat(minimalModel.findNamedComponent("flowWithSource").get().getInnerComponents().get(0).isEnabled(), is(true));
  }

  private MinimalApplicationModelGenerator createGeneratorForConfig(String configFileName) throws Exception {
    Optional<ConfigLine> configLine = xmlApplicationParser.parse(documentLoader
        .loadDocument(configFileName, getClass().getClassLoader().getResourceAsStream("model-generator/" + configFileName))
        .getDocumentElement());
    ConfigFile configFile = new ConfigFile("test", Arrays.asList(configLine.get()));
    ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry = new ComponentBuildingDefinitionRegistry();
    CoreComponentBuildingDefinitionProvider coreComponentBuildingDefinitionProvider =
        new CoreComponentBuildingDefinitionProvider();
    coreComponentBuildingDefinitionProvider.setMuleContext(mockMuleContext);
    coreComponentBuildingDefinitionProvider.init();
    coreComponentBuildingDefinitionProvider.getComponentBuildingDefinitions()
        .stream().forEach(componentBuildingDefinitionRegistry::register);
    return new MinimalApplicationModelGenerator(new ApplicationModel(new ArtifactConfig.Builder().addConfigFile(configFile)
        .build(), new ArtifactDeclaration(), Optional.empty(), Optional.of(componentBuildingDefinitionRegistry)),
                                                componentBuildingDefinitionRegistry);
  }

}
