/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.metadata.extension.CustomStaticMetadataOperations.CSV_VALUE;
import static org.mule.test.metadata.extension.CustomStaticMetadataOperations.JSON_VALUE;
import static org.mule.test.metadata.extension.CustomStaticMetadataOperations.XML_VALUE;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.test.metadata.extension.CustomStaticMetadataSource;

import java.io.IOException;

import jakarta.inject.Inject;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

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
        JsonParser parser = new JsonParser();
        JsonElement payloadTree = parser.parse(CustomStaticMetadataSource.onErrorResult);
        JsonElement expectedTree = parser.parse(JSON_VALUE);
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

  @Test
  public void customIntersectionTypeInput() throws Exception {
    String payload = (String) flowRunner("custom-intersection-type-input").run().getMessage().getPayload().getValue();
    assertEqualJsons(payload, JSON_VALUE);
  }

  @Test
  public void customIntersectionTypeOutput() throws Exception {
    String payload = (String) flowRunner("custom-intersection-type-output").run().getMessage().getPayload().getValue();
    assertEqualJsons(payload, JSON_VALUE);
  }

  private void assertEqualJsons(String payload, String expected) throws IOException {
    JsonParser parser = new JsonParser();
    JsonElement payloadTree = parser.parse(payload);
    JsonElement expectedTree = parser.parse(expected);
    assertThat(payloadTree, is(expectedTree));
  }
}
