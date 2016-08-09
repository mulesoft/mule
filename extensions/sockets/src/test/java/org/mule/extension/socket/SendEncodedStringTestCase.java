/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.socket;

import static java.nio.charset.Charset.availableCharsets;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.util.IOUtils;

import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

public class SendEncodedStringTestCase extends SocketExtensionTestCase {

  private static final String WEIRD_CHAR_MESSAGE = "This is a messag\u00ea with weird chars \u00f1.";

  @Override
  protected String getConfigFile() {
    return "send-encoded-string-config.xml";
  }

  @Test
  public void sendEncodedString() throws Exception {
    final String defaultEncoding = muleContext.getConfiguration().getDefaultEncoding();
    assertThat(defaultEncoding, is(notNullValue()));

    final String customEncoding =
        availableCharsets().keySet().stream().filter(encoding -> !encoding.equals(defaultEncoding)).findFirst().orElse(null);

    assertThat(customEncoding, is(notNullValue()));

    flowRunner("tcp-send").withFlowVariable("encoding", customEncoding).withPayload(WEIRD_CHAR_MESSAGE).run();

    MuleMessage message = receiveConnection();
    byte[] byteArray = IOUtils.toByteArray((InputStream) message.getPayload());
    assertThat(Arrays.equals(byteArray, WEIRD_CHAR_MESSAGE.getBytes(customEncoding)), is(true));
  }
}
