/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.streaming;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.StreamingStatistics;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import javax.inject.Inject;

abstract class AbstractStreamingExtensionTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  private StreamingManager streamingManager;

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    assertAllStreamingResourcesClosed();
  }

  private void assertAllStreamingResourcesClosed() {
    StreamingStatistics stats = streamingManager.getStreamingStatistics();
    new PollingProber(10000, 100).check(new JUnitLambdaProbe(() -> {
      assertThat("There're still open cursor providers", stats.getOpenCursorProvidersCount(), is(0));
      assertThat("There're still open cursors", stats.getOpenCursorsCount(), is(0));
      return true;
    }));
  }

}
