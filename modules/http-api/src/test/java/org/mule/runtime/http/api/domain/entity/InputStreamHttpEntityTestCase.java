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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import sun.misc.IOUtils;

@Feature(HTTP_SERVICE)
@Story("Entities")
public class InputStreamHttpEntityTestCase {

  private InputStream stream = new InputStream() {

    private boolean once = false;

    @Override
    public int read() throws IOException {
      if (once) {
        return -1;
      } else {
        once = true;
        return '+';
      }
    }
  };

  private HttpEntity entity = new InputStreamHttpEntity(stream);

  @Test
  public void nonComposed() {
    assertThat(entity.isComposed(), is(false));
  }

  @Test
  public void streaming() {
    assertThat(entity.isStreaming(), is(true));
  }

  @Test
  public void providesArrayOnce() throws IOException {
    assertThat(entity.getBytes().length, is(1));
    assertThat(entity.getBytes().length, is(0));
  }

  @Test
  public void providesStreamOnce() throws IOException {
    InputStream content = entity.getContent();
    assertThat(content, equalTo(stream));

    assertThat(IOUtils.readFully(content, -1, true), equalTo("+".getBytes()));
    assertThat(IOUtils.readFully(entity.getContent(), -1, true).length, is(0));
  }

  @Test
  public void hasNoParts() throws IOException {
    assertThat(entity.getParts(), is(empty()));
  }

  @Test
  public void hasNoSizeUnlessSpecified() {
    assertThat(entity.getLength().isPresent(), is(false));
    HttpEntity specifiedEntity = new InputStreamHttpEntity(new ByteArrayInputStream("TEST".getBytes()), 4L);
    assertThat(specifiedEntity.getLength().get(), is(4L));
  }

}
