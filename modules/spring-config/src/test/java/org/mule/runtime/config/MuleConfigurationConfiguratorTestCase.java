/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_RESPONSE_TIMEOUT;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENCODING_SYSTEM_PROPERTY;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.parseAndBuildAppExtensionModel;
import static org.mule.tck.junit4.matcher.IsEmptyOptional.empty;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.COMPONENT_CONFIGURATION_PROPERTIES_STORY;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.Is.isA;
import static org.junit.Assert.assertThrows;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.config.internal.ArtifactAstConfigurationBuilder;
import org.mule.runtime.config.internal.bean.TestCustomServiceDependingOnMuleConfiguration;
import org.mule.runtime.config.internal.model.DefaultComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;
import org.mule.runtime.core.api.config.DynamicConfigExpiration;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.EventContextFactory;
import org.mule.runtime.core.internal.config.ImmutableExpirationPolicy;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.transformer.simple.ObjectToInputStream;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.runtime.module.artifact.activation.api.ast.AstXmlParserSupplier;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.MockExtensionManagerConfigurationBuilder;
import org.mule.tck.junit4.matcher.EventMatcher;

import java.util.Calendar;
import java.util.Set;

import org.slf4j.Logger;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_PROPERTIES)
@Story(COMPONENT_CONFIGURATION_PROPERTIES_STORY)
public class MuleConfigurationConfiguratorTestCase extends AbstractMuleTestCase {

  private static final String CUSTOM_SERVICE_DEPENDING_ON_MULE_CONFIGURATION = "TestCustomServiceDependingOnMuleConfiguration";
  private static final Logger LOGGER = getLogger(MuleConfigurationConfiguratorTestCase.class);

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private final TimeSupplier timeSupplier = mock(TimeSupplier.class);

  // Lazily created through getMuleContext
  private MuleContextWithRegistry muleContext;

  @After
  public void after() {
    if (muleContext != null) {
      disposeIfNeeded(muleContext, LOGGER);
    }
  }

  @Test
  public void whenNotConfiguredThenHasSameDefaults() throws MuleException {
    DefaultMuleConfiguration config = getMuleContext().getRegistry().lookupObject(OBJECT_MULE_CONFIGURATION);

    // First make sure the config from the registry is equivalent to the one from the MuleContext
    assertThat(config, is(getMuleContext().getConfiguration()));

    // Second, checks a MuleConfiguration injected in a custom service is also the same
    assertEqualsToMuleConfigurationInjectedInCustomService(config);

    // Now checks that the values are the same as the ones on a fresh DefaultMuleConfiguration instance
    // (except for the ID and system ID because they contain UUIDs, using equals won't do because of that).
    DefaultMuleConfiguration defaultConfig = new DefaultMuleConfiguration();
    assertThat(config.getDefaultResponseTimeout(), is(defaultConfig.getDefaultResponseTimeout()));
    assertThat(config.getWorkingDirectory(), is(defaultConfig.getWorkingDirectory()));
    assertThat(config.getMuleHomeDirectory(), is(defaultConfig.getMuleHomeDirectory()));
    assertThat(config.getDefaultTransactionTimeout(), is(defaultConfig.getDefaultTransactionTimeout()));
    assertThat(config.isClientMode(), is(defaultConfig.isClientMode()));
    assertThat(config.getDefaultEncoding(), is(defaultConfig.getDefaultEncoding()));
    assertThat(config.getDomainId(), is(defaultConfig.getDomainId()));
    assertThat(config.getSystemModelType(), is(defaultConfig.getSystemModelType()));
    assertThat(config.isAutoWrapMessageAwareTransform(), is(defaultConfig.isAutoWrapMessageAwareTransform()));
    assertThat(config.isCacheMessageAsBytes(), is(defaultConfig.isCacheMessageAsBytes()));
    assertThat(config.isEnableStreaming(), is(defaultConfig.isEnableStreaming()));
    assertThat(config.isValidateExpressions(), is(defaultConfig.isValidateExpressions()));
    assertThat(config.isLazyInit(), is(defaultConfig.isLazyInit()));
    assertThat(config.getDefaultQueueTimeout(), is(defaultConfig.getDefaultQueueTimeout()));
    assertThat(config.getShutdownTimeout(), is(defaultConfig.getShutdownTimeout()));
    assertThat(config.getMaxQueueTransactionFilesSizeInMegabytes(),
               is(defaultConfig.getMaxQueueTransactionFilesSizeInMegabytes()));
    assertThat(config.isContainerMode(), is(defaultConfig.isContainerMode()));
    assertThat(config.isStandalone(), is(defaultConfig.isStandalone()));
    assertThat(config.getDefaultErrorHandlerName(), is(defaultConfig.getDefaultErrorHandlerName()));
    assertThat(config.isDisableTimeouts(), is(defaultConfig.isDisableTimeouts()));
    assertThat(config.getDefaultObjectSerializer(), is(defaultConfig.getDefaultObjectSerializer()));
    assertThat(config.getDefaultProcessingStrategyFactory(), is(defaultConfig.getDefaultProcessingStrategyFactory()));
    assertDynamicConfigExpirationEquals(config.getDynamicConfigExpiration(), defaultConfig.getDynamicConfigExpiration());
    assertThat(config.isInheritIterableRepeatability(), is(defaultConfig.isInheritIterableRepeatability()));
    assertThat(config.getMinMuleVersion(), is(defaultConfig.getMinMuleVersion()));
    assertThat(config.getDefaultCorrelationIdGenerator(), is(defaultConfig.getDefaultCorrelationIdGenerator()));
    assertThat(config.getArtifactCoordinates(), is(defaultConfig.getArtifactCoordinates()));

    // These are DefaultMuleConfiguration-specific
    // (getDataFolderName is not compared because it also contains a UUID by default)
    assertThat(config.isFlowTrace(), is(defaultConfig.isFlowTrace()));
    assertThat(config.getExtendedProperties(), is(defaultConfig.getExtendedProperties()));
    assertThat(config.getExtensions(), is(defaultConfig.getExtensions()));
  }

