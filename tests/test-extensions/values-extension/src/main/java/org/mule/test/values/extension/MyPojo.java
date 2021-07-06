/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

public class MyPojo {

  private String pojoId;
  private String pojoName;
  private int pojoNumber;
  private boolean pojoBoolean;

  public MyPojo() {}

  public MyPojo(String pojoId, String pojoName, int pojoNumber, boolean pojoBoolean) {
    this.pojoId = pojoId;
    this.pojoName = pojoName;
    this.pojoNumber = pojoNumber;
    this.pojoBoolean = pojoBoolean;
  }

  public String getPojoId() {
    return pojoId;
  }

  public void setPojoId(String pojoId) {
    this.pojoId = pojoId;
  }

  public String getPojoName() {
    return pojoName;
  }

  public void setPojoName(String pojoName) {
    this.pojoName = pojoName;
  }

  public int getPojoNumber() {
    return pojoNumber;
  }

  public void setPojoNumber(int pojoNumber) {
    this.pojoNumber = pojoNumber;
  }

  public boolean isPojoBoolean() {
    return pojoBoolean;
  }

  public void setPojoBoolean(boolean pojoBoolean) {
    this.pojoBoolean = pojoBoolean;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    MyPojo myPojo = (MyPojo) o;

    if (pojoNumber != myPojo.pojoNumber)
      return false;
    if (pojoBoolean != myPojo.pojoBoolean)
      return false;
    if (pojoId != null ? !pojoId.equals(myPojo.pojoId) : myPojo.pojoId != null)
      return false;
    return pojoName != null ? pojoName.equals(myPojo.pojoName) : myPojo.pojoName == null;
  }

  @Override
  public int hashCode() {
    int result = pojoId != null ? pojoId.hashCode() : 0;
    result = 31 * result + (pojoName != null ? pojoName.hashCode() : 0);
    result = 31 * result + pojoNumber;
    result = 31 * result + (pojoBoolean ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    return "MyPojo{" +
        "pojoId='" + pojoId + '\'' +
        ", pojoName='" + pojoName + '\'' +
        ", pojoNumber=" + pojoNumber +
        ", pojoBoolean=" + pojoBoolean +
        '}';
  }
}
