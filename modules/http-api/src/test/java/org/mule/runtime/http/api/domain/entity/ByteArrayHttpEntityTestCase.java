/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;

import java.io.IOException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import sun.misc.IOUtils;

@Feature(HTTP_SERVICE)
@Story("Entities")
public class ByteArrayHttpEntityTestCase {

  private byte[] content = "TEST".getBytes();
  private HttpEntity entity = new ByteArrayHttpEntity(content);

  @Test
  public void nonComposed() {
    assertThat(entity.isComposed(), is(false));
  }

  @Test
  public void nonStreaming() {
    assertThat(entity.isStreaming(), is(false));
  }

  @Test
  public void providesArray() throws IOException {
    assertThat(entity.getBytes(), equalTo(content));
  }

  @Test
  public void providesNewStream() throws IOException {
    assertThat(IOUtils.readFully(entity.getContent(), -1, true), equalTo(content));
    assertThat(IOUtils.readFully(entity.getContent(), -1, true), equalTo(content));
  }

  @Test
  public void hasNoParts() throws IOException {
    assertThat(entity.getParts(), is(empty()));
  }

  @Test
  public void providesSize() {
    assertThat(entity.getLength().get(), is((long) content.length));
  }

}
