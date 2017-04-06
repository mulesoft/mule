/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.core.util.IOUtils.getResourceAsString;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.api.app.declaration.ArtifactDeclaration;
import org.mule.runtime.api.app.declaration.FlowElementDeclaration;
import org.mule.runtime.api.app.declaration.ParameterElementDeclaration;
import org.mule.runtime.api.app.declaration.fluent.ElementDeclarer;
import org.mule.runtime.api.app.declaration.fluent.ParameterSimpleValue;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.spring.dsl.api.ArtifactDeclarationXmlSerializer;
import org.mule.runtime.config.spring.dsl.model.DslElementModelFactory;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class ArtifactDeclarationLocationPathTestCase extends AbstractElementModelTestCase {

  public ArtifactDeclaration multiFlowDeclaration;
  private ArtifactDeclarationXmlSerializer serializer;

  @Override
  protected String getConfigFile() {
    return "multi-flow-dsl-app.xml";
  }


  @Before
  public void setup() throws Exception {
    Set<ExtensionModel> extensions = muleContext.getExtensionManager().getExtensions();
    String core = IOUtils
        .toString(Thread.currentThread().getContextClassLoader().getResourceAsStream("META-INF/core-extension-model.json"));
    ExtensionModel coreModel = new ExtensionModelJsonSerializer().deserialize(core);

    dslContext = DslResolvingContext.getDefault(ImmutableSet.<ExtensionModel>builder()
        .addAll(extensions).add(coreModel).build());
    modelResolver = DslElementModelFactory.getDefault(dslContext);
    serializer = ArtifactDeclarationXmlSerializer.getDefault(dslContext);
    multiFlowDeclaration = serializer.deserialize(getConfigFile(),
                                                  Thread.currentThread().getContextClassLoader()
                                                      .getResourceAsStream(getConfigFile()));
  }

  @Test
  public void updatePropertyAndSerialize() throws Exception {
    ElementDeclarer jms = ElementDeclarer.forExtension("JMS");

    final Location destinationLocation = Location.builder()
        .globalName("send-payload")
        .addProcessorsPart()
        .addIndexPart(0)
        .addParameterPart()
        .addPart("destination")
        .build();

    Optional<ParameterElementDeclaration> destination = multiFlowDeclaration.findElement(destinationLocation);
    assertThat(destination.isPresent(), is(true));
    destination.get().setValue(ParameterSimpleValue.of("updatedDestination"));

    final Location flowLocation = Location.builder().globalName("send-payload").build();
    Optional<FlowElementDeclaration> flow = multiFlowDeclaration.findElement(flowLocation);
    assertThat(destination.isPresent(), is(true));
    flow.get().addComponent(0, jms.newSource("listener")
        .withConfig("config")
        .withParameter("destination", "myListenerDestination")
        .getDeclaration());

    String serialized = serializer.serialize(multiFlowDeclaration);
    compareXML(getResourceAsString("location-path-update-multi-flow-dsl-app.xml", getClass()), serialized);
  }
}