  @Test
  public void whenConfiguredThroughSystemPropertiesThenHasExpectedValues() throws MuleException {
    setProperty(MULE_ENCODING_SYSTEM_PROPERTY, "UTF-16");
    setProperty(SYSTEM_PROPERTY_PREFIX + "systemModelType", "direct");
    setProperty(SYSTEM_PROPERTY_PREFIX + "workingDirectory", "some-working-dir");
    setProperty(SYSTEM_PROPERTY_PREFIX + "clientMode", "true");
    setProperty(SYSTEM_PROPERTY_PREFIX + "serverId", "MY_SERVER");
    setProperty(SYSTEM_PROPERTY_PREFIX + "domainId", "MY_DOMAIN");
    setProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheBytes", "false");
    setProperty(SYSTEM_PROPERTY_PREFIX + "streaming.enable", "false");
    setProperty(SYSTEM_PROPERTY_PREFIX + "transform.autoWrap", "false");
    setProperty(SYSTEM_PROPERTY_PREFIX + "validate.expressions", "false");
    setProperty(MULE_DISABLE_RESPONSE_TIMEOUT, "true");

    try {
      whenNotConfiguredThenHasSameDefaults();
      // server ID is not asserted in the defaults
      assertThat(getMuleContext().getConfiguration().getId(), is(new DefaultMuleConfiguration().getId()));
    } finally {
      clearProperty(MULE_ENCODING_SYSTEM_PROPERTY);
      clearProperty(SYSTEM_PROPERTY_PREFIX + "systemModelType");
      clearProperty(SYSTEM_PROPERTY_PREFIX + "workingDirectory");
      clearProperty(SYSTEM_PROPERTY_PREFIX + "clientMode");
      clearProperty(SYSTEM_PROPERTY_PREFIX + "serverId");
      clearProperty(SYSTEM_PROPERTY_PREFIX + "domainId");
      clearProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheBytes");
      clearProperty(SYSTEM_PROPERTY_PREFIX + "streaming.enable");
      clearProperty(SYSTEM_PROPERTY_PREFIX + "transform.autoWrap");
      clearProperty(SYSTEM_PROPERTY_PREFIX + "validate.expressions");
      clearProperty(MULE_DISABLE_RESPONSE_TIMEOUT);
    }
  }

