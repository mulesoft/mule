/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader.net;

import org.mule.api.annotation.NoInstantiate;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

/**
 * Registers a handler for the Mule Artifact protocol, so that every time an URL has {@link #PROTOCOL} it will be handled by
 * {@link MuleArtifactUrlConnection}.
 *
 * @since 4.0
 */
@NoInstantiate
public final class MuleArtifactUrlStreamHandler extends URLStreamHandler {

  /**
   * Mule Protocol that will be used to reference artifacts.
   */
  public final static String PROTOCOL = "muleartifact";

  /**
   * Registers the Mule Artifact protocol {@link #PROTOCOL} into the
   * {@link URL#setURLStreamHandlerFactory(URLStreamHandlerFactory)} through the {@link MuleUrlStreamHandlerFactory}.
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
