/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
