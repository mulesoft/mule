/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DataTypeUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void generatesContentTypeWithCharset() throws Exception {
    final DataType dataType = DataType.builder().type(Object.class).mediaType(APPLICATION_JSON).charset(UTF_8.name()).build();

    String contentType = dataType.getMediaType().toRfcString();
    assertThat(contentType, equalTo("application/json; charset=UTF-8"));
  }

  @Test
  public void generatesContentTypeWithoutCharset() throws Exception {
    DataType dataType = DataType.builder().type(Object.class).mediaType(MediaType.APPLICATION_JSON).build();

    String contentType = dataType.getMediaType().toRfcString();
    assertThat(contentType, equalTo("application/json"));
  }
}
