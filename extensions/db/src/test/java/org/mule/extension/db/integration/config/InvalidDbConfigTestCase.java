/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.config;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mule.extension.db.integration.TestDbConfig.getResources;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.core.api.lifecycle.InitialisationException;

import java.sql.SQLException;
import java.util.List;

import org.enhydra.jdbc.standard.StandardDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;


public class InvalidDbConfigTestCase extends AbstractDbIntegrationTestCase {

  public InvalidDbConfigTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/config/datasource-invalid-config.xml"};
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return getResources();
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(containsString("parameters cannot be set at the same time"));
  }

  @Test
  public void failDueToExclusiveDatasourceOptionalsViolation() throws SQLException {
    StandardDataSource dataSource = new StandardDataSource();
    dataSource.setDriverName("org.apache.derby.jdbc.EmbeddedDriver");
    dataSource.setUrl("jdbc:derby:muleEmbeddedDB;sql.enforce_strict_size=true");

    flowRunner("usesBeanDatasourceConfig").withVariable("jdbcDataSource", dataSource);
  }
}
