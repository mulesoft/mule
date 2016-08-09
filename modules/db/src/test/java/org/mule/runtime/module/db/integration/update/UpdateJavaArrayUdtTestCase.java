/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.update;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mule.runtime.module.db.integration.TestDbConfig.getOracleResource;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.ContactDetails;
import org.mule.runtime.module.db.integration.model.OracleTestDatabase;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateJavaArrayUdtTestCase extends AbstractDbIntegrationTestCase {

  public UpdateJavaArrayUdtTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    List<Object[]> params = new LinkedList<>();

    if (!getOracleResource().isEmpty()) {
      params.add(new Object[] {"integration/config/oracle-mapped-udt-db-config.xml", new OracleTestDatabase()});
    }

    return params;
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-udt-array-config.xml"};
  }

  @Test
  public void updatesStringArray() throws Exception {
    final MuleEvent responseEvent = flowRunner("updatesStringArray").withPayload(TEST_MESSAGE).run();
    final MuleMessage response = responseEvent.getMessage();

    assertThat(response.getPayload(), equalTo(new Object[] {"93101", "97201", "99210"}));
  }

  @Test
  public void updatesMappedObjectArray() throws Exception {
    final MuleEvent responseEvent = flowRunner("updatesStructArray").withPayload(TEST_MESSAGE).run();
    final MuleMessage response = responseEvent.getMessage();

    assertThat(response.getPayload(), instanceOf(Object[].class));
    final Object[] arrayPayload = (Object[]) response.getPayload();
    assertThat(arrayPayload.length, equalTo(1));
    assertThat(arrayPayload[0], equalTo(new ContactDetails("work", "2-222-222", "2@2222.com")));
  }
}
