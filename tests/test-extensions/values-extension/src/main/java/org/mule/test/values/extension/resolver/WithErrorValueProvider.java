package org.mule.test.values.extension.resolver;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.Set;

public class WithErrorValueProvider implements ValueProvider {

  public static final String ERROR_MESSAGE = "Error!!!";

  @Parameter
  private String errorCode;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    throw new ValueResolvingException(ERROR_MESSAGE, errorCode);
  }
}
