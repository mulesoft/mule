/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.alert;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;


public class DefaultAlertingSupportTestCase extends AbstractMuleTestCase {

  @Test
  public void a() {
    final var alertingSupport = new DefaultAlertingSupport();

    alertingSupport.triggerAlert("lala");
    alertingSupport.triggerAlert("lala2");

    final var alertsCountAggregation = alertingSupport.alertsCountAggregation();
    System.out.println(alertsCountAggregation);
  }
}
