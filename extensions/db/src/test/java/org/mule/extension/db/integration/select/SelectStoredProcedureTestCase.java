/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import static org.junit.Assume.assumeThat;
import static org.mule.extension.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.extension.db.integration.TestRecordUtil.getAllPlanetRecords;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.api.message.Message;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectStoredProcedureTestCase extends AbstractDbIntegrationTestCase {

  public SelectStoredProcedureTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-stored-procedure-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
    testDatabase.createStoredProcedureGetRecords(getDefaultDataSource());
  }

  @Test
  public void selectsFromStoredProcedure() throws Exception {
    Message response = flowRunner("selectStoredProcedure").run().getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

}
