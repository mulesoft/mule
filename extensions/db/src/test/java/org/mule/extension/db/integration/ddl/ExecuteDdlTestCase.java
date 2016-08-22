/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.ddl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestDbConfig.getResources;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.api.message.MuleMessage;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class ExecuteDdlTestCase extends AbstractDbIntegrationTestCase {

  public ExecuteDdlTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/ddl/execute-ddl-config.xml"};
  }

  @Before
  public void deleteTestDdlTable() throws Exception {

    DataSource dataSource = getDefaultDataSource();
    try (Connection connection = dataSource.getConnection()) {
      QueryRunner qr = new QueryRunner(dataSource);
      qr.update(connection, "DROP TABLE TestDdl");
    } catch (SQLException e) {
      // Ignore: table does not exist
    }
  }

  @Test
  public void executeDdl() throws Exception {
    MuleMessage response = flowRunner("executeDdl").run().getMessage();
    assertTableCreation(response.getPayload());
  }

  protected void assertTableCreation(int affectedRows) throws SQLException {
    assertThat(affectedRows, is(0));
    List<Map<String, String>> result = selectData("select * from TestDdl", getDefaultDataSource());
    assertRecords(result);
  }
}
