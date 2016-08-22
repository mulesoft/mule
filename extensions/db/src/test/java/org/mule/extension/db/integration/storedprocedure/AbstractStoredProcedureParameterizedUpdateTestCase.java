/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.storedprocedure;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.DerbyTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public abstract class AbstractStoredProcedureParameterizedUpdateTestCase extends AbstractDbIntegrationTestCase {

  public AbstractStoredProcedureParameterizedUpdateTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Test
  public void update() throws Exception {
    Map<String, Object> payload = runProcedure("update", "foo");

    assertThat(payload.size(), is(1));
    int expectedUpdateCount = testDatabase instanceof DerbyTestDatabase ? 0 : 1;
    assertThat(payload.get("updateCount1"), equalTo(expectedUpdateCount));

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "foo"), new Field("POSITION", 4)));
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureParameterizedUpdateTestType1(getDefaultDataSource());
  }
}
