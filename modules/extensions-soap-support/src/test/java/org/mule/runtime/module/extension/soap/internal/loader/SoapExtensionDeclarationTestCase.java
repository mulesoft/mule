/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.ATTACHMENTS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.HEADERS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.OPERATION_DESCRIPTION;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.OPERATION_NAME;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.OPERATION_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.REQUEST_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.SERVICE_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.InvokeOperationDeclarer.TRANSPORT_HEADERS_PARAM;
import static org.mule.test.soap.extension.CalcioServiceProvider.CALCIO_DESC;
import static org.mule.test.soap.extension.CalcioServiceProvider.CALCIO_ID;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.model.UnionType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.services.soap.api.exception.error.SoapErrors;
import org.mule.test.soap.extension.FootballSoapExtension;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;

public class SoapExtensionDeclarationTestCase {

  private SoapExtensionModelLoader loader = new SoapExtensionModelLoader();

  @Test
  public void assertSoapExtensionModel() {
    Map<String, Object> params = new HashMap<>();
    params.put(TYPE_PROPERTY_NAME, FootballSoapExtension.class.getName());
    params.put(VERSION, getProductVersion());
    ExtensionModel model =
        loader.loadExtensionModel(FootballSoapExtension.class.getClassLoader(), getDefault(emptySet()), params);

    assertErrorModels(model.getErrorModels());

    assertThat(model.getConfigurationModels(), hasSize(1));
    ConfigurationModel configuration = model.getConfigurationModels().get(0);
    assertThat(configuration.getName(), is(DEFAULT_CONFIG_NAME));
    assertThat(configuration.getDescription(), is(DEFAULT_CONFIG_DESCRIPTION));

    assertThat(configuration.getOperationModels(), hasSize(1));
    assertOperation(configuration.getOperationModels().get(0));

    List<ConnectionProviderModel> providers = configuration.getConnectionProviders();
    assertThat(providers, hasSize(3));

    assertConnectionProvider(providers.get(0), "base-connection", "",
                             new ParameterProber("leaguesAddress", "http://some-url.com", StringType.class, false));

    assertConnectionProvider(providers.get(1), "la-liga-connection", "",
                             new ParameterProber("firstDivision", StringType.class),
                             new ParameterProber("secondDivision", StringType.class),
                             new ParameterProber("wsdlLocation", StringType.class));

    assertConnectionProvider(providers.get(2), CALCIO_ID + "-connection", CALCIO_DESC);
  }

  private void assertErrorModels(Set<ErrorModel> errors) {
    assertThat(errors, hasSize(13));
    ImmutableList<String> errorNames = ImmutableList.<String>builder()
        .addAll(of(SoapErrors.values()).map(Object::toString).collect(toList()))
        .add("RETRY_EXHAUSTED")
        .add("CONNECTIVITY")
        .add("ANY")
        .build();
    errors.forEach(e -> assertThat(e.getType(), isOneOf(errorNames.toArray())));
  }

  private void assertOperation(OperationModel operation) {
    assertThat(operation.getOutput().getType(), is(instanceOf(UnionType.class)));
    assertErrorModels(operation.getErrorModels());
    assertThat(operation.getName(), is(OPERATION_NAME));
    assertThat(operation.getDescription(), is(OPERATION_DESCRIPTION));
    ParameterProber[] probers = new ParameterProber[] {
        new ParameterProber(OPERATION_PARAM, StringType.class),
        new ParameterProber(SERVICE_PARAM, StringType.class),
        new ParameterProber(REQUEST_PARAM, "#[payload]", BinaryType.class, false),
        new ParameterProber(HEADERS_PARAM, null, BinaryType.class, false),
        new ParameterProber(TRANSPORT_HEADERS_PARAM, null, ObjectType.class, false),
        new ParameterProber(ATTACHMENTS_PARAM, null, ObjectType.class, false),
    };
    // the `1` is added because the sdk adds the target parameter automatically
    assertThat(operation.getAllParameterModels(), hasSize(probers.length + 1));
    assertParameters(operation.getAllParameterModels(), probers);
  }

  private void assertConnectionProvider(ConnectionProviderModel provider,
                                        String name,
                                        String description,
                                        ParameterProber... probers) {
    List<ParameterModel> parameterModels = provider.getAllParameterModels();
    assertThat(provider.getName(), is(name));
    assertThat(provider.getDescription(), is(description));
    // the `3` is added because the SDK adds the infrastructure parameters for pooling connection providers.
    assertThat(parameterModels, hasSize(probers.length + 3));
    assertParameters(parameterModels, probers);
  }

  private void assertParameters(List<ParameterModel> parameterModels, ParameterProber... probers) {
    if (!parameterModels.isEmpty()) {
      stream(probers).forEach(prober -> {
        String name = prober.getName();
        Optional<ParameterModel> parameter = parameterModels.stream().filter(p -> name.equals(p.getName())).findAny();
        assertParameter(parameter.orElseThrow(() -> new RuntimeException("parameter [" + name + "] not found")), prober);
      });
    }
  }

  private void assertParameter(ParameterModel param, ParameterProber prober) {
    assertThat(param.getName(), is(prober.getName()));
    assertThat(param.getType(), instanceOf(prober.getType()));
    assertThat(param.getDefaultValue(), is(prober.getDefaultValue()));
    assertThat(param.isRequired(), is(prober.isRequired()));
  }

  private class ParameterProber {

    private final String name;
    private final String defaultValue;
    private final Class type;
    private final boolean required;

    ParameterProber(String name, String defaultValue, Class type, boolean required) {
      this.name = name;
      this.defaultValue = defaultValue;
      this.type = type;
      this.required = required;
    }

    ParameterProber(String name, Class type) {
      this(name, null, type, true);
    }

    public String getName() {
      return name;
    }

    public boolean isRequired() {
      return required;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public Class getType() {
      return type;
    }
  }
}
