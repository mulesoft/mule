/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.extension.db.integration.TestDbConfig.getDerbyResource;
import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;
import static org.mule.extension.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.runtime.api.message.Message;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("DB Extension")
@Stories("Update Statement")
public class UpdateJavaUdtTestCase extends AbstractDbIntegrationTestCase {

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    List<Object[]> params = new LinkedList<>();
    if (!getOracleResource().isEmpty()) {
      final OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      params.add(new Object[] {"integration/config/oracle-mapped-udt-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType()});
    }

    if (!getDerbyResource().isEmpty()) {
      params.add(getDerbyResource().get(0));
    }

    return params;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-udt-config.xml"};
  }

  @Test
  public void updatesObject() throws Exception {
    Message response = flowRunner("updatesObject").run().getMessage();
    assertThat(response.getPayload().getValue(), Matchers.<Object>equalTo(SOUTHWEST_MANAGER.getContactDetails()));
  }

}
