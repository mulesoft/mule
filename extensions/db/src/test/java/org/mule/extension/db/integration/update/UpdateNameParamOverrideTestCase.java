/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.integration.update;

import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestDbConfig.getResources;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;

import org.mule.extension.db.api.StatementResult;
import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.Message;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized;

public class UpdateNameParamOverrideTestCase extends AbstractDbIntegrationTestCase {

  public UpdateNameParamOverrideTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/update/update-name-param-override-config.xml"};
  }

  @Test
  public void usesDefaultParams() throws Exception {
    Message response = flowRunner("defaultParams").run().getMessage();

    assertAffectedRows((StatementResult) response.getPayload().getValue(), 1);
    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

  @Test
  public void usesOverriddenParams() throws Exception {
    Message response = flowRunner("overriddenParams").run().getMessage();

    assertAffectedRows((StatementResult) response.getPayload().getValue(), 1);
    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=2", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 2)));
  }

  public void usesInlineOverriddenParams() throws Exception {
    Message response = flowRunner("inlineOverriddenParams").run().getMessage();

    assertAffectedRows((StatementResult) response.getPayload().getValue(), 1);
    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=3", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 3)));
  }

  @Test
  public void usesParamsInInlineQuery() throws Exception {
    Message response = flowRunner("inlineQuery").run().getMessage();

    assertAffectedRows((StatementResult) response.getPayload().getValue(), 1);
    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=4", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 4)));
  }

  @Test
  public void usesExpressionParam() throws Exception {
    Message response = flowRunner("expressionParam").withFlowVariable("type", 3).run().getMessage();
    assertAffectedRows((StatementResult) response.getPayload().getValue(), 1);

    List<Map<String, String>> result = selectData("select * from PLANET where POSITION=3", getDefaultDataSource());
    assertRecords(result, new Record(new Field("NAME", "Mercury"), new Field("POSITION", 3)));
  }
}
