/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.model;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

import io.qameta.allure.Issue;
import org.junit.Test;

import java.util.List;

public class ApplicationModelTestCase {

  @Test
  @Issue("MULE-20020")
  public void testPrepareAstMustReturnsACollection() {
    assertThat(ApplicationModelAstPostProcessor.AST_POST_PROCESSORS.get(), instanceOf(List.class));
  }

}
