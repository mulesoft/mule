/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.select;

import static org.mule.runtime.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getAllPlanetRecords;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getVenusRecord;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectWithPlaceHolderTestCase extends AbstractDbIntegrationTestCase {

  @Rule
  public SystemProperty query = new SystemProperty("query", "select * from PLANET order by ID");

  @Rule
  public SystemProperty param = new SystemProperty("param", "2");

  public SelectWithPlaceHolderTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-placeholder-config.xml"};
  }

  @Test
  public void replacesPlaceholderInParameterizedQuery() throws Exception {
    final MuleEvent responseEvent = flowRunner("placeholderParameterizedQuery").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Test
  public void replacesPlaceholderInDynamicQuery() throws Exception {
    final MuleEvent responseEvent = flowRunner("placeholderDynamicQuery").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Test
  public void replacesPlaceholderInParameterizedQueryParam() throws Exception {
    final MuleEvent responseEvent = flowRunner("placeholderParameterizedQueryParam").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertMessageContains(response, getVenusRecord());
  }
}
