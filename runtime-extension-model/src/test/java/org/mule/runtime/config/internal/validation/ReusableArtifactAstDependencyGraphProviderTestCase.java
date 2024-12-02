/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.util.stream.Stream.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.internal.validation.ast.ReusableArtifactAstDependencyGraphProvider;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
@Issue("W-12421187")
public class ReusableArtifactAstDependencyGraphProviderTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void verifyReusableProvider() {
    ArtifactAst artifactAst = mock(ArtifactAst.class);
    ReusableArtifactAstDependencyGraphProvider reusableArtifactAstGraphDependencyProvider =
        new ReusableArtifactAstDependencyGraphProvider(artifactAst);
    // The double return is done to avoid the consumption of the stream in the first
    // invocation.
    when(artifactAst.recursiveStream()).thenReturn(empty()).thenReturn(empty());
    assertThat(reusableArtifactAstGraphDependencyProvider.get(artifactAst),
               sameInstance(reusableArtifactAstGraphDependencyProvider.get(artifactAst)));
  }

  @Test
  public void verifyFailureInCaseAnotherArtifactAstIsPassed() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("An incorrect artifactAST was provided for the creation of the graph.");

    ArtifactAst artifactAst = mock(ArtifactAst.class);
    ReusableArtifactAstDependencyGraphProvider reusableArtifactAstGraphDependencyProvider =
        new ReusableArtifactAstDependencyGraphProvider(artifactAst);
    reusableArtifactAstGraphDependencyProvider.get(mock(ArtifactAst.class));
  }
}
