package org.mule.tooling.extensions.metadata.api.parameters;

import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;
import java.util.Map;

public class InnerPojo {

  @Parameter
  private int intParam;

  @Parameter
  private String stringParam;

  @Parameter
  private List<String> simpleListParam;

  @Parameter
  private Map<String, String> simpleMapParam;

  public int getIntParam() {
    return intParam;
  }

  public String getStringParam() {
    return stringParam;
  }

  public List<String> getSimpleListParam() {
    return simpleListParam;
  }

  public Map<String, String> getSimpleMapParam() {
    return simpleMapParam;
  }

}
