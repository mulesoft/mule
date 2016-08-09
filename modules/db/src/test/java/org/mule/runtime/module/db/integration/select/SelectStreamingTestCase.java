/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.select;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getAllPlanetRecords;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.internal.result.resultset.ResultSetIterator;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectStreamingTestCase extends AbstractDbIntegrationTestCase {

  public SelectStreamingTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-streaming-config.xml"};
  }

  @Test
  public void streamsRecords() throws Exception {
    final MuleEvent responseEvent = flowRunner("selectStreaming").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertThat(response.getPayload(), is(instanceOf(ResultSetIterator.class)));
    assertRecords(response.getOutboundProperty("processedRecords"), getAllPlanetRecords());
  }
}
