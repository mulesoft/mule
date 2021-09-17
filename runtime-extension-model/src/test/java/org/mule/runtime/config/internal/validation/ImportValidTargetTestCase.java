/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ImportedResource;
import org.mule.runtime.ast.api.validation.ArtifactValidation;
import org.mule.runtime.ast.api.validation.ValidationResultItem;

import java.util.Optional;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class ImportValidTargetTestCase {

  protected ArtifactValidation getArtifactValidation() {
    return new ImportValidTarget();
  }

  @Test
  public void importOk() {
    ArtifactAst artifact = mock(ArtifactAst.class);

    ImportedResource imported = mock(ImportedResource.class);
    when(imported.getResolutionFailure()).thenReturn(empty());
    when(artifact.getImportedResources()).thenReturn(singleton(imported));

    assertThat(getArtifactValidation().validate(artifact), is(empty()));
  }

  @Test
  public void importWithFailure() {
    ArtifactAst artifact = mock(ArtifactAst.class);

    ImportedResource imported = mock(ImportedResource.class);
    String failureMessage = "Something went wrong :(";
    when(imported.getResolutionFailure()).thenReturn(of(failureMessage));
    when(artifact.getImportedResources()).thenReturn(singleton(imported));

    ArtifactValidation validation = getArtifactValidation();
    Optional<ValidationResultItem> result = validation.validate(artifact);
    assertThat(result, is(not(empty())));
    assertThat(result.get().getValidation(), sameInstance(validation));
    assertThat(result.get().getMessage(), is(failureMessage));
  }
}
