/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

public class SubTypesConnectorConnection {

  private ParentShape shape;
  private Door door;


  public SubTypesConnectorConnection(ParentShape shape, Door door) {
    this.shape = shape;
    this.door = door;
  }

  public ParentShape getShape() {
    return shape;
  }

  public Door getDoor() {
    return door;
  }
}
