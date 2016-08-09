/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getAllPlanetRecords;

import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.matcher.SupportsStoredFunctionsUsingCallSyntax;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.internal.result.resultset.ResultSetIterator;

import java.util.List;
import java.util.Map;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class StoredProcedureStreamingResourceManagementTestCase extends AbstractDbIntegrationTestCase {

  public StoredProcedureStreamingResourceManagementTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/config/derby-pooling-db-config.xml",
        "integration/storedprocedure/stored-procedure-streaming-resource-management-config.xml"};
  }

  @Test
  public void closesConnectionsWhenStatementConsumed() throws Exception {
    doSuccessfulMessageTest();
    doSuccessfulMessageTest();
    doSuccessfulMessageTest();
  }

  private void doSuccessfulMessageTest() throws Exception {
    final MuleEvent responseEvent = flowRunner("storedProcedureStreaming").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    Map payload = (Map) response.getPayload();

    assertThat(payload.size(), IsEqual.equalTo(1));
    assertThat(payload.get("resultSet1"), is(instanceOf(ResultSetIterator.class)));
    assertThat(response.getOutboundProperty("processedResults"), is(instanceOf(List.class)));
    assertRecords(response.getOutboundProperty("processedResults"), getAllPlanetRecords());
  }

  @Test
  public void closesConnectionsOnProcessingError() throws Exception {
    doFailedMessageTest();
    doFailedMessageTest();
    doFailedMessageTest();
  }

  private void doFailedMessageTest() throws Exception {
    MessagingException e = flowRunner("storedProcedureStreamingError").withPayload(TEST_MESSAGE).runExpectingException();
    assertThat(e.getMessage(), containsString("Failing test on purpose"));
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    assumeThat(getDefaultDataSource(), new SupportsStoredFunctionsUsingCallSyntax());
    testDatabase.createStoredProcedureGetRecords(getDefaultDataSource());
  }
}