  @Test
  public void whenConfiguredFromConfigFileThenHasExpectedValues() throws MuleException {
    initMuleContext("withGlobalConfig.xml");

    DefaultMuleConfiguration config = getMuleContext().getRegistry().lookupObject(OBJECT_MULE_CONFIGURATION);

    // First make sure the config from the registry is equivalent to the one from the MuleContext
    assertThat(config, is(getMuleContext().getConfiguration()));

    // Second, checks a MuleConfiguration injected in a custom service is also the same
    assertEqualsToMuleConfigurationInjectedInCustomService(config);

    // Now checks the values that are being overridden by the configuration file
    assertThat(config.getDefaultErrorHandlerName(), is("errorHandler"));
    assertThat(config.getDefaultResponseTimeout(), is(5000));
    assertThat(config.getMaxQueueTransactionFilesSizeInMegabytes(), is(100));
    assertThat(config.getDefaultTransactionTimeout(), is(20000));
    assertThat(config.getShutdownTimeout(), is(2000L));
    assertThat(config.getDefaultObjectSerializer(), isA(TestSerializationProtocol.class));
    assertThat(config.isInheritIterableRepeatability(), is(true));

    DynamicConfigExpiration dynamicConfigExpiration = config.getDynamicConfigExpiration();
    assertThat(dynamicConfigExpiration.getFrequency().getTime(), is(7L));
    assertThat(dynamicConfigExpiration.getFrequency().getUnit(), is(DAYS));
    assertThat(dynamicConfigExpiration.getExpirationPolicy().getMaxIdleTime(), is(40L));
    assertThat(dynamicConfigExpiration.getExpirationPolicy().getTimeUnit(), is(HOURS));

    // We need an expression manager in order to generate a correlation ID, and it is not worth adding such overhead to this test.
    // We will just assert the generator is not the default (empty).
    assertThat(config.getDefaultCorrelationIdGenerator(), not(is(empty())));
  }

  @Test
  public void whenMultipleConfigsInFileThenFails() {
    ConfigurationException e = assertThrows(ConfigurationException.class, () -> initMuleContext("withMultipleGlobalConfig.xml"));
    assertThat(e, hasMessage(containsString("The configuration element 'configuration' can only appear once")));
  }

  @Test
  public void whenBeanFromConfigFileDependsOnMuleConfigurationThenItSeesTheCorrectValues() throws MuleException {
    initMuleContext("withGlobalConfig.xml");
    assertThat(runFlow("mainFlow"), EventMatcher.hasMessage(hasPayload(is("errorHandler"))));
  }

  @Test
  @Issue("MULE-19006")
  public void configuratorExpirationPolicyUsesManagedTimeSupplier() throws Exception {
    MuleConfiguration configuration = getMuleContext().getRegistry()
        .lookupObject(OBJECT_MULE_CONFIGURATION);
    ExpirationPolicy policy = configuration.getDynamicConfigExpiration().getExpirationPolicy();
    assertThat(policy, instanceOf(ImmutableExpirationPolicy.class));
    // This is done so that the timeSupplier is invoked.
    configuration.getDynamicConfigExpiration().getExpirationPolicy().isExpired(0L, DAYS);
    verify(timeSupplier).getAsLong();
  }

  @Test
  @Issue("MULE-20031")
  public void muleContextInjectedIntoTransformers() throws Exception {
    ObjectToInputStream o2isTransformer = getMuleContext().getRegistry().lookupObject(ObjectToInputStream.class);

    assertThat(o2isTransformer.doTransform(singletonMap("key", "value"), UTF_8), not(nullValue()));
  }

  private void assertEqualsToMuleConfigurationInjectedInCustomService(MuleConfiguration actualConfig) throws MuleException {
    TestCustomServiceDependingOnMuleConfiguration testService =
        getMuleContext().getRegistry().lookupObject(CUSTOM_SERVICE_DEPENDING_ON_MULE_CONFIGURATION);
    assertThat(actualConfig, is(testService.getMuleConfigurationJavax()));
    assertThat(actualConfig, is(testService.getMuleConfiguration()));
  }

