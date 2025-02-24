/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.ApplicationConfiguration.APPLICATION_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.artifact.ArtifactCoordinates;
import org.mule.runtime.core.internal.config.builders.MinimalConfigurationBuilder;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.api.context.MuleContextBuilder;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

@SmallTest
@Feature(DEPLOYMENT_CONFIGURATION)
@Story(APPLICATION_CONFIGURATION)
public class MuleConfigurationTestCase extends AbstractMuleTestCase {

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private final ConfigurationProperties mockedConfigurationProperties = mock(ConfigurationProperties.class);

  protected String workingDirectory = "target";
  private MuleContext muleContext;

  @Before
  public void setUp() {
    when(mockedConfigurationProperties.resolveStringProperty(anyString())).thenReturn(empty());
    testServicesConfigurationBuilder.registerAdditionalService("mockedConfigurationProperties", mockedConfigurationProperties);
  }

  @After
  public void tearDown() throws Exception {
    muleContext.dispose();
    muleContext = null;
  }

  @Test
  @Issue("MULE-3092")
  public void testConfigureProgramatically() throws Exception {
    DefaultMuleConfiguration config = new DefaultMuleConfiguration();
    config.setDefaultEncoding("UTF-16");
    config.setDefaultSynchronousEndpoints(true);
    config.setSystemModelType("direct");
    config.setDefaultResponseTimeout(30000);
    config.setDefaultTransactionTimeout(60000);
    config.setWorkingDirectory(workingDirectory);
    config.setClientMode(true);
    config.setId("MY_SERVER");
    config.setDomainId("MY_DOMAIN");
    config.setCacheMessageAsBytes(false);
    config.setEnableStreaming(false);
    config.setMaxQueueTransactionFilesSize(500);
    config.setAutoWrapMessageAwareTransform(false);

    MuleContextBuilder contextBuilder = MuleContextBuilder.builder(APP);
    contextBuilder.setMuleConfiguration(config);
    final ArtifactCoordinates artifactCoordinates = mock(ArtifactCoordinates.class);
    contextBuilder.setArtifactCoordinates(artifactCoordinates);

    muleContext = new DefaultMuleContextFactory()
        .createMuleContext(asList(testServicesConfigurationBuilder, new MinimalConfigurationBuilder()), contextBuilder);
    muleContext.start();

    verifyConfiguration();
    assertThat(muleContext.getConfiguration().getArtifactCoordinates().get(), is(sameInstance(artifactCoordinates)));
  }

