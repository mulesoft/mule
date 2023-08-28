/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.lifecycle;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.isLazyInitMode;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class LifecycleUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void isLazyInit() {
    ConfigurationProperties configurationProperties = mock(ConfigurationProperties.class);

    when(configurationProperties.resolveBooleanProperty(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY)).thenReturn(of(true));
    assertThat(isLazyInitMode(configurationProperties), is(true));

    when(configurationProperties.resolveBooleanProperty(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY)).thenReturn(of(false));
    assertThat(isLazyInitMode(configurationProperties), is(false));

    when(configurationProperties.resolveBooleanProperty(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY)).thenReturn(empty());
    assertThat(isLazyInitMode(configurationProperties), is(false));

    assertThat(isLazyInitMode(null), is(false));
  }
}
