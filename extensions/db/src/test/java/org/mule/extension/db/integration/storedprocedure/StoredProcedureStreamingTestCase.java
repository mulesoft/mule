/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.TestDbConfig.getResources;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Ignore("MULE-10511")
public class StoredProcedureStreamingTestCase extends AbstractDbIntegrationTestCase {

  public StoredProcedureStreamingTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return getResources();
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureDoubleMyInt(getDefaultDataSource());
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/storedprocedure/stored-procedure-streaming-config.xml"};
  }

  @Test
  public void streamingInOutParam() throws Exception {
    Map<String, Object> payload = runProcedure("streamingInOutParam");
    // Apparently Derby has a bug: when there are no resultset returned, then
    // there is a fake updateCount=0 that is returned. Check how this works in other DB vendors.
    // assertThat(payload.size(), equalTo(2));
    // Compares string in to avoid problems when different DB return different integer classes (BigDecimal, integer, etc)
    assertThat("6", equalTo(payload.get("myInt").toString()));
  }


}
