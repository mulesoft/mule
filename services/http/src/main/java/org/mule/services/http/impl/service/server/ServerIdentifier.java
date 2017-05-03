/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.services.http.impl.service.server;

/**
 * Allows to identify a server by it's creation context and name.
 *
 * @since 4.0
 */
public class ServerIdentifier {

  private final String context;
  private final String name;

  public ServerIdentifier(String context, String name) {
    this.context = context;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ServerIdentifier that = (ServerIdentifier) o;

    if (!context.equals(that.context)) {
      return false;
    }
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    int result = context.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }
}
