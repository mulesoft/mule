/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;
import org.mule.extension.db.integration.model.derbyutil.DerbyTestStoredProcedure;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.functional.junit4.FlowRunner;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.functional.junit4.runners.ArtifactClassLoaderRunnerConfig;
import org.mule.functional.junit4.runners.RunnerDelegateTo;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
@ArtifactClassLoaderRunnerConfig(exportClasses = {DbConnectionProvider.class, DerbyTestStoredProcedure.class})
public abstract class AbstractDbIntegrationTestCase extends MuleArtifactFunctionalTestCase {

  private final String dataSourceConfigResource;
  protected final AbstractTestDatabase testDatabase;

  public AbstractDbIntegrationTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    this.dataSourceConfigResource = dataSourceConfigResource;
    this.testDatabase = testDatabase;
  }

  @Before
  public void configDB() throws SQLException {
    testDatabase.createDefaultDatabaseConfig(getDefaultDataSource());
  }

  protected DataSource getDefaultDataSource() {
    return getDefaultDataSource("dbConfig");
  }

  protected DataSource getDefaultDataSource(String configName) {
    try {
      ConfigurationProvider configurationProvider = muleContext.getRegistry().get(configName);
      DbConnectionProvider connectionProvider =
          (DbConnectionProvider) configurationProvider.get(getTestEvent("")).getConnectionProvider().get();
      return connectionProvider.getDataSource();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected final String[] getConfigFiles() {
    StringBuilder builder = new StringBuilder();

    builder.append(getDatasourceConfigurationResource());

    for (String resource : getFlowConfigurationResources()) {
      if (builder.length() != 0) {
        builder.append(",");
      }

      builder.append(resource);
    }

    return builder.toString().split(",");
  }

  protected final String getDatasourceConfigurationResource() {
    return dataSourceConfigResource;
  }

  protected abstract String[] getFlowConfigurationResources();

  protected void assertPlanetRecordsFromQuery(String... names) throws SQLException {
    if (names.length == 0) {
      throw new IllegalArgumentException("Must provide at least a name to query on the DB");
    }

    StringBuilder conditionBuilder = new StringBuilder();
    List<Record> records = new ArrayList<>(names.length);

    for (String name : names) {
      if (conditionBuilder.length() != 0) {
        conditionBuilder.append(",");
      }
      conditionBuilder.append("'").append(name).append("'");
      records.add(new Record(new Field("NAME", name)));
    }

    List<Map<String, String>> result =
        selectData(String.format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());

    assertRecords(result, records.toArray(new Record[0]));
  }


  protected void assertAffectedRows(StatementResult result, int expected) {
    assertThat(result.getAffectedRows(), is(expected));
  }

  protected void assertDeletedPlanetRecords(String... names) throws SQLException {
    if (names.length == 0) {
      throw new IllegalArgumentException("Must provide at least a name to query on the DB");
    }

    StringBuilder conditionBuilder = new StringBuilder();

    for (String name : names) {
      if (conditionBuilder.length() != 0) {
        conditionBuilder.append(",");
      }
      conditionBuilder.append("'").append(name).append("'");
    }

    List<Map<String, String>> result =
        selectData(String.format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());
    assertThat(result.size(), equalTo(0));
  }

  protected Map<String, Object> runProcedure(String flowName) throws Exception {
    return runProcedure(flowName, null);
  }

  protected Map<String, Object> runProcedure(String flowName, Object payload) throws Exception {
    FlowRunner runner = flowRunner(flowName);
    if (payload != null) {
      runner.withPayload(payload);
    }

    MuleMessage response = runner.run().getMessage();
    assertThat(response.getPayload(), is(instanceOf(Map.class)));
    return response.getPayload();

  }
}
