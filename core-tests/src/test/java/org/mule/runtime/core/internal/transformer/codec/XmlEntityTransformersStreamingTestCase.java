/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.codec;

import java.io.ByteArrayInputStream;

public class XmlEntityTransformersStreamingTestCase extends XMLEntityTransformersTestCase {

  @Override
  public Object getTestData() {
    String string = (String) super.getTestData();
    return new ByteArrayInputStream(string.getBytes());
  }

  @Override
  public Object getResultData() {
    String string = (String) super.getResultData();
    return new ByteArrayInputStream(string.getBytes());
  }

}
