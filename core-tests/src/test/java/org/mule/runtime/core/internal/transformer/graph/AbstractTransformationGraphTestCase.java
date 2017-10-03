/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.tck.junit4.AbstractMuleTestCase;


public class AbstractTransformationGraphTestCase extends AbstractMuleTestCase {

  private static final MediaType UTF_8_MEDIA_TYPE = MediaType.create("media", "type", UTF_8);
  private static final MediaType UTF_16_MEDIA_TYPE = MediaType.create("media", "type", UTF_16);


  protected static final DataType XML_DATA_TYPE = DataType.builder().type(XML_CLASS.class).build();
  protected static final DataType JSON_DATA_TYPE = DataType.builder().type(JSON_CLASS.class).build();
  protected static final DataType INPUT_STREAM_DATA_TYPE = DataType.builder().type(INPUT_STREAM_CLASS.class).build();
  protected static final DataType STRING_DATA_TYPE = DataType.builder().type(STRING_CLASS.class).build();

  protected static final DataType UTF_8_DATA_TYPE = DataType.builder().mediaType(UTF_8_MEDIA_TYPE).build();
  protected static final DataType UTF_16_DATA_TYPE = DataType.builder().mediaType(UTF_16_MEDIA_TYPE).build();

  protected static final DataType UNUSED_DATA_TYPE = null;


  private static class XML_CLASS {

  }

  private static class JSON_CLASS {

  }

  private static class INPUT_STREAM_CLASS {

  }

  private static class STRING_CLASS {

  }

}
