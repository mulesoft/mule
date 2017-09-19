/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain.entity.multipart;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;
import org.mule.runtime.http.api.domain.entity.HttpEntity;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(HTTP_SERVICE)
@Story("Entities")
public class MultipartHttpEntityTestCase {

  HttpPart part1 = mock(HttpPart.class);
  HttpPart part2 = mock(HttpPart.class);

  private HttpEntity entity = new MultipartHttpEntity(Arrays.asList(part1, part2));

  @Test
  public void composed() {
    assertThat(entity.isComposed(), is(true));
  }

  @Test
  public void nonStreaming() {
    assertThat(entity.isStreaming(), is(false));
  }

  @Test
  public void doesNotProvideArray() throws IOException {
    assertThat(entity.getBytes(), is(nullValue()));
  }

  @Test
  public void doesNotProvideStream() throws IOException {
    assertThat(entity.getContent(), is(nullValue()));
  }

  @Test
  public void hasParts() throws IOException {
    Collection<HttpPart> parts = entity.getParts();
    assertThat(parts, hasSize(2));
    assertThat(parts, contains(part1, part2));
  }

  @Test
  public void hasNoSize() {
    assertThat(entity.getLength().isPresent(), is(false));
  }

}
