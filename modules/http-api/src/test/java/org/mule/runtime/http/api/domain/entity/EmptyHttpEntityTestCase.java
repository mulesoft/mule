/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.domain.entity;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;

import org.junit.Test;

import java.io.IOException;

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
    assertThat(entity.getBytesLength().getAsLong(), is(0L));
  }

}
