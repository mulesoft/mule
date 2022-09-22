package org.mule.test.oauth;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.subtypes.extension.ParentShape;

public class Rectangle extends ParentShape {

  @Parameter
  private Integer base;

  @Parameter
  private Integer height;

  public Integer getBase() {
    return base;
  }

  public Integer getHeight() {
    return height;
  }
}
