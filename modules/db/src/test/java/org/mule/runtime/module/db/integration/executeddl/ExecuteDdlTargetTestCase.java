/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.executeddl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class ExecuteDdlTargetTestCase extends AbstractExecuteDdlTestCase {

  public ExecuteDdlTargetTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/executeddl/execute-ddl-target-config.xml"};
  }

  @Test
  public void usesCustomTarget() throws Exception {
    final MuleEvent responseEvent = flowRunner("executeDdlCustomTarget").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertThat(getPayloadAsString(response), equalTo(TEST_MESSAGE));
    assertTableCreation(response.<Integer>getOutboundProperty("updateCount"));
  }
}
