/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;

import org.junit.Test;

public class MuleClientTestCase extends AbstractMuleClientTestCase {

  @Test
  public void testCreateMuleClient() throws MuleException {
    assertThat(muleContext, not(nullValue()));
    assertThat(getMuleClient().getMuleContext(), is(muleContext));
    assertThat(muleContext.isInitialised(), is(true));

    muleContext.start();

    assertThat(muleContext.isStarted(), is(true));
    getMuleClient().dispatch("test://test", "message", null);
    getMuleClient().send("test://test", "message", null);
    getMuleClient().dispose();
    assertThat(getMuleClient().getMuleContext().isInitialised(), is(true));
    assertThat(getMuleClient().getMuleContext().isStarted(), is(true));
  }

}
