/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.util.TestTimeSupplier;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

@SmallTest
public class DefaultMutableConfigurationStatsTestCase extends AbstractMuleTestCase {

  private TestTimeSupplier timeSupplier = new TestTimeSupplier(System.currentTimeMillis());
  private MutableConfigurationStats stats = new DefaultMutableConfigurationStats(timeSupplier);

  @Test
  public void lastUsed() {
    assertThat(stats.getLastUsedMillis(), is(timeSupplier.get()));
  }

  @Test
  public void updateLastUsed() {
    lastUsed();
    long now = timeSupplier.move(1, TimeUnit.MINUTES);
    assertThat(stats.getLastUsedMillis() + 60000, is(now));
    stats.updateLastUsed();

    assertThat(stats.getLastUsedMillis(), is(now));
  }

  @Test
  public void inflightOperations() {
    assertThat(stats.getInflightOperations(), is(0));
    assertThat(stats.addInflightOperation(), is(1));
    assertThat(stats.getInflightOperations(), is(1));
    assertThat(stats.discountInflightOperation(), is(0));
    assertThat(stats.getInflightOperations(), is(0));
  }
}
