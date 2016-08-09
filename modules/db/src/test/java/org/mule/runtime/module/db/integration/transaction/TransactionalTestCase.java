/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.transaction;

import static junit.framework.TestCase.fail;
import static org.mule.runtime.module.db.integration.DbTestUtil.selectData;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.runtime.module.db.integration.model.Planet.MARS;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.Field;
import org.mule.runtime.module.db.integration.model.Record;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class TransactionalTestCase extends AbstractDbIntegrationTestCase {

  public TransactionalTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/transaction/transactional-config.xml"};
  }

  @Test
  public void commitsChanges() throws Exception {
    runFlowWithError("jdbcCommit");

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

  @Test
  public void rollbacksChanges() throws Exception {
    runFlowWithError("jdbcRollback");

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", MARS.getName()), new Field("POSITION", 4)));
  }

  @Test
  public void commitsChangesWhenMpIsNotTransactionalOnRollback() throws Exception {
    runFlowWithError("commitWithNonTransactionalMP");

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

  @Test
  public void commitsChangesWhenMpIsNotTransactionalOnCommit() throws Exception {
    final String flowName = "rollbackWithNonTransactionalMP";
    runFlowWithError(flowName);

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

  private void runFlowWithError(String flowName) {
    try {
      flowRunner(flowName).withPayload(TEST_MESSAGE).run();
      fail("Exception expected");
    } catch (Exception e) {
      // Ignore
    }
  }

}
