/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api.pojos;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@TypeDsl(allowTopLevelDefinition = true)
public class ElementWithAttributeAndChild {

  @Parameter
  @Optional
  private MyPojo myPojo = new MyPojo("jose");

  public MyPojo getMyPojo() {
    return myPojo;
  }

  public void setMyPojo(MyPojo myPojo) {
    this.myPojo = myPojo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElementWithAttributeAndChild that = (ElementWithAttributeAndChild) o;
    return myPojo != null ? myPojo.equals(that.myPojo) : that.myPojo == null;

  }

  @Override
  public int hashCode() {
    return myPojo != null ? myPojo.hashCode() : 0;
  }

}
