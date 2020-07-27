package org.mule.tooling.extensions.metadata.internal.value;

import static java.util.Collections.singleton;
import static org.mule.runtime.extension.api.values.ValueBuilder.newValue;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.tooling.extensions.metadata.internal.connection.TstExtensionClient;

import java.util.Set;

public class ConfigLessNoActingParamVP implements ValueProvider {

  @Connection
  private TstExtensionClient client;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return singleton(newValue(client.getName()).build());
  }
}
