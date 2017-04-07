/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.update;

import static org.mule.extension.db.integration.TestDbConfig.getOracleResource;
import static org.mule.extension.db.integration.model.RegionManager.SOUTHWEST_MANAGER;
import static org.mule.test.allure.AllureConstants.DbFeature.DB_EXTENSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.OracleTestDatabase;
import org.mule.runtime.api.message.Message;

import java.sql.Struct;
import java.util.LinkedList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(DB_EXTENSION)
@Stories("Update Statement")
public class UpdateJavaUdtTestCase extends AbstractDbIntegrationTestCase {

  @Parameterized.Parameters(name = "{2}")
  public static List<Object[]> parameters() {
    List<Object[]> params = new LinkedList<>();
    if (!getOracleResource().isEmpty()) {
      final OracleTestDatabase oracleTestDatabase = new OracleTestDatabase();
      params.add(new Object[] {"integration/config/oracle-unmapped-udt-db-config.xml", oracleTestDatabase,
          oracleTestDatabase.getDbType()});
    }

    return params;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-udt-config.xml"};
  }

  @Test
  public void updatesWithStruct() throws Exception {
    Message response = flowRunner("updatesWithStruct").run().getMessage();
    assertThat(((Struct) response.getPayload().getValue()).getAttributes(),
               equalTo(SOUTHWEST_MANAGER.getContactDetails().asObjectArray()));
  }

  @Ignore("MULE-11162: db:parameter-types are ignored")
  @Test
  public void updatesWithArray() throws Exception {
    Object[] payload = SOUTHWEST_MANAGER.getContactDetails().asObjectArray();

    Message response = flowRunner("updatesWithObject").withPayload(payload).run().getMessage();
    assertThat(((Struct) response.getPayload().getValue()).getAttributes(),
               equalTo(SOUTHWEST_MANAGER.getContactDetails().asObjectArray()));
  }

}
