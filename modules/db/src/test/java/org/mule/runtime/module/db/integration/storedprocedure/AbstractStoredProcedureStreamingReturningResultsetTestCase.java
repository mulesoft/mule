/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.db.integration.storedprocedure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.db.integration.TestRecordUtil.assertMessageContains;
import static org.mule.runtime.module.db.integration.TestRecordUtil.getAllPlanetRecords;
import static org.mule.runtime.module.db.integration.model.Planet.EARTH;
import static org.mule.runtime.module.db.integration.model.Planet.MARS;
import static org.mule.runtime.module.db.integration.model.Planet.VENUS;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.module.db.integration.AbstractDbIntegrationTestCase;
import org.mule.runtime.module.db.integration.matcher.ContainsMessage;
import org.mule.runtime.module.db.integration.model.AbstractTestDatabase;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.core.AllOf;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractStoredProcedureStreamingReturningResultsetTestCase extends AbstractDbIntegrationTestCase {

  public AbstractStoredProcedureStreamingReturningResultsetTestCase(String dataSourceConfigResource,
                                                                    AbstractTestDatabase testDatabase) {
    super(dataSourceConfigResource, testDatabase);
  }

  @Test
  public void testOneWay() throws Exception {
    flowRunner("messagePerRecordOneWay").withPayload(TEST_MESSAGE).asynchronously().run();

    MuleClient client = muleContext.getClient();
    List<MuleMessage> responses = new LinkedList<MuleMessage>();
    MuleMessage response = client.request("test://testOut", RECEIVE_TIMEOUT).getRight().get();
    responses.add(response);
    response = client.request("test://testOut", RECEIVE_TIMEOUT).getRight().get();
    responses.add(response);
    response = client.request("test://testOut", RECEIVE_TIMEOUT).getRight().get();
    responses.add(response);

    assertEquals(3, responses.size());
    assertThat(responses,
               AllOf.allOf(ContainsMessage.mapPayloadWith("NAME", MARS.getName()),
                           ContainsMessage.mapPayloadWith("NAME", EARTH.getName()),
                           ContainsMessage.mapPayloadWith("NAME", VENUS.getName())));
  }

  @Test
  public void testRequestResponse() throws Exception {
    final MuleEvent responseEvent = flowRunner("defaultQueryRequestResponse").withPayload(TEST_MESSAGE).run();

    final MuleMessage response = responseEvent.getMessage();
    assertMessageContains(response, getAllPlanetRecords());
  }

  @Before
  public void setupStoredProcedure() throws Exception {
    testDatabase.createStoredProcedureGetRecords(getDefaultDataSource());
  }
}
