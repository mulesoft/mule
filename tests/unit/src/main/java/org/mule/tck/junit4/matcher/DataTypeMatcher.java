/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.matcher;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;

import java.nio.charset.Charset;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class DataTypeMatcher extends TypeSafeMatcher<DataType> {

  private final Class type;
  private final MediaType mimeType;
  private final Charset encoding;

  public DataTypeMatcher(Class type, MediaType mimeType, Charset encoding) {
    this.type = type;
    this.mimeType = mimeType;
    this.encoding = encoding;
  }

  @Override
  protected boolean matchesSafely(DataType dataType) {
    boolean sameType = type == null ? dataType.getType() == null : type.equals(dataType.getType());
    boolean sameEncoding = encoding == null ? !dataType.getMediaType().getCharset().isPresent()
        : encoding.equals(dataType.getMediaType().getCharset().get());
    boolean sameMimeType = mimeType == null ? dataType.getMediaType() == null : mimeType.matches(dataType.getMediaType());

    return sameType && sameEncoding && sameMimeType;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a dataType with type = " + type.getName() + ", mimeType= " + mimeType + ", encoding=" + encoding);
  }

  public static Matcher<DataType> like(Class type, MediaType mimeType, Charset encoding) {
    return new DataTypeMatcher(type, mimeType, encoding);
  }

  public static Matcher<DataType> like(DataType dataType) {
    return new DataTypeMatcher(dataType.getType(), dataType.getMediaType(), dataType.getMediaType().getCharset().orElse(null));
  }
}