  private void assertDynamicConfigExpirationEquals(DynamicConfigExpiration actual, DynamicConfigExpiration expected) {
    assertThat(actual.getFrequency().getTime(), is(expected.getFrequency().getTime()));
    assertThat(actual.getFrequency().getUnit(), is(expected.getFrequency().getUnit()));
    assertThat(actual.getExpirationPolicy().getMaxIdleTime(), is(expected.getExpirationPolicy().getMaxIdleTime()));
    assertThat(actual.getExpirationPolicy().getTimeUnit(), is(expected.getExpirationPolicy().getTimeUnit()));
  }

  private CoreEvent runFlow(String flowName) throws MuleException {
    FlowConstruct flow = getMuleContext().getRegistry().lookupFlowConstruct(flowName);
    EventContext eventContext = EventContextFactory.create(flow, from(flow.getName()));
    CoreEvent event = CoreEvent.builder(eventContext)
        .message(Message.builder().nullValue().build())
        .build();
    return ((Flow) flow).process(event);
  }

  private MuleContextWithRegistry getMuleContext() throws MuleException {
    if (muleContext == null) {
      initMuleContext();
    }
    return muleContext;
  }

  private void initMuleContext() throws MuleException {
    initMuleContext(null);
  }

  private void initMuleContext(String configPath) throws MuleException {
    String[] configFiles = configPath != null ? new String[] {configPath} : new String[] {};

    muleContext = (MuleContextWithRegistry) new DefaultMuleContextFactory()
        .createMuleContext(testServicesConfigurationBuilder,
                           new AbstractConfigurationBuilder() {

                             @Override
                             protected void doConfigure(MuleContext muleContext) {
                               muleContext.getCustomizationService()
                                   .interceptDefaultServiceImpl(OBJECT_TIME_SUPPLIER, si -> si.overrideServiceImpl(timeSupplier));
                               muleContext.getCustomizationService()
                                   .registerCustomServiceClass(CUSTOM_SERVICE_DEPENDING_ON_MULE_CONFIGURATION,
                                                               TestCustomServiceDependingOnMuleConfiguration.class);
                             }
                           },
                           new MockExtensionManagerConfigurationBuilder(singleton(getExtensionModel())),
                           new AppParserConfigurationBuilder(configFiles));
    muleContext.start();
    muleContext.getRegistry().lookupByType(Calendar.class);
  }

  private static class AppParserConfigurationBuilder extends AbstractConfigurationBuilder implements AstXmlParserSupplier {

    private final String[] configFiles;

    private AppParserConfigurationBuilder(String[] configFiles) {
      this.configFiles = configFiles;
    }

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception {
      ArtifactAst artifactAst;
      if (configFiles.length == 0) {
        artifactAst = emptyArtifact();
      } else {
        artifactAst = parseAndBuildAppExtensionModel(muleContext.getConfiguration().getId(),
                                                     configFiles, this, muleContext.getExtensionManager().getExtensions(), false,
                                                     muleContext.getExecutionClassLoader(), muleContext.getConfiguration(), null);
      }
      new ArtifactAstConfigurationBuilder(artifactAst, emptyMap(), APP, false, false,
                                          new DefaultComponentBuildingDefinitionRegistryFactory()
                                              .create(artifactAst.dependencies(),
                                                      artifactAst::dependenciesDsl))
                                                          .configure(muleContext);
    }

    @Override
    public AstXmlParser getParser(Set<ExtensionModel> extensions, boolean disableValidations) {
      return AstXmlParser.builder()
          .withArtifactType(APPLICATION)
          .withExtensionModels(extensions)
          .build();
    }
  }

  /**
   * Just a dummy serializer used for testing that a custom serializer can be configured globally.
   * <p>
   * Not really used for serializing.
   */
  public static class TestSerializationProtocol implements ObjectSerializer {

    @Override
    public SerializationProtocol getInternalProtocol() {
      return null;
    }

    @Override
    public SerializationProtocol getExternalProtocol() {
      return null;
    }
  }

}
