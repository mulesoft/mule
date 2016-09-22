/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.insert;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.api.message.Message;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class InsertTestCase extends AbstractDbIntegrationTestCase {

  public InsertTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/insert/insert-default-config.xml"};
  }

  @Test
  public void insert() throws Exception {
    Message response = flowRunner("insert").run().getMessage();
    StatementResult result = (StatementResult) response.getPayload().getValue();
    assertThat(result.getAffectedRows(), is(1));
    assertThat(result.getGeneratedKeys().isEmpty(), is(true));
  }

  @Test
  public void insertDynamic() throws Exception {
    final String planet = "'Mercury'";
    Message response = flowRunner("insertDynamic").withPayload(planet).run().getMessage();
    assertInsert((StatementResult) response.getPayload().getValue(), planet);
  }

  @Test
  public void usesParameterizedQuery() throws Exception {
    final String planet = "Pluto";
    Message response = doInsertParameterized(777, planet);
    assertInsert((StatementResult) response.getPayload().getValue(), planet);
  }

  @Test
  public void insertMany() throws Exception {
    for (int i = 0; i < 10; i++) {
      int id = 1000 + i;
      doInsertParameterized(id, "Planet-" + id);
    }

    List<Map<String, Object>> planets = selectData("select * from PLANET", getDefaultDataSource());
    assertThat(planets, hasSize(13));
  }

  private Message doInsertParameterized(int id, String planet) throws Exception {
    return flowRunner("insertParameterized").withPayload(planet).withVariable("id", id).run().getMessage();
  }

  private void assertInsert(StatementResult result, String planetName) throws SQLException {
    assertThat(result.getAffectedRows(), is(1));
    assertThat(result.getGeneratedKeys().isEmpty(), is(true));
    assertPlanetRecordsFromQuery(planetName);
  }

}
