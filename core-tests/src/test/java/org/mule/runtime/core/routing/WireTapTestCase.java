/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.Event;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class WireTapTestCase extends AbstractMuleContextTestCase {

  protected SensingNullMessageProcessor tapListener;
  protected WireTap wireTap;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    wireTap = new WireTap();
    tapListener = getSensingNullMessageProcessor();
    wireTap.setTap(tapListener);
  }

  @Test
  public void testWireTapNoFilter() throws Exception {
    Event primaryOutput = wireTap.process(testEvent());

    assertSame(testEvent(), primaryOutput);

    assertNotNull(tapListener.event);
    assertThat(tapListener.event.getMessage().getPayload().getValue(), equalTo(testEvent().getMessage().getPayload().getValue()));
  }

  @Test
  public void testWireTapFilterAccepted() throws Exception {
    wireTap.setFilter((message, builder) -> true);

    Event primaryOutput = wireTap.process(testEvent());

    assertSame(testEvent(), primaryOutput);

    assertNotNull(tapListener.event);
    assertThat(tapListener.event.getMessage().getPayload().getValue(), equalTo(testEvent().getMessage().getPayload().getValue()));
  }

  @Test
  public void testWireTapFilterUnaccepted() throws Exception {
    wireTap.setFilter((message, builder) -> false);

    Event primaryOutput = wireTap.process(testEvent());

    assertSame(testEvent(), primaryOutput);

    assertNull(tapListener.event);
  }

  @Test
  public void testWireTapNullTap() throws Exception {
    wireTap.setTap(null);

    Event primaryOutput = wireTap.process(testEvent());

    assertSame(testEvent(), primaryOutput);
  }

}
