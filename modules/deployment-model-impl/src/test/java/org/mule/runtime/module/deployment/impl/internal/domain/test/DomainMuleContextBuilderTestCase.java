/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainMuleContextBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DomainMuleContextBuilderTestCase extends AbstractMuleTestCase {

  @Test
  public void createsContainerConfiguration() throws Exception {
    DomainMuleContextBuilder builder = new DomainMuleContextBuilder("test");

    MuleConfiguration muleConfiguration = builder.getMuleConfiguration();

    assertThat(muleConfiguration.isContainerMode(), is(true));
  }
}
