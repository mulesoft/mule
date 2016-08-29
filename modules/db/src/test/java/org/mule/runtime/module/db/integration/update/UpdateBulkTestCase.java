/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.update;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.module.db.integration.DbTestUtil.selectData;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertRecords;
import static org.mule.runtime.module.db.integration.model.Planet.EARTH;
import static org.mule.runtime.module.db.integration.model.Planet.MARS;
import static org.mule.runtime.module.db.integration.model.Planet.VENUS;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.TestDbConfig;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;
import org.mule.runtime.module.db.integration.model.Field;
import org.mule.runtime.module.db.integration.model.Record;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateBulkTestCase extends AbstractDbIntegrationTestCase {

  public UpdateBulkTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-bulk-config.xml"};
  }

  @Test
  public void updatesInBulkModeWithCollection() throws Exception {
    final MuleEvent responseEvent = flowRunner("updateBulk").withPayload(getPlanetNames()).run();

    final MuleMessage response = responseEvent.getMessage();
    assertBulkModeResult(response);
  }

  @Test
  public void updatesInBulkModeWithIterator() throws Exception {
    final MuleEvent responseEvent = flowRunner("updateBulk").withPayload(getPlanetNames().iterator()).run();

    final MuleMessage response = responseEvent.getMessage();
    assertBulkModeResult(response);
  }

  @Test
  public void updatesInBulkModeWithIterable() throws Exception {
    final List<String> planetNames = getPlanetNames();
    Iterable<String> iterable = new Iterable<String>() {

      @Override
      public Iterator<String> iterator() {
        return planetNames.iterator();
      }
    };

    final MuleEvent responseEvent = flowRunner("updateBulk").withPayload(iterable).run();

    final MuleMessage response = responseEvent.getMessage();
    assertBulkModeResult(response);
  }

  @Test(expected = MessagingException.class)
  public void requiresSplittableType() throws Exception {
    flowRunner("updateBulk").withPayload(TEST_MESSAGE).run();
  }

  private void assertBulkModeResult(MuleMessage response) throws SQLException {
    assertTrue(response.getPayload() instanceof int[]);
    int[] counters = (int[]) response.getPayload();
    assertThat(counters[0], anyOf(equalTo(1), equalTo(Statement.SUCCESS_NO_INFO)));
    assertThat(counters[1], anyOf(equalTo(1), equalTo(Statement.SUCCESS_NO_INFO)));
    assertThat(counters[2], anyOf(equalTo(1), equalTo(Statement.SUCCESS_NO_INFO)));

    List<Map<String, String>> result = selectData("select * from PLANET order by ID", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 2)),
                  new Record(new Field("NAME", "Mercury"), new Field("POSITION", 3)),
                  new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

  private List<String> getPlanetNames() {
    List<String> planetNames = new ArrayList<String>();
    planetNames.add(VENUS.getName());
    planetNames.add(MARS.getName());
    planetNames.add(EARTH.getName());
    return planetNames;
  }
}
