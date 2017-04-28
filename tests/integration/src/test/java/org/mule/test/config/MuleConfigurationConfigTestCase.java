/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

public class MuleConfigurationConfigTestCase extends AbstractMuleTestCase {

  @ClassRule
  public static TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private MuleContext muleContext;

  @After
  public void after() {
    if (muleContext != null) {
      muleContext.dispose();
    }
  }

  @Test
  public void configurationQueueTxLogSizeExplicitValue() throws Exception {
    muleContext = new WithServicesApplicationContextBuilder()
        .setApplicationResources(new String[] {"org/mule/test/config/configuration-queue-tx-log-size-explict-config.xml"})
        .build();
    assertThat(muleContext.getConfiguration().getMaxQueueTransactionFilesSizeInMegabytes(), is(100));
  }

  @Test
  public void configurationQueueTxLogSizeDefaultValue() throws Exception {
    muleContext = new WithServicesApplicationContextBuilder().setApplicationResources(new String[] {}).build();
    assertThat(muleContext.getConfiguration().getMaxQueueTransactionFilesSizeInMegabytes(), is(500));
  }

  private static class WithServicesApplicationContextBuilder extends ApplicationContextBuilder {

    @Override
    protected void addBuilders(List<ConfigurationBuilder> builders) {
      super.addBuilders(builders);
      builders.add(testServicesConfigurationBuilder);
    }
  }
}
