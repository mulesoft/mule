package org.mule.tooling.extensions.metadata.internal.value;

import static java.util.Collections.singleton;
import static org.mule.runtime.extension.api.values.ValueBuilder.newValue;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

public class ActingParameterVP implements ValueProvider {

  @Parameter
  private String actingParameter;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return singleton(newValue("WITH-ACTING-PARAMETER-" + actingParameter).build());
  }
}
