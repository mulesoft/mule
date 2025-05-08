/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.alert;

import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SUPPORTABILITY;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SupportabilityStory.ALERTS;

import static java.lang.System.currentTimeMillis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.util.TestTimeSupplier;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SUPPORTABILITY)
@Story(ALERTS)
public class DefaultAlertingSupportTestCase extends AbstractMuleTestCase {

  @Test
  public void alertsCountAggregation() {
    final var alertingSupport = new DefaultAlertingSupport();
    alertingSupport.setTimeSupplier(new TestTimeSupplier(currentTimeMillis()));

    alertingSupport.triggerAlert("a");
    alertingSupport.triggerAlert("b");
    alertingSupport.triggerAlert("b");

    final var alertsCountAggregation = alertingSupport.alertsCountAggregation();

    assertThat(alertsCountAggregation, hasKey("a"));
    assertThat(alertsCountAggregation, hasKey("b"));

    assertThat(alertsCountAggregation.get("a").forLast1MinInterval(), is(1));
    assertThat(alertsCountAggregation.get("b").forLast1MinInterval(), is(2));
  }
}
