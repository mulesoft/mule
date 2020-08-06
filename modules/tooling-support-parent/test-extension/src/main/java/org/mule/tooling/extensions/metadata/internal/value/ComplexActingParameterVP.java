package org.mule.tooling.extensions.metadata.internal.value;

import static java.util.Collections.singleton;
import static org.mule.runtime.extension.api.values.ValueBuilder.newValue;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.tooling.extensions.metadata.internal.parameters.ComplexActingParameter;
import org.mule.tooling.extensions.metadata.internal.parameters.InnerPojo;

import java.util.Set;

public class ComplexActingParameterVP implements ValueProvider {

  @Parameter
  private ComplexActingParameter actingParameter;

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(actingParameter.getIntParam());
    stringBuilder.append(actingParameter.getStringParam());
    actingParameter.getSimpleListParam().forEach(stringBuilder::append);
    actingParameter.getSimpleMapParam().forEach((k, v) -> stringBuilder.append(k).append(v));
    appendInnerPojoValues(stringBuilder, actingParameter.getInnerPojoParam());
    actingParameter.getComplexListParam().forEach(l -> appendInnerPojoValues(stringBuilder, l));
    actingParameter.getComplexMapParam().forEach((k,v) -> {
      stringBuilder.append(k);
      appendInnerPojoValues(stringBuilder, v);
    });
    return singleton(newValue(stringBuilder.toString()).build());
  }

  private void appendInnerPojoValues(StringBuilder stringBuilder, InnerPojo innerPojo) {
    stringBuilder.append(innerPojo.getIntParam()).append(innerPojo.getStringParam());
    innerPojo.getSimpleListParam().forEach(stringBuilder::append);
    innerPojo.getSimpleMapParam().forEach((k, v) -> stringBuilder.append(k).append(v));
  }
}
