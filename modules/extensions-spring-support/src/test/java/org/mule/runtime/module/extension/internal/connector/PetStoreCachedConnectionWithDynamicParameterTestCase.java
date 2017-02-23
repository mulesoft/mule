/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.test.petstore.extension.PetStoreClient;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.Calendar;

import org.joda.time.format.ISODateTimeFormat;
import org.junit.Test;

public class PetStoreCachedConnectionWithDynamicParameterTestCase extends ExtensionFunctionalTestCase {

  public static final String FLOW_NAME = "getClient";
  public static final String USERNAME = "username";
  public static final String PASSWORD = "password";
  public static final String DATE = "date";

  @Override
  protected String getConfigFile() {
    return "petstore-dynamic-connection-cached-parameter.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnector.class};
  }

  @Test
  public void getDynamicConnectionParametersOnCachedConnection() throws Exception {
    PetStoreClient client = (PetStoreClient) flowRunner(FLOW_NAME)
        .withVariable(USERNAME, "john")
        .withVariable(PASSWORD, "doe")
        .withVariable(DATE,
                      ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime("2008-09-15T15:53:23+05:00").toDate())
        .run().getMessage().getPayload().getValue();

    assertThat(client.getUsername(), is("john"));
    assertThat(client.getPassword(), is("doe"));

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(client.getOpeningDate());
    assertEquals(calendar.get(Calendar.YEAR), 2008);
    assertEquals(calendar.get(Calendar.MONTH) + 1, 9);
    assertEquals(calendar.get(Calendar.DAY_OF_MONTH), 15);

    client = (PetStoreClient) flowRunner(FLOW_NAME)
        .withVariable(USERNAME, "john")
        .withVariable(PASSWORD, "doe")
        .withVariable(DATE, ISODateTimeFormat.dateTimeParser().withOffsetParsed().parseDateTime("2017-02-10").toDate())
        .run().getMessage().getPayload().getValue();

    assertThat(client.getUsername(), is("john"));
    assertThat(client.getPassword(), is("doe"));
    calendar.setTime(client.getOpeningDate());
    assertEquals(calendar.get(Calendar.YEAR), 2017);
    assertEquals(calendar.get(Calendar.MONTH) + 1, 2);
    assertEquals(calendar.get(Calendar.DAY_OF_MONTH), 10);
  }

}
