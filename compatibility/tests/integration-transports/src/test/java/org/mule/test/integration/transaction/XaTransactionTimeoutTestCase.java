/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transaction;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.functional.extensions.CompatibilityFunctionalTestCase;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.transaction.TransactionConfig;

import org.junit.Test;

public class XaTransactionTimeoutTestCase extends CompatibilityFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transaction/xa-transaction-timeout-config.xml";
  }

  @Test
  public void configuresTransactionTimeout() throws Exception {
    final Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("main");
    final InboundEndpoint inboundEndpoint = (InboundEndpoint) flow.getMessageSource();
    final TransactionConfig transactionConfig = inboundEndpoint.getTransactionConfig();

    assertThat(transactionConfig.getTimeout(), equalTo(5000));
  }
}
