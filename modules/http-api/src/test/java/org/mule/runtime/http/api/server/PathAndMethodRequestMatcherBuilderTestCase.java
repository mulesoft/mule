/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server;

import static org.mule.runtime.http.api.AllureConstants.HttpFeature.HTTP_SERVICE;

import io.qameta.allure.Feature;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(HTTP_SERVICE)
public class PathAndMethodRequestMatcherBuilderTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void methodCannotBeNull() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("method matcher cannot be null");
    PathAndMethodRequestMatcher.builder().methodRequestMatcher(null);
  }

  @Test
  public void pathCannotBeNull() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("path cannot be empty nor null");
    PathAndMethodRequestMatcher.builder().path(null);
  }

  @Test
  public void pathCannotBeEmpty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("path cannot be empty nor null");
    PathAndMethodRequestMatcher.builder().path("     ");
  }

}
