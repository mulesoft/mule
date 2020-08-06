package org.mule.tooling.extensions.metadata.api.parameters;

import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

public class ActingParameterGroup {

  @Parameter
  private String stringParam;

  @Parameter
  private int intParam;

  @Parameter
  private List<String> listParams;

  public String getStringParam() {
    return stringParam;
  }

  public int getIntParam() {
    return intParam;
  }

  public List<String> getListParams() {
    return listParams;
  }
}
