/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.reboot;

import static org.mule.runtime.jpms.api.JpmsUtils.validateNoBootModuleLayerTweaking;
import static org.mule.runtime.module.reboot.internal.MuleContainerWrapperProvider.getMuleContainerWrapper;

import org.mule.runtime.module.reboot.internal.MuleContainerFactory;
import org.mule.runtime.module.reboot.internal.MuleContainerWrapper;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 * Determine which is the main class to run and delegate control to the Java Service Wrapper. If OSGi is not being used to boot
 * with, configure the classpath based on the libraries in $MULE_HOME/lib/*
 * <p/>
 * Note: this class is intentionally kept free of any external library dependencies and therefore repeats a few utility methods.
 */
public class MuleContainerBootstrap {

  public static final String MULE_HOME_DIRECTORY_PROPERTY = "mule.home";
  public static final String MULE_BASE_DIRECTORY_PROPERTY = "mule.base";

  public static final String[][] CLI_OPTIONS = {{"main", "true", "Main Class"},
      {"production", "false", "Modify the system class loader for production use (as in Mule 2.x)"},
      {"version", "false", "Show product and version information"}};

  public static void main(String[] args) throws Exception {
    MuleContainerFactory muleContainerFactory =
        new MuleContainerFactory(MULE_HOME_DIRECTORY_PROPERTY, MULE_BASE_DIRECTORY_PROPERTY);
    MuleContainerWrapper muleContainerWrapper = getMuleContainerWrapper();

    // TODO W-12412001: move this into a configurer
    // Optionally remove existing handlers attached to j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)

    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    SLF4JBridgeHandler.install();

    // TODO W-12412001: move this into a configurer
    validateNoBootModuleLayerTweaking();

    CommandLine commandLine;
    try {
      // Parse any command line options based on the list above.
      commandLine = parseCommandLine(args);
    } catch (Exception ex) {
      muleContainerWrapper.haltAndCatchFire(1, ex.getMessage());
      return;
    }

    muleContainerWrapper.configureAndStart(muleContainerFactory, commandLine);
  }

  /**
   * Parse any command line arguments using the Commons CLI library.
   */
  private static CommandLine parseCommandLine(String[] args) throws ParseException {
    Options options = new Options();
    for (String[] element : CLI_OPTIONS) {
      options.addOption(element[0], "true".equalsIgnoreCase(element[1]), element[2]);
    }
    return new BasicParser().parse(options, args, true);
  }
}
