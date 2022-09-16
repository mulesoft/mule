/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.core.internal.management.stats.DefaultFlowsSummaryStatistics.isApiKitFlow;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.PRICING_METRICS;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.FlowSummaryStory.ACTIVE_FLOWS_SUMMARY;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Note: flow-mappings are not considered
 *
 */
@SmallTest
@Feature(PRICING_METRICS)
@Story(ACTIVE_FLOWS_SUMMARY)
public class FlowsSummaryStatisticsApikitTestCase extends AbstractMuleTestCase {

  @Test
  public void noApikitFlowSimpleName() {
    assertThat(isApiKitFlow("privateFlow"), is(false));
  }

  @Test
  public void apikitSoapFlowMethod() {
    // APiKit soap does not consider content type
    assertThat(isApiKitFlow("ListInventory:\\config"), is(true));
  }

  @Test
  public void apikitFlowMethodGet() {
    assertThat(isApiKitFlow("get:\\reservation:api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPost() {
    assertThat(isApiKitFlow("post:\\reservation:api-config"), is(true));
  }

  /**
   * The HTTP methods may be anything, not just the commonly used ones. So, since APIKIT may route a request with any method, the
   * flows handling these methods are valid and must be considered as APIKIT flows.
   */
  @Test
  public void apikitFlowMethodAnything() {
    assertThat(isApiKitFlow("randomize:\\reservation:api-config"), is(true));

  }

  @Test
  public void apikitFlowWithContentType() {
    assertThat(isApiKitFlow("put:\\accounts:application\\json:account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithMorePathParts() {
    assertThat(isApiKitFlow("delete:\\accounts\\myAccount:account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithParamPathPart() {
    assertThat(isApiKitFlow("delete:\\accounts\\(id):account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithPathPartWithNumbers() {
    assertThat(isApiKitFlow("delete:\\accounts101\\(id):account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithPathPartWithSpecialChars() {
    assertThat(isApiKitFlow("delete:\\accounts.-_~!$&'()*+,;=:@\\(id):account-domain-api-config"), is(true));
  }

  @Test
  public void apikitFlowMethodPathWithPathPartWithEncodedChars() {
    assertThat(isApiKitFlow("delete:\\accounts\\(id)\\%00:account-domain-api-config"), is(true));
  }

}
