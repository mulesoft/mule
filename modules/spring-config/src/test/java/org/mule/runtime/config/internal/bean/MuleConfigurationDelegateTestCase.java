/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.bean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.jupiter.api.Test;

public class MuleConfigurationDelegateTestCase extends AbstractMuleTestCase {

  @Test
  void delegatesIdsProperly() {
    final var muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.getId()).thenReturn("theAppId");
    when(muleConfiguration.getDomainId()).thenReturn("theDomainId");

    final var muleContext = mock(MuleContext.class);
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

    final var muleConfigurationDelegate = new MuleConfigurationDelegate();
    muleConfigurationDelegate.setMuleContext(muleContext);
    assertThat(muleConfigurationDelegate.getId(), is("theAppId"));
    assertThat(muleConfigurationDelegate.getDomainId(), is("theDomainId"));
  }

  @Test
  void delegatesDirectoriesProperly() {
    final var muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.getWorkingDirectory()).thenReturn("theWorkDir");
    when(muleConfiguration.getMuleHomeDirectory()).thenReturn("theHomeDir");

    final var muleContext = mock(MuleContext.class);
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

    final var muleConfigurationDelegate = new MuleConfigurationDelegate();
    muleConfigurationDelegate.setMuleContext(muleContext);
    assertThat(muleConfigurationDelegate.getWorkingDirectory(), is("theWorkDir"));
    assertThat(muleConfigurationDelegate.getMuleHomeDirectory(), is("theHomeDir"));
  }

}
