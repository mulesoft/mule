/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.streaming;

import org.junit.Test;

public class StreamingFromPojoTestCase extends AbstractStreamingExtensionTestCase {

  @Override
  protected String getConfigFile() {
    return "streaming/streaming-inside-pojo-config.xml";
  }

  @Test
  public void cursorComingFromProviderIsResetOnReconnection() throws Exception {
    flowRunner("streamingWithListOfPojos");
  }

}
