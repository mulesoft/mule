/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.vendor.oracle;

import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.storedprocedure.AbstractStoredProcedureReturningResultsetTestCase;

import java.util.List;

import org.junit.runners.Parameterized;

public class OracleStoredProcedureResultsetAssignmentTestCase extends AbstractStoredProcedureReturningResultsetTestCase {

  public OracleStoredProcedureResultsetAssignmentTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getOracleResource();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/vendor/oracle/stored-procedure-resultset-assignment-config.xml"};
  }

  @Override
  public void setupStoredProcedure() throws Exception {
    testDatabase.createFunctionGetRecords(getDefaultDataSource());
  }
}
