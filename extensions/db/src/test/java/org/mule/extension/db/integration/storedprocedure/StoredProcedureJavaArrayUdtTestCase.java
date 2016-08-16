/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;
import static org.mule.extension.db.integration.model.Contact.CONTACT1;
import static org.mule.extension.db.integration.model.Region.SOUTHWEST;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.runtime.api.message.MuleMessage;

import java.util.LinkedList;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureJavaArrayUdtTestCase extends AbstractDbIntegrationTestCase {

  public StoredProcedureJavaArrayUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    List<Object[]> params = new LinkedList<>();

    if (!getOracleResource().isEmpty()) {
      params.add(new Object[] {"integration/config/oracle-mapped-udt-db-config.xml", new OracleTestDatabase()});
    }

    return params;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-udt-array-config.xml"};
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    final DataSource dataSource = getDefaultDataSource();
    testDatabase.createStoredProcedureGetZipCodes(dataSource);
    testDatabase.createStoredProcedureGetContactDetails(dataSource);
  }

  @Test
  public void returnsDefaultArray() throws Exception {
    MuleMessage response = flowRunner("returnsDefaultArrayValue").run().getMessage();
    assertThat(response.getPayload(), equalTo(SOUTHWEST.getZips()));
  }

  @Test
  public void returnsCustomArray() throws Exception {
    MuleMessage response = flowRunner("returnsCustomArrayValue").run().getMessage();
    assertThat(response.getPayload(), equalTo(CONTACT1.getDetails()));
  }
}
