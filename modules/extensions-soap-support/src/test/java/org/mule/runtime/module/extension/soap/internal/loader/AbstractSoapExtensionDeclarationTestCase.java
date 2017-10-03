/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static java.util.Arrays.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.extension.api.property.InfrastructureParameterModelProperty;
import org.mule.runtime.module.extension.soap.api.loader.SoapExtensionModelLoader;

import java.util.List;
import java.util.Optional;

public class AbstractSoapExtensionDeclarationTestCase {

  SoapExtensionModelLoader loader = new SoapExtensionModelLoader();

  void assertConnectionProvider(ConnectionProviderModel provider,
                                String name,
                                String description,
                                ParameterProber... probers) {
    List<ParameterModel> parameterModels = provider.getAllParameterModels();
    assertThat(provider.getName(), is(name));
    assertThat(provider.getDescription(), is(description));

    Long parameterCount = parameterModels.stream()
        .filter(p -> !p.getModelProperty(InfrastructureParameterModelProperty.class).isPresent())
        .count();

    assertThat(parameterCount.intValue(), is(probers.length));
    assertParameters(parameterModels, probers);
  }

  void assertParameters(List<ParameterModel> parameterModels, ParameterProber... probers) {
    if (!parameterModels.isEmpty()) {
      stream(probers).forEach(prober -> {
        String name = prober.getName();
        Optional<ParameterModel> parameter = parameterModels.stream().filter(p -> name.equals(p.getName())).findAny();
        assertParameter(parameter.orElseThrow(() -> new RuntimeException("parameter [" + name + "] not found")), prober);
      });
    }
  }

  void assertParameter(ParameterModel param, ParameterProber prober) {
    assertThat(param.getName(), is(prober.getName()));
    assertThat(param.getType(), instanceOf(prober.getType()));
    assertThat(param.getDefaultValue(), is(prober.getDefaultValue()));
    assertThat(param.isRequired(), is(prober.isRequired()));
  }
}
