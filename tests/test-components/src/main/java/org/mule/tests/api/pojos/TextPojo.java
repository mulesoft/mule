/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.api.pojos;

import org.mule.runtime.extension.api.annotation.dsl.xml.TypeDsl;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Text;

@TypeDsl(allowTopLevelDefinition = true)
public class TextPojo {

  @Parameter
  @Text
  private String text;

  public TextPojo() {}

  public TextPojo(String text) {
    this.text = text;
  }

  public void setSomeParameter(String text) {
    this.text = text;
  }

  public String getSomeParameter() {
    return text;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TextPojo that = (TextPojo) o;

    return text != null ? text.equals(that.text) : that.text == null;

  }

  @Override
  public int hashCode() {
    return text != null ? text.hashCode() : 0;
  }
}
