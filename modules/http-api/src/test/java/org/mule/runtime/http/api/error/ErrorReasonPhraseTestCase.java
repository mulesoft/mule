/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
