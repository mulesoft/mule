/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;

import java.io.IOException;

import org.junit.Test;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(HTTP_SERVICE)
@Story("Entities")
public class EmptyHttpEntityTestCase {

  private HttpEntity entity = new EmptyHttpEntity();

  @Test
  public void nonComposed() {
    assertThat(entity.isComposed(), is(false));
  }

  @Test
  public void nonStreaming() {
    assertThat(entity.isStreaming(), is(false));
  }

  @Test
  public void providesEmptyArray() throws IOException {
    assertThat(entity.getBytes().length, is(0));
  }

  @Test
  public void providesNewEmptyStream() throws IOException {
    assertThat(entity.getContent().read(), is(-1));
    assertThat(entity.getContent().read(), is(-1));
  }

  @Test
  public void hasNoParts() throws IOException {
    assertThat(entity.getParts(), is(empty()));
  }

  @Test
  public void hasZeroSize() {
    assertThat(entity.getLength().get(), is(0L));
  }

}
