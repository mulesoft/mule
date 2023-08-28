/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;

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
