/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.serialization.ArtifactAstDeserializer;
import org.mule.runtime.ast.api.serialization.ArtifactAstSerializerProvider;
import org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Test;

public class ComponentLocationTestCase extends AbstractDslModelTestCase {

  private static final String CONFIG_NAME = "myConfig";

  private Set<ExtensionModel> extensions;

  @Before
  public void setUp() {
    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .build();
  }

  @Test
  public void validateConnectionLocation() throws Exception {
    ArtifactAstDeserializer defaultArtifactAstDeserializer = new ArtifactAstSerializerProvider().getDeserializer();

    ArtifactAst applicationModel = defaultArtifactAstDeserializer
        .deserialize(this.getClass().getResourceAsStream("/asts/ComponentLocationTestCase.ast"), name -> extensions.stream()
            .filter(x -> x.getName().equals(name))
            .findFirst()
            .orElse(null));

    ComponentAst config = applicationModel.topLevelComponentsStream().findFirst().get();
    assertThat(config.getModel(ConfigurationModel.class), is(not(empty())));
    assertThat(config.getLocation().getLocation(), equalTo(CONFIG_NAME));

    ComponentAst connection = config.directChildrenStream().findFirst().get();
    assertThat(connection.getModel(ConnectionProviderModel.class), is(not(empty())));
    assertThat(connection.getLocation().getLocation(), equalTo(CONFIG_NAME + "/connection"));
  }

}
