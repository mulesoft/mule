/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.storedprocedure;

import static org.junit.Assert.assertThat;
import static org.mule.module.db.integration.TestDbConfig.getResources;
import org.mule.api.MuleEvent;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.model.AbstractTestDatabase;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

public class StoredProcedureClobInputParamTestCase extends AbstractDbIntegrationTestCase
{

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  public StoredProcedureClobInputParamTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
  {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters()
  {
    return getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources()
  {
    return new String[] {"integration/storedprocedure/stored-procedure-clob-input-param-config.xml"};
  }

  @Test
  public void convertsStringToClob() throws Exception
  {
    MuleEvent response = runFlow("clobInputParameter", TEST_MESSAGE);

    assertThat(response.getMessage().getPayload(), Matchers.<Object>equalTo(TEST_MESSAGE));
  }

  @Before
  public void setupStoredProcedure() throws Exception
  {
    testDatabase.createStoredProcedureParameterizedUpdatePlanetDescription(getDefaultDataSource());
  }
}
