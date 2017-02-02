/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.api.message.Message;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class StoredProcedureClobInputParamTestCase extends AbstractDbIntegrationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-clob-input-param-config.xml"};
  }

  @Test
  public void convertsStringToClob() throws Exception {
    Message response = flowRunner("clobInputParameter").withPayload(TEST_MESSAGE).run().getMessage();
    assertThat(response.getPayload().getValue(), equalTo(TEST_MESSAGE));
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureParameterizedUpdatePlanetDescription(getDefaultDataSource());
  }
}
