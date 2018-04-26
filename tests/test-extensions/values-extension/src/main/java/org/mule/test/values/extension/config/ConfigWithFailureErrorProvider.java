package org.mule.test.values.extension.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.test.values.extension.resolver.WithErrorValueProvider;

@Configuration(name = "failure-config")
public class ConfigWithFailureErrorProvider {

  @Parameter
  @OfValues(WithErrorValueProvider.class)
  String values;

  @Parameter
  String errorCode;
}
