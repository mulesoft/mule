/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.http.multipart;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;

import org.junit.Rule;
import org.junit.Test;

public class HttpAttachmentDataTypeTestCase extends FunctionalTestCase {

  private static final List<String> contentTypes = new ArrayList();

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Override
  protected String getConfigFile() {
    return "http-attachment-datatype-config.xml";
  }

  @Test
  public void testContentType() throws Exception {
    runFlow("testFlow");

    assertThat(contentTypes.size(), equalTo(2));
    assertThat(contentTypes, containsInAnyOrder("text/html; charset=US-ASCII", "application/xml; charset=UTF-8"));
  }

  public static class AttachmentListener {

    public DataHandler process(DataHandler dataHandler) {
      contentTypes.add(dataHandler.getContentType());

      return dataHandler;
    }
  }
}
