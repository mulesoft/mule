/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.vendor.oracle;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class OracleSelectsLongTransactionTestCase extends AbstractDbIntegrationTestCase {

  public OracleSelectsLongTransactionTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getOracleResource();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/vendor/oracle/selects-long-transaction-config.xml"};
  }

  @Test
  public void longTransaction() throws Exception {
    List<Integer> sequence = new ArrayList<>();
    for (int i = 0; i < 500; i++) {
      sequence.add(i);
    }
    final MuleEvent responseEvent = flowRunner("longTransaction").withPayload(sequence).run();
    assertThat(responseEvent.getError(), nullValue());
  }
}
