/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.MockExtensionManagerConfigurationBuilder;
import org.mule.tck.junit4.SimpleRegistryConfigurationBuilder;

import java.util.Calendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

public class OptionalObjectsLoadedTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(OptionalObjectsLoadedTestCase.class);
  private static final String OPTIONAL_OBJECT_KEY = "optional";

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private MuleContextWithRegistry muleContext;

  @Before
  public void before() throws Exception {
    muleContext =
        (MuleContextWithRegistry) new DefaultMuleContextFactory().createMuleContext(new SimpleRegistryConfigurationBuilder(),
                                                                                    testServicesConfigurationBuilder,
                                                                                    new MockExtensionManagerConfigurationBuilder(),
                                                                                    new SpringXmlConfigurationBuilder(new String[0],
                                                                                                                      emptyMap()));
    muleContext.start();
    muleContext.getRegistry().lookupByType(Calendar.class);
  }

  @After
  public void after() {
    if (muleContext != null) {
      disposeIfNeeded(muleContext, LOGGER);
    }
  }

  @Test
  public void optionalObjectSafelyLoaded() {
    assertThat(muleContext.getRegistry().lookupObject(OPTIONAL_OBJECT_KEY), is(not(nullValue())));
  }
}
