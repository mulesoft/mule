/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

/**
 * Test an entry-point resolver used for multiple classes
 */
public class EntryPointResolverCacheTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/entry-point-resolver-cache-flow.xml";
  }

  @Test
  public void testCache() throws Exception {
    Event responseEvent =
        flowRunner("refServiceOne").withPayload("a request").withInboundProperty("method", "retrieveReferenceData").run();
    Message response = responseEvent.getMessage();
    Object payload = response.getPayload().getValue();

    assertThat(payload, instanceOf(String.class));
    assertThat(payload, is("ServiceOne"));

    response = flowRunner("refServiceTwo").withPayload("another request").withInboundProperty("method", "retrieveReferenceData")
        .run().getMessage();
    payload = response.getPayload().getValue();
    if ((payload == null) || (responseEvent.getError().isPresent())) {
      Throwable exception = responseEvent.getError().get().getCause();
      if (exception != null) {
        fail(exception.getMessage());
      } else {
        fail(responseEvent.getError().get().toString());
      }
    }
    assertThat(payload, instanceOf(String.class));
    assertThat(payload, is("ServiceTwo"));

  }

  public interface ReferenceDataService {

    String retrieveReferenceData(String refKey);
  }

  public static class RefDataServiceOne implements ReferenceDataService {

    @Override
    public String retrieveReferenceData(String refKey) {
      return "ServiceOne";
    }
  }

  public static class RefDataServiceTwo implements ReferenceDataService {

    @Override
    public String retrieveReferenceData(String refKey) {
      return "ServiceTwo";
    }
  }
}
