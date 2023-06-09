/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.jar;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import sun.net.www.protocol.jar.Handler;

/**
 * Caching is disabled only for Java 8 and earlier
 */
public class NonCachingURLStreamHandlerFactory implements URLStreamHandlerFactory {

  private static class NonCachingJarResourceURLStreamHandler extends Handler {

    public NonCachingJarResourceURLStreamHandler() {
      super();
    }

    @Override
    protected java.net.URLConnection openConnection(URL u) throws IOException {
      JarURLConnection c = new sun.net.www.protocol.jar.JarURLConnection(u, this);
      c.setUseCaches(false);
      return c;
    }
  }

  @Override
  public URLStreamHandler createURLStreamHandler(String protocol) {
    return new NonCachingJarResourceURLStreamHandler();
  }
}


