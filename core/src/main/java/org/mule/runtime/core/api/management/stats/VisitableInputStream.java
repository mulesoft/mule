/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.management.stats;

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream that can be visit to decorate.
 *
 * @since 4.4, 4.3.1
 */
public class VisitableInputStream extends InputStream implements Visitable<InputStream> {

  private final InputStream delegate;

  public VisitableInputStream(InputStream delegate) {
    this.delegate = delegate;
  }

  @Override
  public InputStream accept(Visitor visitor) {
    return visitor.visitInputStream(this);
  }

  @Override
  public int read() throws IOException {
    return delegate.read();
  }

  @Override
  public InputStream getDelegate() {
    return delegate;
  }

}
