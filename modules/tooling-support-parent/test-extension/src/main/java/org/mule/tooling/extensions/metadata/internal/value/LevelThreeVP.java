package org.mule.tooling.extensions.metadata.internal.value;

import static java.util.Arrays.asList;
import static org.mule.runtime.extension.api.values.ValueBuilder.newValue;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.HashSet;
import java.util.Set;

public class LevelThreeVP implements ValueProvider {

  @Parameter
  private String levelTwo;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return new HashSet<>(
            asList(newValue("LEVEL-THREE-ONE-" + levelTwo).build(),
                   newValue("LEVEL-THREE-TWO-" + levelTwo).build(),
                   newValue("LEVEL-THREE-THREE-" + levelTwo).build()
            ));
  }
}
