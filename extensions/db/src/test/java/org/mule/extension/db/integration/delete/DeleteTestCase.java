/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.delete;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.TestDbConfig.getResources;
import static org.mule.extension.db.integration.model.Planet.VENUS;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.api.message.Message;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class DeleteTestCase extends AbstractDbIntegrationTestCase {

  public DeleteTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/delete/delete-config.xml"};
  }

  @Test
  public void deleteDynamic() throws Exception {
    doDelete("deleteDynamic", VENUS.getName());
  }

  @Test
  public void deleteParemeterized() throws Exception {
    doDelete("deleteParameterized", VENUS.getName());
  }

  private void doDelete(String flowName, String payload) throws Exception {
    Message response = flowRunner(flowName).withPayload(payload).run().getMessage();
    assertThat(response.getPayload().getValue(), is(1));
    assertDeletedPlanetRecords(payload);
  }
}
