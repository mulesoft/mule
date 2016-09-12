/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration.select;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import static org.mule.extension.db.integration.TestRecordUtil.getAllPlanetRecords;
import static org.mule.extension.db.integration.TestRecordUtil.getEarthRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getMarsRecord;
import static org.mule.extension.db.integration.TestRecordUtil.getVenusRecord;
import static org.mule.extension.db.integration.model.Planet.MARS;

import org.mule.extension.db.integration.AbstractDbIntegrationTestCase;
import org.mule.extension.db.integration.TestDbConfig;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Planet;
import org.mule.extension.db.integration.model.Record;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.message.MuleEvent;

import java.util.Iterator;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runners.Parameterized;

public class SelectTestCase extends AbstractDbIntegrationTestCase {

  public SelectTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Parameterized.Parameters
  public static List<Object[]> parameters() {
    return TestDbConfig.getResources();
  }

  @Override
  protected String[] getFlowConfigurationResources() {
    return new String[] {"integration/select/select-config.xml"};
  }

  @Test
  public void select() throws Exception {
    Message response = flowRunner("select").run().getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Test
  public void fixedParam() throws Exception {
    Message response = flowRunner("fixedParam").run().getMessage();
    assertMessageContains(response, getMarsRecord());
  }

  @Test
  public void expressionAndFixedParamMixed() throws Exception {
    Message response = flowRunner("expressionAndFixedParamMixed").run().getMessage();
    assertMessageContains(response, getEarthRecord());
  }

  @Test
  public void dynamicQuery() throws Exception {
    Message response = flowRunner("dynamicQuery").run().getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Test
  public void maxRows() throws Exception {
    Message response = flowRunner("selectMaxRows").run().getMessage();
    assertMessageContains(response, getVenusRecord(), getEarthRecord());
  }

  @Test
  public void limitsStreamedRows() throws Exception {
    Message response = flowRunner("selectMaxStreamedRows").run().getMessage();
    assertMessageContains(response, getVenusRecord(), getEarthRecord());
  }

  @Test
  public void namedParameter() throws Exception {
    Message response = flowRunner("selectParameterizedQuery").withPayload(MARS.getName()).run().getMessage();
    assertMessageContains(response, getMarsRecord());
  }

  @Test
  public void chunksStreamedRecords() throws Exception {
    Message response = flowRunner("selectStreamingChunks").run().getMessage();

    List<Planet> chunks = (List<Planet>) response.getPayload().getValue();
    assertThat(chunks, hasSize(2));
    assertThat(chunks.get(0), is(instanceOf(List.class)));
    assertRecords(chunks.get(0), getVenusRecord(), getEarthRecord());
    assertThat(chunks.get(1), is(instanceOf(List.class)));
    assertRecords(chunks.get(1), getMarsRecord());
  }

  @Test
  public void streamsRecords() throws Exception {
    MuleEvent event = flowRunner("selectStreaming").run();
    Message response = event.getMessage();

    assertThat(response.getPayload().getValue(), CoreMatchers.is(instanceOf(Iterator.class)));
    assertRecords(event.getVariable("records").getValue(), getAllPlanetRecords());
  }

  @Test
  public void returnsAliasInResultSet() throws Exception {
    final String nameFieldAlias = "PLANETNAME";

    Message response = flowRunner("usesAlias").run().getMessage();
    assertMessageContains(response, new Record[] {
        new Record(new Field(nameFieldAlias, Planet.VENUS.getName())),
        new Record(new Field(nameFieldAlias, Planet.EARTH.getName())),
        new Record(new Field(nameFieldAlias, Planet.MARS.getName())),
    });
  }

}
