/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.message;

import org.mule.runtime.api.message.Attributes;

import java.io.ObjectStreamException;

/**
 * Default implementation of {@link Attributes} to be used when no other connector specific attributes instance is set.
 * 
 * @since 4.0
 */
public final class NullAttributes implements Attributes {

  public static NullAttributes NULL_ATTRIBUTES = new NullAttributes();

  private NullAttributes() {
    // Nothing to do
  }

  private static final long serialVersionUID = 1201393762712713465L;

  private Object readResolve() throws ObjectStreamException {
    return NULL_ATTRIBUTES;
  }

  @Override
  public String toString() {
    return "{NullAttributes}";
  }
}
