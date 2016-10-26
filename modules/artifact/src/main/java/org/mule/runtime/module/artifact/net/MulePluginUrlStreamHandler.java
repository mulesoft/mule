/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.net;

import org.mule.runtime.core.util.MuleUrlStreamHandlerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * Registers a handler for the Mule Plugin protocol
 *
 * @since 4.0
 */
public class MulePluginUrlStreamHandler extends URLStreamHandler {

  public final static String PROTOCOL = "muleplugin";

  public static void register() {
    MuleUrlStreamHandlerFactory.registerHandler(PROTOCOL, new MulePluginUrlStreamHandler());
  }

  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    return new MulePluginURLConnection(url);
  }
}
