/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.select;

import static org.mule.extension.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.extension.db.integration.TestRecordUtil.getMarsRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getVenusRecord;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.api.message.Message;

import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectNameParamOverrideTestCase extends AbstractDbIntegrationTestCase {

  public SelectNameParamOverrideTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-name-param-override-config.xml"};
  }

  @Test
  public void usesParamOverriddenByName() throws Exception {
    Message response = flowRunner("overriddenParamsByName").run().getMessage();
    assertMessageContains(response, getMarsRecord());
  }

  @Test
  public void usesInlineParamOverriddenByName() throws Exception {
    Message response = flowRunner("inlineOverriddenParamsByName").run().getMessage();
    assertMessageContains(response, getVenusRecord());
  }
}

