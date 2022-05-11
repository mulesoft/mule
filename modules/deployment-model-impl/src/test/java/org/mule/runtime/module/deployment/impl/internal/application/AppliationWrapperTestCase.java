/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.IOException;

import org.junit.Test;

import io.qameta.allure.Issue;

public class AppliationWrapperTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("MULE-19487")
  public void regsitryNullSafe() throws IOException {
    final Application app = mock(Application.class);
    when(app.getArtifactContext()).thenReturn(null);

    assertThat(new ApplicationWrapper(app).getRegistry(), is(nullValue()));
  }
}
