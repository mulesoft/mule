/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.config.spring.XmlConfigurationDocumentLoader.schemaValidatingDocumentLoader;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.spring.XmlConfigurationDocumentLoader;
import org.mule.runtime.config.spring.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.spring.dsl.processor.ConfigFile;
import org.mule.runtime.config.spring.dsl.processor.ConfigLine;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlApplicationParser;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MinimalApplicationModelGeneratorTestCase extends AbstractMuleTestCase {

  @Rule
  public final ExpectedException expectedException = none();
  private final ServiceRegistry mockServiceRegistry = mock(ServiceRegistry.class);
  private final XmlApplicationParser xmlApplicationParser =
      new XmlApplicationParser(mockServiceRegistry,
                               singletonList(MinimalApplicationModelGeneratorTestCase.class.getClassLoader()));
  private final XmlConfigurationDocumentLoader documentLoader = schemaValidatingDocumentLoader();

  @Test
  public void noElements() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("no-elements-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(builder().globalName("flow").build());
    assertThat(minimalModel.findTopLevelNamedComponent("flow").isPresent(), is(true));
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
    assertThat(minimalModel.findTopLevelNamedComponent("errorHandler").isPresent(), is(true));
  }

  @Test
  public void twoElements() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("two-elements-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(builder().globalName("flow").build());
    assertThat(minimalModel.findTopLevelNamedComponent("flow").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("errorHandler").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("errorHandler").get().isEnabled(), is(false));
  }

  @Test
  public void dependencyBetweenElements() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("element-dependency-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(builder().globalName("flowOne").build());
    assertThat(minimalModel.findTopLevelNamedComponent("flowOne").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowTwo").isPresent(), is(true));
  }

  @Test
  public void resolveDependencies() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("resolve-dependencies-config.xml");
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("flowTwo").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findTopLevelNamedComponent("flowTwo").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowTwo").get().isEnabled(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowOne").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowOne").get().isEnabled(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").get().isEnabled(), is(false));

    List<ComponentModel> componentModelList = generator.resolveComponentModelDependencies();
    assertThat(componentModelList, hasSize(3));

    assertThat(componentModelList.get(0).getIdentifier().getName(), equalTo("flow-ref"));
    assertThat(componentModelList.get(1).getNameAttribute(), equalTo("flowTwo"));
    assertThat(componentModelList.get(2).getNameAttribute(), equalTo("flowOne"));
  }

  @Test
  public void resolveDependenciesDoesNotFailIfAReferenceIsInvalid() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("bad-reference-config.xml");
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("myFlow").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findTopLevelNamedComponent("myFlow").isPresent(), is(true));;
  }

  @Test
  public void resolveDependenciesMultipleConfigFiles() throws Exception {
    final String firstConfigFile = "resolve-dependencies-config-1.xml";
    final String secondConfigFile = "resolve-dependencies-config-2.xml";
    MinimalApplicationModelGenerator generator =
        createGeneratorForConfig(firstConfigFile, secondConfigFile);
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("flowTwo").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findTopLevelNamedComponent("flowTwo").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowTwo").get().isEnabled(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowOne").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowOne").get().isEnabled(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").get().isEnabled(), is(false));

    List<ComponentModel> componentModelList = generator.resolveComponentModelDependencies();
    assertThat(componentModelList, hasSize(3));

    assertThat(componentModelList.get(0).getNameAttribute(), equalTo("flowOne"));
    assertThat(componentModelList.get(1).getIdentifier().getName(), equalTo("flow-ref"));
    assertThat(componentModelList.get(2).getNameAttribute(), equalTo("flowTwo"));

    assertThat(minimalModel.findTopLevelNamedComponent("flowTwo").get().getConfigFileName().get(),
               is(firstConfigFile));
    assertThat(minimalModel.findTopLevelNamedComponent("flowOne").get().getConfigFileName().get(),
               is(secondConfigFile));
  }

  @Test
  public void resolveCyclicDependencies() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("resolve-dependencies-cyclic-dependency-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(Location.builder().globalName("flowA").build());
    assertThat(minimalModel.findTopLevelNamedComponent("flowA").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowB").isPresent(), is(true));

    List<ComponentModel> componentModelList = generator.resolveComponentModelDependencies();
    assertThat(componentModelList, hasSize(4));
  }

  @Test
  public void dependencyBetweenElementsReferencingMp() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("element-dependency-config.xml");
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("flowOne").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findTopLevelNamedComponent("flowOne").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowTwo").isPresent(), is(true));
  }

  @Test
  public void noDependencyBetweenElementsReferencingMp() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("element-dependency-config.xml");
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("flowTwo").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findTopLevelNamedComponent("flowOne").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowTwo").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowTwo").get().isEnabled(), is(true));
  }

  @Test
  public void flowWithSourcePathToMp() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("flow-source-config.xml");
    ApplicationModel minimalModel =
        generator.getMinimalModel(builder().globalName("flowWithSource").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").get().getInnerComponents().size(), is(2));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").get().getInnerComponents().get(1).getIdentifier(),
               is(buildFromStringRepresentation("mule:set-payload")));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").get().getInnerComponents().get(0).isEnabled(),
               is(false));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").get().getInnerComponents().get(1).isEnabled(), is(true));
  }

  @Test
  public void flowWithSourcePathToSource() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("flow-source-config.xml");
    ApplicationModel minimalModel = generator.getMinimalModel(builder().globalName("flowWithSource").addSourcePart().build());
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").get().getInnerComponents().size(), is(2));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").get().getInnerComponents().get(0).getIdentifier(),
               is(buildFromStringRepresentation("mule:scheduler")));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithSource").get().getInnerComponents().get(0).isEnabled(), is(true));
  }

  @Test
  public void nameAttributeOnNonTopLevelElementIsAllowed() throws Exception {
    MinimalApplicationModelGenerator generator = createGeneratorForConfig("low-level-name-attribute-config.xml");
    ApplicationModel minimalModel =
        generator
            .getMinimalModel(builder().globalName("flowWithLowLevelNameAttribute").addProcessorsPart().addIndexPart(0).build());
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithLowLevelNameAttribute").isPresent(), is(true));
    assertThat(minimalModel.findTopLevelNamedComponent("flowWithLowLevelNameAttribute").get().getInnerComponents().get(0)
        .getNameAttribute().equals("asyncName"), is(true));
  }

  private MinimalApplicationModelGenerator createGeneratorForConfig(String... configFileName) throws Exception {
    List<ConfigFile> configFiles = new ArrayList<>();
    for (String configFile : configFileName) {
      Optional<ConfigLine> configLine = xmlApplicationParser.parse(documentLoader
          .loadDocument(configFile, getClass().getClassLoader().getResourceAsStream("model-generator/" + configFile))
          .getDocumentElement());
      configFiles.add(new ConfigFile(configFile, Arrays.asList(configLine.get())));
    }
    ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry = new ComponentBuildingDefinitionRegistry();
    CoreComponentBuildingDefinitionProvider coreComponentBuildingDefinitionProvider =
        new CoreComponentBuildingDefinitionProvider();
    coreComponentBuildingDefinitionProvider.init();
    coreComponentBuildingDefinitionProvider.getComponentBuildingDefinitions()
        .stream().forEach(componentBuildingDefinitionRegistry::register);

    final ArtifactConfig.Builder builder = new ArtifactConfig.Builder();
    configFiles.stream().forEach(configFile -> builder.addConfigFile(configFile));
    final ArtifactConfig artifactConfig = builder.build();
    return new MinimalApplicationModelGenerator(new ApplicationModel(artifactConfig, new ArtifactDeclaration(), emptySet(),
                                                                     Optional.of(componentBuildingDefinitionRegistry)),
                                                componentBuildingDefinitionRegistry);
  }

}
