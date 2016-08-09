/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.handlers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate;

import org.junit.Test;

public class DefaultRetryPolicyTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/handlers/default-retry-policy.xml";
  }

  @Test
  public void testConnectorPolicy() throws Exception {
    Connector c = muleContext.getRegistry().lookupObject("testConnector");
    assertThat(c, not(nullValue()));

    RetryPolicyTemplate rpf = c.getRetryPolicyTemplate();
    assertThat(rpf, not(nullValue()));
    assertThat(rpf, instanceOf(SimpleRetryPolicyTemplate.class));
    assertThat(((SimpleRetryPolicyTemplate) rpf).getCount(), is(3));

    assertThat(c.isConnected(), is(true));
    assertThat(c.isStarted(), is(true));
  }
}
