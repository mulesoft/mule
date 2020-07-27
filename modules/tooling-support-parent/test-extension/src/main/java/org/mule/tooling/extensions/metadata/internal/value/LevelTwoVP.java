package org.mule.tooling.extensions.metadata.internal.value;

import static java.util.Arrays.asList;
import static org.mule.runtime.extension.api.values.ValueBuilder.newValue;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.HashSet;
import java.util.Set;

public class LevelTwoVP implements ValueProvider {

  @Parameter
  private String levelOne;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return new HashSet<>(
            asList(newValue("LEVEL-TWO-ONE-" + levelOne).build(),
                   newValue("LEVEL-TWO-TWO-" + levelOne).build(),
                   newValue("LEVEL-TWO-THREE-" + levelOne).build()
            ));
  }
}
