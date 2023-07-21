/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.spi.impl;

import org.foo.spi.SpiInterface;

public class SpiImplementation implements SpiInterface {

  public String value() {
    return "SpiImplementation";
  }
}
