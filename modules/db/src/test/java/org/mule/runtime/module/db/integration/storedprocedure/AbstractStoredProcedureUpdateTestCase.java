/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.db.integration.DbTestUtil.selectData;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecords;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.DerbyTestDatabase;
import org.mule.runtime.module.db.integration.model.Field;
import org.mule.runtime.module.db.integration.model.Record;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public abstract class AbstractStoredProcedureUpdateTestCase extends AbstractDbIntegrationTestCase {

  public AbstractStoredProcedureUpdateTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Test
  public void testRequestResponse() throws Exception {
    final MuleEvent responseEvent = flowRunner("defaultQueryRequestResponse").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertThat(response.getPayload(), is(instanceOf(Map.class)));
    Map mapPayload = (Map) response.getPayload();
    int expectedUpdateCount = testDatabase instanceof DerbyTestDatabase ? 0 : 1;
    assertThat(mapPayload.get("updateCount1"), equalTo(expectedUpdateCount));

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureUpdateTestType1(getDefaultDataSource());
  }
}
