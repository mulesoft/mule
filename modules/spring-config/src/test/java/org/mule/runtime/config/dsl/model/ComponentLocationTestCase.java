/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.config.api.dsl.processor.ArtifactConfig;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

public class ComponentLocationTestCase extends AbstractDslModelTestCase {

  private static final String CONFIG_NAME = "myConfig";

  private ElementDeclarer declarer;
  private Set<ExtensionModel> extensions;

  @Before
  public void setUp() {
    declarer = ElementDeclarer.forExtension(EXTENSION_NAME);
    extensions = ImmutableSet.<ExtensionModel>builder()
        .add(MuleExtensionModelProvider.getExtensionModel())
        .add(mockExtension)
        .build();
  }

  @Test
  public void validateConnectionLocation() throws Exception {
    ApplicationModel applicationModel = loadApplicationModel(buildAppDeclaration());
    ComponentModel root = applicationModel.getRootComponentModel();
    assertThat(root.getComponentLocation(), is(nullValue()));

    ComponentModel config = root.getInnerComponents().get(0);
    assertThat(config.getModel(ConfigurationModel.class), is(not(empty())));
    assertThat(config.getComponentLocation().getLocation(), equalTo(CONFIG_NAME));

    ComponentModel connection = config.getInnerComponents().get(0);
    assertThat(connection.getModel(ConnectionProviderModel.class), is(not(empty())));
    assertThat(connection.getComponentLocation().getLocation(), equalTo(CONFIG_NAME + "/connection"));
  }



  protected ApplicationModel loadApplicationModel(ArtifactDeclaration declaration) throws Exception {
    return new ApplicationModel(new ArtifactConfig.Builder().build(),
                                declaration, extensions, emptyMap(), empty(), empty(),
                                uri -> getClass().getResourceAsStream(uri), getFeatureFlaggingService());
  }

  private ArtifactDeclaration buildAppDeclaration() {
    return ElementDeclarer.newArtifact()
        .withGlobalElement(declarer.newConfiguration(CONFIGURATION_NAME)
            .withRefName(CONFIG_NAME)
            .withParameterGroup(newParameterGroup()
                .withParameter(CONTENT_NAME, CONTENT_VALUE)
                .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
                .withParameter(LIST_NAME, newListValue().withValue(ITEM_VALUE).build())
                .getDeclaration())
            .withConnection(declarer.newConnection(CONNECTION_PROVIDER_NAME)
                .withParameterGroup(newParameterGroup()
                    .withParameter(CONTENT_NAME, CONTENT_VALUE)
                    .withParameter(BEHAVIOUR_NAME, BEHAVIOUR_VALUE)
                    .withParameter(LIST_NAME,
                                   newListValue().withValue(ITEM_VALUE).build())
                    .getDeclaration())
                .getDeclaration())
            .getDeclaration())
        .getDeclaration();
  }

}
