/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.model;

import static java.lang.String.format;

public class SimpleKnockeableDoor {

  private KnockeableDoor delegate;

  public SimpleKnockeableDoor(KnockeableDoor delegate) {
    this.delegate = delegate;
  }

  public String getSimpleName() {
    return format("%s @ %s", delegate.getVictim(), delegate.getAddress());
  }

}
