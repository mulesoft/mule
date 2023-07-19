/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
