package org.mule.tooling.extensions.metadata.internal.parameters;


import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;
import java.util.Map;

public class ComplexActingParameter {

  @Parameter
  private int intParam;

  @Parameter
  private String stringParam;

  @Parameter
  private InnerPojo innerPojoParam;

  @Parameter
  private List<String> simpleListParam;

  @Parameter
  private Map<String, String> simpleMapParam;

  @Parameter
  private List<InnerPojo> complexListParam;

  @Parameter
  private Map<String, InnerPojo> complexMapParam;

  public int getIntParam() {
    return intParam;
  }

  public String getStringParam() {
    return stringParam;
  }

  public List<String> getSimpleListParam() {
    return simpleListParam;
  }

  public InnerPojo getInnerPojoParam() {
    return innerPojoParam;
  }

  public Map<String, String> getSimpleMapParam() {
    return simpleMapParam;
  }

  public List<InnerPojo> getComplexListParam() {
    return complexListParam;
  }

  public Map<String, InnerPojo> getComplexMapParam() {
    return complexMapParam;
  }
}
