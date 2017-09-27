/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Calendar;

public class OptionalObjectsIgnoredTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(OptionalObjectsIgnoredTestCase.class);
  private static final String OPTIONAL_OBJECT_KEY = "problematic";

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private MuleContextWithRegistries muleContext;

  @Before
  public void before() throws Exception {
    muleContext =
        (MuleContextWithRegistries) new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                                      new SpringXmlConfigurationBuilder(new String[0],
                                                                                                                        emptyMap()));
    muleContext.start();
    muleContext.getRegistry().lookupByType(Calendar.class);
  }

  @After
  public void after() throws Exception {
    if (muleContext != null) {
      disposeIfNeeded(muleContext, LOGGER);
    }
  }

  @Test
  public void optionalObjectSafelyIgnored() {
    assertThat(muleContext.getRegistry().lookupObject(OPTIONAL_OBJECT_KEY), is(nullValue()));
  }
}
