/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.validation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import static java.util.stream.Stream.empty;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.validation.ast.CachingArtifactAstGraphDependencyProvider;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Assert;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class CachingArtifactAstGraphDependencyProviderTestCase {

  @Test
  public void verifyCachingProvider() {
    CachingArtifactAstGraphDependencyProvider cachingArtifactAstGraphDependencyProvider =
        new CachingArtifactAstGraphDependencyProvider();
    ArtifactAst artifactAst = mock(ArtifactAst.class);
    when(artifactAst.recursiveStream()).thenReturn(empty());
    Assert.assertThat(cachingArtifactAstGraphDependencyProvider.get(artifactAst),
                      sameInstance(cachingArtifactAstGraphDependencyProvider.get(artifactAst)));
  }
}
