/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Registers a handler for the Mule Artifact protocol, so that every time an URL has {@link #PROTOCOL} it will be
 * handled by {@link MuleArtifactUrlConnection}.
 *
 * @since 4.0
 */
public class MuleArtifactUrlStreamHandler extends URLStreamHandler {

  /**
   * Mule Protocol that will be used to reference artifacts.
   */
  public final static String PROTOCOL = "muleartifact";

  /**
   * Registers the Mule Artifact protocol {@link #PROTOCOL} into the {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)}
   * through the {@link MuleUrlStreamHandlerFactory}.
   */
  public static void register() {
    MuleUrlStreamHandlerFactory.registerHandler(PROTOCOL, new MuleArtifactUrlStreamHandler());
  }

  /**
   * Opens a connection to the object referenced by the {@code url} argument if the protocol of it it's {@link #PROTOCOL}.
   *
   * @param url that represents a Mule Artifact to connect with.
   * @return a {@link URLConnection} object for the {@code url}.
   * @throws IOException if an I/O error occurs while opening the {@link URL#openConnection()}.
   */
  @Override
  protected URLConnection openConnection(URL url) throws IOException {
    return new MuleArtifactUrlConnection(url);
  }
}
