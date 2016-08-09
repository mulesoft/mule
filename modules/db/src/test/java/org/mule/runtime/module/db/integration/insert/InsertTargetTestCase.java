/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.insert;

import static org.junit.Assert.assertEquals;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class InsertTargetTestCase extends AbstractDbIntegrationTestCase {

  public InsertTargetTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/insert/insert-target-config.xml"};
  }

  @Test
  public void usesCustomTarget() throws Exception {
    final MuleEvent responseEvent = flowRunner("insertCustomTarget").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertInsert(response.getOutboundProperty("updateCount"));
  }

  private void assertInsert(Object responseValue) throws SQLException {
    assertEquals(1, responseValue);

    assertPlanetRecordsFromQuery("Pluto");
  }
}
