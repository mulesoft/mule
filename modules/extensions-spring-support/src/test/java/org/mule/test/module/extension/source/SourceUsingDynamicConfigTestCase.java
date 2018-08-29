/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.util.ExceptionUtils;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.test.heisenberg.extension.AsyncHeisenbergSource.completionCallback;

public class SourceUsingDynamicConfigTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  @Named(value = LAZY_COMPONENT_INITIALIZER_SERVICE_KEY)
  private LazyComponentInitializer lazyComponentInitializer;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  protected String getConfigFile() {
    return "heisenberg-source-dynamic-config.xml";
  }

  @Test
  public void usingDynamicConfig() {
    expectedException.expectMessage("Sources cannot use dynamic configurations");
    lazyComponentInitializer.initializeComponent(Location.builder().globalName("source").build());
  }
}
