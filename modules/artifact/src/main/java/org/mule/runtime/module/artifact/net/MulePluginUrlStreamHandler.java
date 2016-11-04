/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.net;

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

  public static void register() { //TODO MULE-10873 talk to ESB guys, I need this class registered before any test or the "muleplugin" protocol will break everything
    MuleUrlStreamHandlerFactory.registerHandler(PROTOCOL, new MulePluginUrlStreamHandler());
  }

  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    return new MulePluginURLConnection(url);
  }
}
