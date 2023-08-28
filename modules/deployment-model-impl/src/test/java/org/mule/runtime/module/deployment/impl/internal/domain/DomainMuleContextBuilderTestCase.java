/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

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
