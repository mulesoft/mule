package org.mule.tooling.extensions.metadata.api.parameters;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class InnerActingParameter {

  @Parameter
  private String stringParam;

  public String getStringParam() {
    return stringParam;
  }

}
