/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.metadata.extension.CustomStaticMetadataOperations.CSV_VALUE;
import static org.mule.test.metadata.extension.CustomStaticMetadataOperations.JSON_VALUE;
import static org.mule.test.metadata.extension.CustomStaticMetadataOperations.XML_VALUE;
import static org.mule.test.module.extension.source.HeisenbergMessageSourceTestCase.POLL_DELAY_MILLIS;
import static org.mule.test.module.extension.source.HeisenbergMessageSourceTestCase.TIMEOUT_MILLIS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.test.heisenberg.extension.HeisenbergSource;
import org.mule.test.metadata.extension.CustomStaticMetadataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CustomStaticMetadataOperationExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  @Inject
  public Flow onSuccessCustomType;

  @Inject
  public Flow onErrorCustomType;

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"metadata-static.xml"};
  }

  @Test
  public void xmlOutput() throws Exception {
    Object payload = flowRunner("output").keepStreamsOpen().run().getMessage().getPayload().getValue();
    assertThat(IOUtils.toString(((CursorStreamProvider) payload).openCursor()), is(XML_VALUE));
  }

  @Test
  public void xmlInput() throws Exception {
    Object payload = flowRunner("input").withPayload(XML_VALUE).run().getMessage().getPayload().getValue();
    assertThat(payload, is(XML_VALUE));
  }

  @Test
  public void jsonInputToMap() throws Exception {
    Object payload = flowRunner("jsonInputToMap").keepStreamsOpen().run().getMessage().getPayload().getValue();
    assertThat(payload, is(12));
  }

  @Test
  public void jsonInputToStream() throws Exception {
    String payload = (String) flowRunner("jsonInputToStream").keepStreamsOpen().run().getMessage().getPayload().getValue();
    assertEqualJsons(payload, JSON_VALUE);
  }

  @Test
  public void jsonOutput() throws Exception {
    Object payload = flowRunner("jsonOutput").keepStreamsOpen().run().getMessage().getPayload().getValue();
    assertEqualJsons(IOUtils.toString(((CursorStreamProvider) payload).openCursor()), JSON_VALUE);
  }

  @Test
  public void customOutput() throws Exception {
    Object payload = flowRunner("custom-output").run().getMessage().getPayload().getValue();
    assertThat(payload, is(CSV_VALUE));
  }

  @Test
  public void onErrorCustomType() throws MuleException {
    onErrorCustomType.start();
    probe(5000, 20, () -> {
      if (CustomStaticMetadataSource.onErrorResult != null) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode payloadTree = mapper.readTree(CustomStaticMetadataSource.onErrorResult);
        JsonNode expectedTree = mapper.readTree(JSON_VALUE);
        return payloadTree.equals(expectedTree);
      } else {
        return false;
      }
    }, () -> "OnError was not called");
  }

  @Test
  public void onSuccessCustomType() throws MuleException {
    String expected = "<?xml version='1.0' encoding='UTF-8'?>\n"
        + "<person>\n"
        + "  <age>12</age>\n"
        + "</person>";
    onSuccessCustomType.start();
    probe(5000, 20, () -> {
      if (CustomStaticMetadataSource.onSuccessResult != null) {
        if (CustomStaticMetadataSource.onSuccessResult.equals(expected)) {
          return true;
        } else {
          throw new RuntimeException("That Xml was not expected");
        }
      } else {
        return false;
      }
    }, () -> "OnSuccess was not called");
  }

  @Test
  public void customInput() throws Exception {
    String payload = (String) flowRunner("custom-input").run().getMessage().getPayload().getValue();
    assertEqualJsons(payload, JSON_VALUE);
  }

  private void assertEqualJsons(String payload, String expected) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode payloadTree = mapper.readTree(payload);
    JsonNode expectedTree = mapper.readTree(expected);
    assertThat(payloadTree, is(expectedTree));
  }
}
