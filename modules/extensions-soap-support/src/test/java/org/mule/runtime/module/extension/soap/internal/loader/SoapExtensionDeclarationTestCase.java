/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.loader;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.extension.api.error.MuleErrors.ANY;
import static org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.ATTACHMENTS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.BODY_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.HEADERS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.OPERATION_DESCRIPTION;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.OPERATION_NAME;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.OPERATION_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.SERVICE_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.TRANSPORT_HEADERS_PARAM;
import static org.mule.test.soap.extension.CalcioServiceProvider.CALCIO_DESC;
import static org.mule.test.soap.extension.CalcioServiceProvider.CALCIO_ID;
import org.mule.metadata.api.model.BinaryType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.module.extension.internal.loader.enricher.ModuleErrors;
import org.mule.runtime.soap.api.exception.error.SoapErrors;
import org.mule.test.soap.extension.FootballSoapExtension;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SoapExtensionDeclarationTestCase extends AbstractSoapExtensionDeclarationTestCase {

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
                             new ParameterProber("laLigaAddress", null, StringType.class, true),
                             new ParameterProber("leaguesAddress", "http://some-url.com", StringType.class, false));

    assertConnectionProvider(providers.get(1), CALCIO_ID + "-connection", CALCIO_DESC);

    assertConnectionProvider(providers.get(2), "la-liga-connection", "",
                             new ParameterProber("firstDivision", StringType.class),
                             new ParameterProber("secondDivision", StringType.class),
                             new ParameterProber("wsdlLocation", StringType.class));
  }

  private void assertErrorModels(Set<ErrorModel> errors) {
    assertThat(errors, hasSize(12));
    ImmutableList<String> errorNames = ImmutableList.<String>builder()
        .addAll(of(SoapErrors.values()).map(Object::toString).collect(toList()))
        .addAll(of(ModuleErrors.values()).map(Object::toString).collect(toList()))
        .add(ANY.name())
        .build();
    errors.forEach(e -> assertThat(e.getType(), isOneOf(errorNames.toArray())));
  }

  private void assertOperation(OperationModel operation) {
    assertThat(operation.getOutput().getType(), is(instanceOf(ObjectType.class)));
    assertThat(operation.getOutputAttributes().getType(), is(instanceOf(ObjectType.class)));
    assertErrorModels(operation.getErrorModels());
    assertThat(operation.getName(), is(OPERATION_NAME));
    assertThat(operation.getDescription(), is(OPERATION_DESCRIPTION));
    ParameterProber[] probers = new ParameterProber[] {
        new ParameterProber(OPERATION_PARAM, StringType.class),
        new ParameterProber(SERVICE_PARAM, StringType.class),
        new ParameterProber(BODY_PARAM, PAYLOAD, BinaryType.class, false),
        new ParameterProber(HEADERS_PARAM, null, BinaryType.class, false),
        new ParameterProber(TRANSPORT_HEADERS_PARAM, null, ObjectType.class, false),
        new ParameterProber(ATTACHMENTS_PARAM, null, ObjectType.class, false),
    };
    // the `3` is added because the sdk adds the target, targetValue, and retryPolicy parameters automatically
    assertThat(operation.getAllParameterModels(), hasSize(probers.length + 3));
    assertParameters(operation.getAllParameterModels(), probers);
  }
}
