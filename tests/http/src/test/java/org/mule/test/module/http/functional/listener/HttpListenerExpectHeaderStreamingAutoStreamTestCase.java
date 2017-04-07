/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.listener;

import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;

import org.mule.test.runner.RunnerDelegateTo;

import org.junit.runners.Parameterized;
import ru.yandex.qatools.allure.annotations.Features;

@RunnerDelegateTo(Parameterized.class)
@Features(HTTP_EXTENSION)
public class HttpListenerExpectHeaderStreamingAutoStreamTestCase extends HttpListenerExpectHeaderStreamingAlwaysTestCase {

  @Override
  protected String getConfigFile() {
    return "http-listener-expect-header-streaming-auto-stream-config.xml";
  }

  public HttpListenerExpectHeaderStreamingAutoStreamTestCase(String persistentConnections) {
    super(persistentConnections);
  }

}

