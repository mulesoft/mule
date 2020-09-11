package org.mule.tooling.extensions.metadata.api.parameters;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

public class ActingParameterGroupWithAlias {

  @Parameter
  @Optional(defaultValue = "defaultStringValue")
  private String stringParam;

  @Parameter
  @Alias("integerParam")
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