  @Test
  @Issue("MULE-3092")
  public void testConfigureWithSystemProperties() throws Exception {
    setProperty(SYSTEM_PROPERTY_PREFIX + "encoding", "UTF-16");
    setProperty(SYSTEM_PROPERTY_PREFIX + "endpoints.synchronous", "true");
    setProperty(SYSTEM_PROPERTY_PREFIX + "systemModelType", "direct");
    setProperty(SYSTEM_PROPERTY_PREFIX + "timeout.synchronous", "30000");
    setProperty(SYSTEM_PROPERTY_PREFIX + "timeout.transaction", "60000");
    setProperty(SYSTEM_PROPERTY_PREFIX + "remoteSync", "true");
    setProperty(SYSTEM_PROPERTY_PREFIX + "workingDirectory", workingDirectory);
    setProperty(SYSTEM_PROPERTY_PREFIX + "clientMode", "true");

    setProperty(SYSTEM_PROPERTY_PREFIX + "serverId", "MY_SERVER");
    setProperty(SYSTEM_PROPERTY_PREFIX + "domainId", "MY_DOMAIN");
    setProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheBytes", "false");
    setProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheOriginal", "false");
    setProperty(SYSTEM_PROPERTY_PREFIX + "streaming.enable", "false");
    setProperty(SYSTEM_PROPERTY_PREFIX + "message.assertAccess", "false");
    setProperty(SYSTEM_PROPERTY_PREFIX + "transform.autoWrap", "false");

    muleContext = new DefaultMuleContextFactory()
        .createMuleContext(testServicesConfigurationBuilder, new MinimalConfigurationBuilder());
    muleContext.start();

    verifyConfiguration();

    clearProperty(SYSTEM_PROPERTY_PREFIX + "encoding");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "endpoints.synchronous");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "systemModelType");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "timeout.synchronous");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "timeout.transaction");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "remoteSync");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "workingDirectory");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "clientMode");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "disable.threadsafemessages");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "serverId");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "domainId");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheBytes");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "message.cacheOriginal");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "streaming.enable");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "message.assertAccess");
    clearProperty(SYSTEM_PROPERTY_PREFIX + "transform.autoWrap");
  }

  @Test
  @Issue("MULE-3110")
  public void testConfigureAfterInitFails() throws Exception {
    muleContext = new DefaultMuleContextFactory()
        .createMuleContext(testServicesConfigurationBuilder, new MinimalConfigurationBuilder());

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());

    // These are OK to change after init but before start
    mutableConfig.setDefaultSynchronousEndpoints(true);
    mutableConfig.setSystemModelType("direct");
    mutableConfig.setClientMode(true);

    // These are not OK to change after init
    mutableConfig.setDefaultEncoding("UTF-16");
    mutableConfig.setWorkingDirectory(workingDirectory);
    mutableConfig.setId("MY_SERVER");
    mutableConfig.setDomainId("MY_DOMAIN");

    MuleConfiguration config = muleContext.getConfiguration();

    // These are OK to change after init but before start
    assertThat(config.getSystemModelType(), is("direct"));
    assertThat(config.isClientMode(), is(true));

    // These are not OK to change after init
    assertThat(config.getDefaultEncoding(), not(equals("UTF-16")));
    assertThat(config.getWorkingDirectory(), not(equals(workingDirectory)));
    assertThat(config.getId(), not(equals("MY_SERVER")));
    assertThat(config.getDomainId(), not(equals("MY_DOMAIN")));
  }

  @Test
  @Issue("MULE-3110")
  public void testConfigureAfterStartFails() throws Exception {
    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new MinimalConfigurationBuilder());
    muleContext.start();

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());
    mutableConfig.setDefaultSynchronousEndpoints(true);
    mutableConfig.setSystemModelType("direct");
    mutableConfig.setClientMode(true);

    MuleConfiguration config = muleContext.getConfiguration();
    assertThat(config.getSystemModelType(), not(equals("direct")));
    assertThat(config.isClientMode(), is(false));
  }

  protected void verifyConfiguration() {
    MuleConfiguration config = muleContext.getConfiguration();
    assertThat(config.getDefaultEncoding(), is("UTF-16"));
    assertThat(config.getSystemModelType(), is("direct"));
    // on windows this ends up with a c:/ in it
    assertThat(config.getWorkingDirectory(), containsString(workingDirectory));
    assertThat(config.isClientMode(), is(true));
    assertThat(config.getId(), is("MY_SERVER"));
    assertThat(config.getDomainId(), is("MY_DOMAIN"));
    assertThat(config.isCacheMessageAsBytes(), is(false));
    assertThat(config.isEnableStreaming(), is(false));
    assertThat(config.isAutoWrapMessageAwareTransform(), is(false));
    assertThat(config.getMaxQueueTransactionFilesSizeInMegabytes(), is(500));
  }

  @Test
  public void notContainerNotstandaloneDirectory() throws Exception {
    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new MinimalConfigurationBuilder());
    muleContext.start();

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());
    mutableConfig.setContainerMode(false);
    mutableConfig.setDataFolderName("sarasa");
    assertThat(mutableConfig.getWorkingDirectory(), is("./.mule"));
  }

  @Test
  public void standaloneDirectory() throws Exception {
    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new MinimalConfigurationBuilder());
    muleContext.start();

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());
    mutableConfig.setContainerMode(false);
    try {
      System.setProperty(MULE_HOME_DIRECTORY_PROPERTY, "thehome");
      mutableConfig.setDataFolderName("sarasa");
      assertThat(mutableConfig.getWorkingDirectory(), is("./.mule/sarasa"));
    } finally {
      System.clearProperty(MULE_HOME_DIRECTORY_PROPERTY);
    }
  }

  @Test
  public void clusterId() throws Exception {
    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new MinimalConfigurationBuilder());

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());
    mutableConfig.setClusterId("the cluster id");
    assertThat(mutableConfig.getSystemName(), containsString("the cluster id"));
  }

  @Test
  public void extendedProperties() throws Exception {
    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new MinimalConfigurationBuilder());

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());
    Map<String, String> props = new HashMap<>();
    props.put("name", "value");
    mutableConfig.setExtendedProperties(props);
    mutableConfig.setExtendedProperty("another", "property");
    assertThat(mutableConfig.getExtendedProperties().keySet(), hasSize(2));
    assertThat(mutableConfig.getExtendedProperty("name"), is("value"));
    assertThat(mutableConfig.getExtendedProperty("another"), is("property"));
  }

  @Test
  public void getExtension() throws Exception {
    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new MinimalConfigurationBuilder());

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());
    assertThat(mutableConfig.getExtension(MuleConfigurationTestCase.class), is(empty()));
  }

  @Test
  public void testEquals() throws Exception {
    muleContext = new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder,
                                                                    new MinimalConfigurationBuilder());

    DefaultMuleConfiguration mutableConfig = ((DefaultMuleConfiguration) muleContext.getConfiguration());

    MuleContext muleContext2 = null;
    try {
      muleContext2 =
          new DefaultMuleContextFactory().createMuleContext(testServicesConfigurationBuilder, new MinimalConfigurationBuilder());
      muleContext2.start();
      DefaultMuleConfiguration mutableConfig2 = ((DefaultMuleConfiguration) muleContext2.getConfiguration());
      assertThat(mutableConfig.equals(mutableConfig2), is(false));
    } finally {
      if (muleContext2 != null) {
        muleContext2.dispose();
      }
    }
  }

}
