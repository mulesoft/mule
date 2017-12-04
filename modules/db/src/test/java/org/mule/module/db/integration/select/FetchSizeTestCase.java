/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.integration.select;

import static org.mule.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.module.db.integration.TestRecordUtil.getAllPlanetRecords;
import org.mule.api.MuleEvent;
import org.mule.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.module.db.integration.TestDbConfig;
import org.mule.module.db.integration.model.AbstractTestDatabase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class FetchSizeTestCase extends AbstractDbIntegrationTestCase {


  @Rule
  public SystemProperty fetchSize = new SystemProperty("fetchSize", "2");

  public FetchSizeTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase)
  {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters()
  {
    return TestDbConfig.getResources();
  }

  @Override protected String[] getFlowConfigurationResources() {
    return new String[] { "integration/select/select-fetch-size-config.xml"};
  }

  @Test
  public void usesFetchSizeFromProperty() throws Exception {
    MuleEvent event = runFlow("fetchSizePlaceHolder", TEST_PAYLOAD);

    assertMessageContains(event.getMessage(), getAllPlanetRecords());
  }
}
