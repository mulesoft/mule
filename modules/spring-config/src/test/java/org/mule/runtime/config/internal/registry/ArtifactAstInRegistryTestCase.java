/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.registry;

import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.config.internal.error.MuleCoreErrorTypeRepository.MULE_CORE_ERROR_TYPE_REPOSITORY;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_ADD_ARTIFACT_AST_TO_REGISTRY_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_ARTIFACT_AST;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.RegistryFeature.REGISTRY;
import static org.mule.test.allure.AllureConstants.RegistryFeature.ObjectRegistrationStory.OBJECT_REGISTRATION;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import static org.slf4j.LoggerFactory.getLogger;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.MockExtensionManagerConfigurationBuilder;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;

@Features({@Feature(ARTIFACT_AST), @Feature(REGISTRY)})
@Story(OBJECT_REGISTRATION)
@RunWith(Parameterized.class)
public class ArtifactAstInRegistryTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(ArtifactAstInRegistryTestCase.class);

  @Parameters(name = "addArtifactAstToRegistry: {0}; lazyInit: {1}")
  public static List<Boolean[]> params() {
    return asList(new Boolean[][] {
        {false, false},
        {true, false},
        {false, true},
        {true, true}
    });
  }

  @Parameter(0)
  public Boolean addArtifactAstToRegistry;

  @Parameter(1)
  public Boolean lazyInit;

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private ArtifactAst artifactAst;

  private MuleContextWithRegistry muleContext;

  @Before
  public void before() throws Exception {
    artifactAst = mock(ArtifactAst.class);
    when(artifactAst.getArtifactName()).thenReturn("my mock ast");
    when(artifactAst.getArtifactType()).thenReturn(APPLICATION);
    when(artifactAst.getErrorTypeRepository()).thenReturn(MULE_CORE_ERROR_TYPE_REPOSITORY);
    when(artifactAst.enrichedErrorTypeRepository()).thenReturn(MULE_CORE_ERROR_TYPE_REPOSITORY);
    ArtifactAstConfigurationBuilder artifactAstConfigurationBuilder = new ArtifactAstConfigurationBuilder(artifactAst,
                                                                                                          singletonMap(MULE_ADD_ARTIFACT_AST_TO_REGISTRY_DEPLOYMENT_PROPERTY,
                                                                                                                       addArtifactAstToRegistry
                                                                                                                           .toString()),
                                                                                                          APP,
                                                                                                          lazyInit,
                                                                                                          mock(ComponentBuildingDefinitionRegistry.class));

    muleContext = (MuleContextWithRegistry) new DefaultMuleContextFactory()
        .createMuleContext(testServicesConfigurationBuilder,
                           new MockExtensionManagerConfigurationBuilder(),
                           artifactAstConfigurationBuilder);
    muleContext.start();
  }

  @After
  public void after() {
    if (muleContext != null) {
      disposeIfNeeded(muleContext, LOGGER);
    }
  }

  @Test
  public void artifactAstAvailableInRegistryIfEnabled() {
    ArtifactAst actualArtifactAst = muleContext.getRegistry().lookupObject(OBJECT_ARTIFACT_AST);
    if (addArtifactAstToRegistry) {
      assertThat(actualArtifactAst.getArtifactName(), is("my mock ast"));
    } else {
      assertThat(actualArtifactAst, is(nullValue()));
    }
  }
}
