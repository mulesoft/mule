/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.splash;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;

import org.mule.runtime.core.api.MuleContext;

public class ApplicationStartupSplashScreen extends SplashScreen {

  @Override
  protected void doHeader(MuleContext context) {
    header.add("Application: " + context.getConfiguration().getId());
    header.add(format("OS encoding: %s, Mule encoding: %s",
                      defaultCharset().name(),
                      context.getConfiguration().getDefaultEncoding()));
    header.add(" ");
  }
}
