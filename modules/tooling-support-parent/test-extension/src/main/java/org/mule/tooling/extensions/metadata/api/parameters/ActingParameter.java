package org.mule.tooling.extensions.metadata.api.parameters;

import org.mule.runtime.extension.api.annotation.param.Parameter;

public class ActingParameter {

  @Parameter
  private InnerActingParameter innerActingParameter;

  public InnerActingParameter getInnerActingParameter() {
    return innerActingParameter;
  }

}
