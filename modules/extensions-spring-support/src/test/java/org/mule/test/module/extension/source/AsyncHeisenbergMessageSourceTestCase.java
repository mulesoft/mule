/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.test.heisenberg.extension.AsyncHeisenbergSource.completionCallback;

import org.junit.Test;

public class AsyncHeisenbergMessageSourceTestCase extends HeisenbergMessageSourceTestCase {

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    completionCallback = null;
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    completionCallback = null;
  }

  @Override
  protected String getConfigFile() {
    return "heisenberg-async-source-config.xml";
  }

  @Test
  public void asyncSource() throws Exception {
    startFlow("source");

    try {
      assertSourceCompleted();
      fail("Source should not have completed");
    } catch (AssertionError e) {
      assertThat(completionCallback, is(notNullValue()));
      completionCallback.success();
      assertSourceCompleted();
    }
  }

  @Test
  public void asyncOnException() throws Exception {
    startFlow("sourceFailed");

    try {
      assertSourceFailed();
      fail("Source should not have completed");
    } catch (AssertionError e) {
      assertThat(completionCallback, is(notNullValue()));
      completionCallback.error(new Exception());
      assertSourceFailed();
    }
  }
}
