package org.mule.tooling.extensions.metadata.internal.value;

import static java.util.Collections.singleton;
import static org.mule.runtime.extension.api.values.ValueBuilder.newValue;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.tooling.extensions.metadata.api.parameters.ActingParameter;

import java.util.Set;

public class ComplexActingParameterVP implements ValueProvider {

  @Parameter
  private ActingParameter actingParameter;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    final String innerValueId = actingParameter.getInnerActingParameter().getStringParam();
    return singleton(newValue(innerValueId).build());
  }
}
