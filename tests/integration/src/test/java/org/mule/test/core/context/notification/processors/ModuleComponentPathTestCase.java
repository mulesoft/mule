/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification.processors;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.FLOW;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.PROCESSOR;
import static org.mule.runtime.api.component.TypedComponentIdentifier.builder;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.FLOW_IDENTIFIER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.junit.Assert;
import org.junit.Test;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.context.notification.MessageProcessorNotification;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.extension.internal.loader.XmlExtensionModelLoader;
import org.mule.runtime.module.extension.internal.manager.DefaultExtensionManager;
import ru.yandex.qatools.allure.annotations.Description;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Description("Test suite to ensure XML connectors do properly generate the paths")
public class ModuleComponentPathTestCase extends MuleArtifactFunctionalTestCase {

  private static final String COLON_SEPARATOR = ":";
  private static final String MODULE_SIMPLE_XML = "module-simple.xml";
  private static final String MODULE_SIMPLE_PROXY_XML = "module-simple-proxy.xml";
  private static final String FLOWS_USING_MODULE_SIMPLE_XML = "flows-using-modules.xml";
  private static final String BASE_PATH_XML_MODULES = "org/mule/test/integration/notifications/modules/";

  @Override
  protected String getConfigFile() {
    return BASE_PATH_XML_MODULES + FLOWS_USING_MODULE_SIMPLE_XML;
  }

  private static final Optional<String> CONFIG_FILE_NAME = of(FLOWS_USING_MODULE_SIMPLE_XML);
  private static final Optional<String> MODULE_SIMPLE_FILE_NAME = of(MODULE_SIMPLE_XML);
  private static final Optional<String> MODULE_SIMPLE_PROXY_FILE_NAME = of(MODULE_SIMPLE_PROXY_XML);
  private static final Optional<TypedComponentIdentifier> FLOW_TYPED_COMPONENT_IDENTIFIER =
      of(builder().withIdentifier(FLOW_IDENTIFIER).withType(FLOW).build());

