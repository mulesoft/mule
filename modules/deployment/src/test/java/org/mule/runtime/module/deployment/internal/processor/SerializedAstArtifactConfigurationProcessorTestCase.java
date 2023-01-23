/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.processor;

import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.extension.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.SERIALIZED_ARTIFACT_AST_LOCATION;
import static org.mule.tck.util.MuleContextUtils.addExtensionModelToMock;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DOMAIN_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DeploymentFailureStory.DEPLOYMENT_FAILURE;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContextConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(DOMAIN_DEPLOYMENT)
@Story(DEPLOYMENT_FAILURE)
public class SerializedAstArtifactConfigurationProcessorTestCase extends AbstractMuleTestCase {

  private SerializedAstArtifactConfigurationProcessor configurationBuilder;
  private MuleContextWithRegistry muleContext;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void setUp() throws Exception {
    muleContext = mockContextWithServices();
    addExtensionModelToMock(muleContext, getExtensionModel());
    muleContext.getInjector().inject(this);
    configurationBuilder = new SerializedAstArtifactConfigurationProcessor();
  }

  @Test
  @Issue("W-12374722")
  public void serializedAstWithIncorrectArtifactType() throws ConfigurationException {
    when(muleContext.getExecutionClassLoader()).thenReturn(new ClassLoader() {

      @Override
      public InputStream getResourceAsStream(String name) {
        if (SERIALIZED_ARTIFACT_AST_LOCATION.equals(name)) {
          return toInputStream("JSON#1.0#UTF-8#\n" +
              "{\"artifactName\":\"artifact\",\"artifactType\":\"" + APPLICATION.name() + "\"," +
              "\"dependencies\":[{\"name\":\"mule\"}]," +
              "  \"topLevelComponentAsts\": []," +
              "  \"errorTypeRepository\": {\"hierarchy\": []}," +
              "  \"importedResources\": []}",
                                       UTF_8);
        } else {
          return SerializedAstArtifactConfigurationProcessorTestCase.class.getClassLoader().getResourceAsStream(name);
        }
      }
    });

    expectedException.expect(ConfigurationException.class);
    expectedException.expectCause(instanceOf(IllegalStateException.class));
    expectedException
        .expectMessage("Expected artifact type '" + DOMAIN.name() + "' but serialized ast was '" + APPLICATION.name() + "'");

    configurationBuilder.createArtifactContext(ArtifactContextConfiguration.builder()
        .setConfigResources(new String[] {"mule-config.xml"})
        .setArtifactType(DOMAIN)
        .setMuleContext(muleContext)
        .build());
  }


}
