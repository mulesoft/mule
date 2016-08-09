/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.functional.FlowAssert;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.util.IOUtils;
import org.mule.test.module.http.functional.AbstractHttpTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;

public class HttpRequestAttachmentsTestCase extends AbstractHttpTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "http-request-attachment-config.xml";
  }

  /**
   * "Unsupported content type" means one that is not out of the box supported by javax.activation.
   */
  @Test
  public void inputStreamAttachmentWithUnsupportedContentType() throws Exception {
    final MuleEvent result = runFlow("attachmentFromBytes");
    assertThat(IOUtils.toString((InputStream) result.getMessage().getPayload()), is("OK"));
    FlowAssert.verify("reqWithAttachment");
  }

  /**
   * "Unsupported content type" means one that is not out of the box supported by javax.activation.
   */
  @Test
  public void byteArrayAttachmentWithUnsupportedContentType() throws Exception {
    final MuleEvent result = runFlow("attachmentFromStream");
    assertThat(IOUtils.toString((InputStream) result.getMessage().getPayload()), is("OK"));
    FlowAssert.verify("reqWithAttachment");
  }
}
