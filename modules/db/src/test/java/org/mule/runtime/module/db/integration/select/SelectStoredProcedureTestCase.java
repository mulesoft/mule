/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.select;

import static org.junit.Assume.assumeThat;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getAllPlanetRecords;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.matcher.SupportsReturningStoredProcedureResultsWithoutParameters;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

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

  @Test
  public void selectsFromStoredProcedure() throws Exception {
    final MuleEvent responseEvent = flowRunner("selectStoredProcedure").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    assumeThat(getDefaultDataSource(), new SupportsReturningStoredProcedureResultsWithoutParameters());
    testDatabase.createStoredProcedureGetRecords(getDefaultDataSource());
  }
}
