/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
