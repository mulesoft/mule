/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.model;

public class Relic {

  private Object description;

  public Relic(Object description) {
    this.description = description;
  }

  public Relic() {}

  public Object getDescription() {
    return description;
  }

  public void setDescription(Object description) {
    this.description = description;
  }
}
