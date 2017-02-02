/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;

import org.junit.Test;

public class MuleClientStandaloneTestCase extends AbstractMuleClientTestCase {

  @Override
  protected MuleClient createMuleClient() throws MuleException {
    return new MuleClient();
  }

  @Override
  public void after() {
    getMuleClient().dispose();
    assertThat(getMuleClient().getMuleContext().isInitialised(), is(false));
    assertThat(getMuleClient().getMuleContext().isStarted(), is(false));
  }

  @Test
  public void testCreateMuleClient() throws MuleException {
    assertThat(getMuleClient().getMuleContext(), not(sameInstance(muleContext)));
    assertThat(getMuleClient().getMuleContext().isInitialised(), is(true));
    assertThat(getMuleClient().getMuleContext().isStarted(), is(true));
    getMuleClient().dispatch("test://test", "message", null);
    getMuleClient().send("test://test", "message", null);
  }

}
