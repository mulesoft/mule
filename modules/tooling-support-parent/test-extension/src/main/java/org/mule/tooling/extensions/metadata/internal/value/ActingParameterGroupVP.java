package org.mule.tooling.extensions.metadata.internal.value;

import static org.mule.runtime.extension.api.values.ValueBuilder.newValue;
import static java.util.Collections.singleton;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.List;
import java.util.Set;

public class ActingParameterGroupVP implements ValueProvider {

  @Parameter
  private String stringParam;

  @Parameter
  private int intParam;

  @Parameter
  private List<String> listParams;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    final StringBuilder sb = new StringBuilder();
    sb.append(stringParam).append("-").append(intParam);
    listParams.forEach(p -> sb.append("-").append(p));
    return singleton(newValue(sb.toString()).build());
  }
}
