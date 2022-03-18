/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.tooling.internal.bootstrap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tooling.internal.util.bootstrap.ToolingMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class ToolingMuleContextFactoryTestCase extends AbstractMuleTestCase {

  private ToolingMuleContextFactory factory = new ToolingMuleContextFactory();

  @Test
  public void createMuleContext() throws Exception {
    MuleContext muleContext = factory.createMuleContext(true);

    assertThat(muleContext.isInitialised(), is(true));
    assertThat(muleContext.isStarted(), is(true));
    assertThat(muleContext.getExpressionManager(), is(notNullValue()));
    assertThat(muleContext.getConfiguration().getDefaultEncoding(), is(not((isEmptyString()))));
  }
}
