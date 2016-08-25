/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.db.integration;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.functional.junit4.FlowRunner;
import org.mule.runtime.module.db.integration.model.DerbyTestDatabase;
import org.mule.runtime.module.db.internal.domain.database.DbConfig;
import org.mule.runtime.module.db.internal.resolver.database.DbConfigResolver;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DbSharedConnectorTestCase extends DomainFunctionalTestCase {

  public static final String CLIENT_APP = "client";
  public static final String SERVER_APP = "server";

  private final String domainConfig;

  public DbSharedConnectorTestCase(String domainConfig) {
    this.domainConfig = domainConfig;
  }

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {{"integration/domain/db-shared-connnector.xml"},
        {"integration/domain/db-derby-shared-connnector.xml"}});
  }

  @Override
  protected String getDomainConfig() {
    return domainConfig;
  }

  @Before
  public void configDB() throws SQLException {
    final DerbyTestDatabase testDatabase = new DerbyTestDatabase();
    testDatabase.createDefaultDatabaseConfig(getDefaultDataSource());
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {new ApplicationConfig(CLIENT_APP, new String[] {"integration/domain/db-client-app.xml"}),
        new ApplicationConfig(SERVER_APP, new String[] {"integration/domain/db-server-app.xml"})};
  }

  @Test
  public void createJdbcRecordAndConsumeIt() throws Exception {
    final MuleContext clientAppMuleContext = getMuleContextForApp(CLIENT_APP);
    new FlowRunner(clientAppMuleContext, "dbClientService").withPayload(new Object()).run();

    MuleMessage response = getMuleContextForApp(SERVER_APP).getClient().request("test://out", 5000).getRight().get();
    assertThat(response, notNullValue());
  }

  public DataSource getDefaultDataSource() {
    DbConfigResolver dbConfigResolver = getMuleContextForDomain().getRegistry().get("dbConfig");
    DbConfig config = dbConfigResolver.resolve(null);

    return config.getDataSource();
  }
}
