/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.error;

import static org.mule.runtime.http.api.HttpConstants.HttpStatus.getReasonPhraseForStatusCode;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.ERRORS;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(HTTP_SERVICE)
@Story(ERRORS)
public class ErrorReasonPhraseTestCase {

  @Test
  @Issue("W-12293483")
  public void unprocessableEntity422() {
    assertThat(getReasonPhraseForStatusCode(422), is("Unprocessable Entity"));
  }
}
