/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newArtifact;
import static org.mule.test.infrastructure.maven.MavenTestUtils.getMavenLocalRepository;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;
import org.mule.runtime.module.tooling.api.artifact.DeclarationSession;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class DeclarationSessionTestCase extends AbstractFakeMuleServerTestCase implements TestExtensionAware {

  private static final String EXTENSION_GROUP_ID = "org.mule.tooling";
  private static final String EXTENSION_ARTIFACT_ID = "tooling-support-test-extension";
  private static final String EXTENSION_VERSION = "1.0.0-SNAPSHOT";
  private static final String EXTENSION_CLASSIFIER = "mule-plugin";
  private static final String EXTENSION_TYPE = "jar";

  private static final String CONFIG_NAME = "dummyConfig";
  private static final String CLIENT_NAME = "client";
  private static final String PROVIDED_PARAMETER_NAME = "providedParameter";

  private DeclarationSession session;

  @ClassRule
  public static SystemProperty artifactsLocation =
      new SystemProperty("mule.test.maven.artifacts.dir", DeclarationSession.class.getResource("/").getPath());

  @Rule
  public SystemProperty repositoryLocation =
      new SystemProperty("muleRuntimeConfig.maven.repositoryLocation", getMavenLocalRepository().getAbsolutePath());

  @Override
  public void setUp() throws Exception {
    ArtifactDeclarer artifactDeclarer = newArtifact();
    artifactDeclarer.withGlobalElement(configurationDeclaration(CONFIG_NAME, connectionDeclaration(CLIENT_NAME)));
    super.setUp();
    this.session = this.muleServer
        .toolingService()
        .newDeclarationSessionBuilder()
        .addDependency(EXTENSION_GROUP_ID,
                       EXTENSION_ARTIFACT_ID,
                       EXTENSION_VERSION,
                       EXTENSION_CLASSIFIER,
                       EXTENSION_TYPE)
        .setArtifactDeclaration(artifactDeclarer.getDeclaration())
        .build();
    this.muleServer.start();
  }

  @Test
  public void testConnection() {
    ConnectionValidationResult connectionValidationResult = session.testConnection(CONFIG_NAME);
    assertThat(connectionValidationResult.isValid(), equalTo(true));
  }

  private String expectedParameter(String actingParameter) {
    return "WITH-ACTING-PARAMETER-" + actingParameter;
  }

  @Test
  public void configLessConnectionLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessConnectionLessOPDeclaration(CONFIG_NAME);
    getResultAndValidate(elementDeclaration, PROVIDED_PARAMETER_NAME, "ConfigLessConnectionLessNoActingParameter");
  }

  @Test
  public void configLessOnOperation() {
    ComponentElementDeclaration elementDeclaration = configLessOPDeclaration(CONFIG_NAME);
    getResultAndValidate(elementDeclaration, PROVIDED_PARAMETER_NAME, CLIENT_NAME);
  }

  @Test
  public void actingParameterOnOperation() {
    final String actingParameter = "actingParameter";
    ComponentElementDeclaration elementDeclaration = actingParameterOPDeclaration(CONFIG_NAME, actingParameter);
    getResultAndValidate(elementDeclaration, PROVIDED_PARAMETER_NAME, expectedParameter(actingParameter));
  }

  @Test
  public void actingParameterGroup() {
    final String stringValue = "stringValue";
    final int intValue = 0;
    final List<String> listValue = asList("one", "two", "three");
    ComponentElementDeclaration elementDeclaration =
        actingParameterGroupOPDeclaration(CONFIG_NAME, stringValue, intValue, listValue);
    getResultAndValidate(elementDeclaration, PROVIDED_PARAMETER_NAME, "stringValue-0-one-two-three");
  }

  @Test
  public void complexActingParameter() {
    final String stringValue = "stringValue";
    ComponentElementDeclaration elementDeclaration =
        complexActingParameterOPDeclaration(CONFIG_NAME, stringValue);
    getResultAndValidate(elementDeclaration, PROVIDED_PARAMETER_NAME, stringValue);
  }

  private void getResultAndValidate(ComponentElementDeclaration elementDeclaration, String parameterName, String expectedValue) {
    ValueResult providerResult = session.getValues(elementDeclaration, parameterName);

    assertThat(providerResult.isSuccess(), equalTo(true));
    assertThat(providerResult.getValues(), hasSize(1));
    assertThat(providerResult.getValues().iterator().next().getId(), is(expectedValue));
  }

}
