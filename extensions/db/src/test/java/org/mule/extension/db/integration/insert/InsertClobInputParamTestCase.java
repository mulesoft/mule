/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.insert;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.api.message.Message;

import org.junit.Before;
import org.junit.Test;

public class InsertClobInputParamTestCase extends AbstractDbIntegrationTestCase {

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/insert/insert-clob-input-param-config.xml"};
  }

  @Test
  public void usesStringOnImplicitParam() throws Exception {
    Message response = flowRunner("usesStringOnImplicitParam").withPayload(TEST_MESSAGE).run().getMessage();

    assertThat(response.getPayload().getValue(), equalTo(TEST_MESSAGE));
  }

  @Test
  public void usesStringOnExplicitParam() throws Exception {
    Message response = flowRunner("usesStringOnExplicitParam").withPayload(TEST_MESSAGE).run().getMessage();

    assertThat(response.getPayload().getValue(), equalTo(TEST_MESSAGE));
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureUpdateTestType1(getDefaultDataSource());
  }
}
