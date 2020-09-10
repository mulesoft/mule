package org.mule.tooling.extensions.metadata.internal.value;

import static java.util.Collections.singleton;
import static org.mule.runtime.extension.api.values.ValueBuilder.newValue;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.tooling.extensions.metadata.internal.config.SimpleConfiguration;

import java.util.Set;

public class ConnectionLessNoActingParamVP implements ValueProvider {

  @Config
  private SimpleConfiguration config;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return singleton(newValue(config.getActingParameter()).build());
  }
}
