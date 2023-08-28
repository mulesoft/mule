/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
