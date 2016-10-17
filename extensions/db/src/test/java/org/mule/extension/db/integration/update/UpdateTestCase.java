/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.update;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mule.extension.db.integration.DbTestUtil.DbType.MYSQL;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;

import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.Message;

import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("DB Extension")
@Stories("Update Statement")
public class UpdateTestCase extends AbstractDbIntegrationTestCase {

  private static final String PLUTO = "Pluto";

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-config.xml"};
  }

  @Test
  @Description("This tests the MERGE statement. Is not executed with MySQL due that is not supported by the DB.")
  public void mergesTables() throws Exception {
    assumeThat(testDatabase.getDbType(), is(not(MYSQL)));
    assertMergeResult(flowRunner("merge").run().getMessage());
  }

  @Test
  public void truncateTable() throws Exception {
    flowRunner("truncateTable").run();
    List<Map<String, String>> result = selectData("select * from PLANET", getDefaultDataSource());
    assertThat(result.isEmpty(), is(true));
  }

  @Test
  public void update() throws Exception {
    Message response = flowRunner("update").run().getMessage();
    verifyUpdatedRecord((StatementResult) response.getPayload().getValue());
  }

  @Test
  public void updateDynamic() throws Exception {
    Message response = flowRunner("updateDynamic").run().getMessage();
    verifyUpdatedRecord((StatementResult) response.getPayload().getValue());
  }

  @Test
  public void updateParameterized() throws Exception {
    Message response = flowRunner("updateParameterized").withPayload(PLUTO).run().getMessage();
    assertAffectedRows((StatementResult) response.getPayload().getValue(), 1);
    assertPlanetRecordsFromQuery(PLUTO);
  }

  @Test
  @Description("This test tries to update the value of a Blob column from a byte[]. " +
      "This implies that DB connector will detect this type, and transform it from byte[] to Blob")
  public void updateBlob() throws Exception {
    byte[] picture = new byte[100];
    new Random().nextBytes(picture);

    Message response = flowRunner("updateBlob").withPayload(picture).run().getMessage();
    assertAffectedRows((StatementResult) response.getPayload().getValue(), 1);
    assertPlanetRecordsFromQuery("Mars");
  }

  @Test
  @Description("This test tries to update the value of a Blob column from an InputStream. " +
      "This implies that DB connector will detect this type, and transform it from InputStream to Blob")
  public void updateBlobWithStream() throws Exception {
    byte[] picture = new byte[100];
    new Random().nextBytes(picture);

    Message response = flowRunner("updateBlob").withPayload(new ByteArrayInputStream(picture)).run().getMessage();
    assertAffectedRows((StatementResult) response.getPayload().getValue(), 1);
    assertPlanetRecordsFromQuery("Mars");
  }

  private void assertMergeResult(Message response) throws SQLException {
    StatementResult result = (StatementResult) response.getPayload().getValue();
    assertThat(result.getAffectedRows(), is(3));

    List<Map<String, String>> data = selectData("select * from PLANET order by ID", getDefaultDataSource());
    assertRecords(data, createRecord(2), createRecord(3), createRecord(4));
  }

  private Record createRecord(int pos) {
    return new Record(new Field("NAME", "merged"), new Field("POSITION", pos));
  }

  private void verifyUpdatedRecord(StatementResult statementResult) throws SQLException {
    assertAffectedRows(statementResult, 1);
    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }
}
