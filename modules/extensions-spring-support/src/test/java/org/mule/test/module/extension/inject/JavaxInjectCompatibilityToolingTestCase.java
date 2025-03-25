/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.inject;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.createComponentParameterizationFromComponentAst;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVAX_INJECT_COMPATIBILITY;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableWithSize.iterableWithSize;
import static org.hamcrest.core.Is.is;

import org.mule.functional.junit4.AbstractArtifactAstTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.module.extension.api.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.api.runtime.config.ExtensionDesignTimeResolversFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultExtensionDesignTimeResolversFactory;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.test.javaxinject.JavaxInjectCompatibilityTestExtension;
import org.mule.test.module.extension.data.sample.SampleDataExecutor;
import org.mule.test.module.extension.values.ValueProviderExecutor;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(JAVA_SDK)
@Story(JAVAX_INJECT_COMPATIBILITY)
public class JavaxInjectCompatibilityToolingTestCase extends AbstractArtifactAstTestCase {

  private ExtensionModel javaxInjectExtension;

  private ExtensionDesignTimeResolversFactory extensionDesignTimeResolversFactory;
  private ConfigurationProviderFactory configurationProviderFactory;
  private SampleDataExecutor sampleDataExecutor;

  @Before
  public void createExtensionDesignTimeResolversFactory() throws InitialisationException {
    extensionDesignTimeResolversFactory = new DefaultExtensionDesignTimeResolversFactory();
    initialiseIfNeeded(extensionDesignTimeResolversFactory, true, muleContext);
    configurationProviderFactory = new DefaultConfigurationProviderFactory();
    initialiseIfNeeded(configurationProviderFactory, true, muleContext);

    sampleDataExecutor = new SampleDataExecutor(extensionDesignTimeResolversFactory);
  }

  @Override
  protected String getConfigFile() {
    return "inject/javax-inject-compatibility-config.xml";
  }

  @Override
  protected Set<ExtensionModel> getRequiredExtensions() {
    final var extensions = new HashSet<ExtensionModel>();
    extensions.add(getExtensionModel());
    javaxInjectExtension = loadExtension(JavaxInjectCompatibilityTestExtension.class, emptySet());
    extensions.add(javaxInjectExtension);
    return extensions;
  }

  @Test
  public void sampleData() throws SampleDataException {
    final var operationAst = getFlowComponent("operation", OPERATION);
    final Message sampleData =
        sampleDataExecutor.getSampleData(javaxInjectExtension,
                                         createComponentParameterizationFromComponentAst(operationAst),
                                         empty())
            .getSampleData()
            .orElseThrow();

    assertThat(sampleData.getPayload().getValue(), is(defaultCharset().name()));
  }

  @Test
  public void valueProviders() {
    final var operationAst = getFlowComponent("valueProvider", OPERATION);

    final var valueProviderExecutor = new ValueProviderExecutor(extensionDesignTimeResolversFactory,
                                                                operationAst.getModel(ParameterizedModel.class).orElseThrow());

    final var operationParameterization = createComponentParameterizationFromComponentAst(operationAst);
    final ValueResult values =
        valueProviderExecutor.resolveValues(javaxInjectExtension, "param", operationParameterization, empty(), null);

    assertThat(values.getValues(), iterableWithSize(1));
    assertThat(values.getValues().iterator().next().getId(), is(defaultCharset().name()));
  }

}