  private static final DefaultComponentLocation FLOW_WITH_SINGLE_MP_LOCATION =
      new DefaultComponentLocation(of("flowWithSingleMp"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithSingleMp",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(15))));

  private static final DefaultComponentLocation FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER =
      new DefaultComponentLocation(of("flowWithProxyAndSimpleModuleAndLogger"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithProxyAndSimpleModuleAndLogger",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(49))));

  private static final DefaultComponentLocation FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE =
      new DefaultComponentLocation(of("flowWithProxyAndSimpleModuleAndLoggerReverse"),
                                   asList(new DefaultComponentLocation.DefaultLocationPart("flowWithProxyAndSimpleModuleAndLoggerReverse",
                                                                                           FLOW_TYPED_COMPONENT_IDENTIFIER,
                                                                                           CONFIG_FILE_NAME,
                                                                                           of(55))));

  /**
   * "module-simple" operations defined below
   */
  private static final String MODULE_SIMPLE_NAMESPACE_IN_APP = "simple-prefix";
  private static final String SET_PAYLOAD_HARDCODED_VALUE_NAME = "set-payload-hardcoded-value";
  private static final Optional<TypedComponentIdentifier> MODULE_SET_PAYLOAD_HARDCODED_VALUE =
      of(builder()
          .withIdentifier(buildFromStringRepresentation(MODULE_SIMPLE_NAMESPACE_IN_APP + ":" + SET_PAYLOAD_HARDCODED_VALUE_NAME))
          .withType(OPERATION).build());
  private static final DefaultComponentLocation OPERATION_SET_PAYLOAD_HARDCODED_VALUE =
      new DefaultComponentLocation(of(SET_PAYLOAD_HARDCODED_VALUE_NAME),
                                   asList(new DefaultComponentLocation.DefaultLocationPart(SET_PAYLOAD_HARDCODED_VALUE_NAME,
                                                                                           MODULE_SET_PAYLOAD_HARDCODED_VALUE,
                                                                                           MODULE_SIMPLE_FILE_NAME,
                                                                                           of(12))));

  private static final String SET_PAYLOAD_PARAM_VALUE_NAME = "set-payload-param-value";
  private static final Optional<TypedComponentIdentifier> MODULE_SET_PAYLOAD_PARAM_VALUE =
      of(builder()
          .withIdentifier(buildFromStringRepresentation(MODULE_SIMPLE_NAMESPACE_IN_APP + ":" + SET_PAYLOAD_PARAM_VALUE_NAME))
          .withType(OPERATION).build());
  private static final DefaultComponentLocation OPERATION_SET_PAYLOAD_PARAM_VALUE =
      new DefaultComponentLocation(of(SET_PAYLOAD_PARAM_VALUE_NAME),
                                   asList(new DefaultComponentLocation.DefaultLocationPart(SET_PAYLOAD_PARAM_VALUE_NAME,
                                                                                           MODULE_SET_PAYLOAD_PARAM_VALUE,
                                                                                           MODULE_SIMPLE_FILE_NAME,
                                                                                           of(19))));

  private static final String SET_PAYLOAD_TWO_TIMES_NAME = "set-payload-two-times";
  private static final Optional<TypedComponentIdentifier> MODULE_SET_PAYLOAD_TWO_TIMES =
      of(builder()
          .withIdentifier(buildFromStringRepresentation(MODULE_SIMPLE_NAMESPACE_IN_APP + ":" + SET_PAYLOAD_TWO_TIMES_NAME))
          .withType(OPERATION).build());
  private static final DefaultComponentLocation OPERATION_SET_PAYLOAD_TWO_TIMES =
      new DefaultComponentLocation(of(SET_PAYLOAD_TWO_TIMES_NAME),
                                   asList(new DefaultComponentLocation.DefaultLocationPart(SET_PAYLOAD_TWO_TIMES_NAME,
                                                                                           MODULE_SET_PAYLOAD_TWO_TIMES,
                                                                                           MODULE_SIMPLE_FILE_NAME,
                                                                                           of(29))));

  /**
   * "module-simple-proxy" operations defined below
   */
  private static final String MODULE_SIMPLE_PROXY_NAMESPACE_IN_APP = "module-simple-proxy";
  private static final String PROXY_SET_PAYLOAD_NAME = "proxy-set-payload-hardcoded-value";
  private static final Optional<TypedComponentIdentifier> MODULE_PROXY_SET_PAYLOAD =
      of(builder()
          .withIdentifier(buildFromStringRepresentation(MODULE_SIMPLE_PROXY_NAMESPACE_IN_APP + ":" + PROXY_SET_PAYLOAD_NAME))
          .withType(OPERATION).build());
  private static final DefaultComponentLocation OPERATION_PROXY_SET_PAYLOAD =
      new DefaultComponentLocation(of(PROXY_SET_PAYLOAD_NAME),
                                   asList(new DefaultComponentLocation.DefaultLocationPart(PROXY_SET_PAYLOAD_NAME,
                                                                                           MODULE_PROXY_SET_PAYLOAD,
                                                                                           MODULE_SIMPLE_PROXY_FILE_NAME,
                                                                                           of(13))));

  private static final String PROXY_SET_PAYLOAD_AND_LOGGER_NAME = "proxy-set-payload-hardcoded-value-and-logger";
  private static final Optional<TypedComponentIdentifier> MODULE_PROXY_SET_PAYLOAD_AND_LOGGER =
      of(builder().withIdentifier(buildFromStringRepresentation(MODULE_SIMPLE_PROXY_NAMESPACE_IN_APP + COLON_SEPARATOR
          + PROXY_SET_PAYLOAD_AND_LOGGER_NAME))
          .withType(OPERATION).build());
  private static final DefaultComponentLocation OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER =
      new DefaultComponentLocation(of(PROXY_SET_PAYLOAD_AND_LOGGER_NAME),
                                   asList(new DefaultComponentLocation.DefaultLocationPart(PROXY_SET_PAYLOAD_AND_LOGGER_NAME,
                                                                                           MODULE_PROXY_SET_PAYLOAD_AND_LOGGER,
                                                                                           MODULE_SIMPLE_PROXY_FILE_NAME,
                                                                                           of(20))));

  /**
   * runtime provided MPs
   */
  private static final Optional<TypedComponentIdentifier> LOGGER =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:logger"))
          .withType(PROCESSOR).build());
  private static final Optional<TypedComponentIdentifier> SET_PAYLOAD =
      of(builder().withIdentifier(buildFromStringRepresentation("mule:set-payload"))
          .withType(PROCESSOR).build());

  final ProcessorNotificationStore listener = new ProcessorNotificationStore();

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    listener.setLogSingleNotification(true);
    muleContext.getNotificationManager().addListener(listener);
  }

  @Test
  public void flowWithSingleMp() throws Exception {
    //simple test to be sure the macro expansion doesn't mess up the a flow that has no modifications
    flowRunner("flowWithSingleMp").run();
    assertNextProcessorLocationIs(FLOW_WITH_SINGLE_MP_LOCATION
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(16)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadHardcoded() throws Exception {
    flowRunner("flowWithSetPayloadHardcoded").run();
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(14)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadHardcodedTwice() throws Exception {
    flowRunner("flowWithSetPayloadHardcodedTwice").run();
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(14)));

    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(14)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadParamValue() throws Exception {
    flowRunner("flowWithSetPayloadParamValue").run();
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_PARAM_VALUE.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_PARAM_VALUE, MODULE_SIMPLE_FILE_NAME, of(19)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_PARAM_VALUE.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_PARAM_VALUE, MODULE_SIMPLE_FILE_NAME, of(19))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(24)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadTwoTimes() throws Exception {
    flowRunner("flowWithSetPayloadTwoTimes").run();
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, of(29)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, of(29))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(31)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, of(29))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(32)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithSetPayloadTwoTimesTwice() throws Exception {
    flowRunner("flowWithSetPayloadTwoTimesTwice").run();
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, of(29)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, of(29))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(31)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, of(29))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(32)));
    //assertion on the second call of the OP
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, of(29)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, of(29))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(31)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_TWO_TIMES.appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_TWO_TIMES, MODULE_SIMPLE_FILE_NAME, of(29))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(32)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxySetPayloadHardcoded() throws Exception {
    flowRunner("flowWithProxySetPayloadHardcoded").run();
    //flow assertion
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD, MODULE_SIMPLE_PROXY_FILE_NAME, of(13)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(14)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxySetPayloadHardcodedAndLogger() throws Exception {
    flowRunner("flowWithProxySetPayloadHardcodedAndLogger").run();
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(20)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(14)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(20))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(23)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxyAndSimpleModuleAndLogger() throws Exception {
    flowRunner("flowWithProxyAndSimpleModuleAndLogger").run();
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(20)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(14)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(20))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(23)));

    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(14)));

    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("2", LOGGER, CONFIG_FILE_NAME, of(52)));
    assertNoNextProcessorNotification();
  }

  @Test
  public void flowWithProxyAndSimpleModuleAndLoggerReverse() throws Exception {
    flowRunner("flowWithProxyAndSimpleModuleAndLoggerReverse").run();
    assertNextProcessorLocationIs(FLOW_WITH_PROXY_AND_SIMPLE_MODULE_AND_LOGGER_REVERSE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", LOGGER, CONFIG_FILE_NAME, of(56)));

    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(14)));

    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("2", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(20)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12)));
    assertNextProcessorLocationIs(OPERATION_SET_PAYLOAD_HARDCODED_VALUE
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", MODULE_SET_PAYLOAD_HARDCODED_VALUE, MODULE_SIMPLE_FILE_NAME, of(12))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("0", SET_PAYLOAD, MODULE_SIMPLE_FILE_NAME, of(14)));
    assertNextProcessorLocationIs(OPERATION_PROXY_SET_PAYLOAD_AND_LOGGER
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("2", MODULE_PROXY_SET_PAYLOAD_AND_LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(20))
        .appendLocationPart("processors", empty(), empty(), empty())
        .appendLocationPart("1", LOGGER, MODULE_SIMPLE_PROXY_FILE_NAME, of(23)));
    assertNoNextProcessorNotification();
  }


  private void assertNoNextProcessorNotification() {
    Iterator iterator = listener.getNotifications().iterator();
    Assert.assertThat(iterator.hasNext(), is(false));
  }

  private void assertNextProcessorLocationIs(DefaultComponentLocation componentLocation) {
    Assert.assertThat(listener.getNotifications().isEmpty(), is(false));
    MessageProcessorNotification processorNotification =
        (MessageProcessorNotification) listener.getNotifications().get(0);
    listener.getNotifications().remove(0);
    Assert.assertThat(processorNotification.getComponentLocation().getLocation(), is(componentLocation.getLocation()));
    Assert.assertThat(processorNotification.getComponentLocation(), is(componentLocation));
  }

  private String[] getModulePaths() {
    return new String[] {BASE_PATH_XML_MODULES + MODULE_SIMPLE_XML,
        BASE_PATH_XML_MODULES + MODULE_SIMPLE_PROXY_XML};
  }

  // TODO(fernandezlautaro): MULE-10982 implement a testing framework for XML based connectors
  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(0, new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        DefaultExtensionManager extensionManager;
        if (muleContext.getExtensionManager() == null) {
          extensionManager = new DefaultExtensionManager();
          ((DefaultMuleContext) muleContext).setExtensionManager(extensionManager);
        }
        extensionManager = (DefaultExtensionManager) muleContext.getExtensionManager();
        initialiseIfNeeded(extensionManager, muleContext);

        registerXmlExtensions(extensionManager);
      }

      private void registerXmlExtensions(DefaultExtensionManager extensionManager) {
        final Set<ExtensionModel> extensions = new HashSet<>();
        for (String modulePath : getModulePaths()) {
          Map<String, Object> params = new HashMap<>();
          params.put(XmlExtensionModelLoader.RESOURCE_XML, modulePath);
          final DslResolvingContext dslResolvingContext = getDefault(extensions);
          final ExtensionModel extensionModel =
              new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), dslResolvingContext, params);
          extensions.add(extensionModel);
        }
        for (ExtensionModel extension : extensions) {
          extensionManager.registerExtension(extension);
        }
      }
    });
  }
}
