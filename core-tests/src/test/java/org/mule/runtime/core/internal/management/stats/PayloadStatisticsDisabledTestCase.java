/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_PAYLOAD_STATISTICS;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STATISTICS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.tck.junit4.rule.SystemProperty;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(STATISTICS)
public class PayloadStatisticsDisabledTestCase extends AbstractPayloadStatisticsTestCase {

  @Rule
  public SystemProperty muleEnableStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, "true");

  @Rule
  public SystemProperty muleDisablePayloadStatistics = new SystemProperty(MULE_DISABLE_PAYLOAD_STATISTICS, "true");

  @Test
  public void decorateInputStreamDisabled() throws IOException {
    final AllStatistics allStatistics = muleContext.getStatistics();
    // By default the payload stats are disabled by system property
    verifyStatisticsOnDecoratedStream(component1, 0L, false);

    // Enable stats and verify that they are computed for same component
    allStatistics.enablePayloadStatistics(true);
    verifyStatisticsOnDecoratedStream(component1, 1L, true);

    // Add a new component to stats and verify that the stats are computed
    verifyStatisticsOnDecoratedStream(component2, 1L, true);

    // Verify that the stats are not computed when disabled
    allStatistics.enablePayloadStatistics(false);
    verifyStatisticsOnDecoratedStream(component2, 1L, false);
  }

  private void verifyStatisticsOnDecoratedStream(Component component, Long expectedInputByteCount, boolean expectedStatsEnabled)
      throws IOException {
    final InputStream decorated = new ByteArrayInputStream("Hello World".getBytes(UTF_8));
    final InputStream decorator = decoratorFactory.componentDecoratorFactory(component).decorateInput(decorated, CORR_ID);
    final PayloadStatistics statistics = muleContext.getStatistics().getPayloadStatistics(component.getLocation().getLocation());
    decorator.read();
    assertThat(statistics.getInputByteCount(), is(expectedInputByteCount));
    assertThat(statistics.isEnabled(), is(expectedStatsEnabled));
  }

}
