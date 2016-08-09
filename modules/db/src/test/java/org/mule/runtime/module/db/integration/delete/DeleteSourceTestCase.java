/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.delete;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.db.integration.model.Planet.VENUS;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.sql.SQLException;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class DeleteSourceTestCase extends AbstractDbIntegrationTestCase {

  public DeleteSourceTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/delete/delete-source-config.xml"};
  }

  @Test
  public void usesDefaultSource() throws Exception {
    final MuleEvent responseEvent = flowRunner("delete").withPayload(VENUS.getName()).run();

    final MuleMessage response = responseEvent.getMessage();
    assertDelete(response);
  }

  @Test
  public void usesCustomSource() throws Exception {
    final MuleEvent responseEvent = flowRunner("deleteCustomSource").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertDelete(response);
  }

  private void assertDelete(MuleMessage response) throws SQLException {
    assertThat(response.getPayload(), equalTo(1));

    assertDeletedPlanetRecords(VENUS.getName());
  }
}
