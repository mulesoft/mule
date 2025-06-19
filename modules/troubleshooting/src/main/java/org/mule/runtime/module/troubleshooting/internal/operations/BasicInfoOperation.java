/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleBaseFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;
import static org.mule.runtime.manifest.api.MuleManifest.getMuleManifest;

import static java.lang.System.getProperties;
import static java.lang.System.getProperty;
import static java.lang.System.lineSeparator;
import static java.time.Duration.between;
import static java.time.Instant.now;
import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;

import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingOperationDefinition;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Operation used to collect basic environment and metadata information for the current Mule Runtime.
 * <p>
 * The name of the operation is "basicInfo".
 */
public class BasicInfoOperation implements TroubleshootingOperation {

  public static final String BASIC_INFO_OPERATION_NAME = "basicInfo";
  public static final String BASIC_INFO_OPERATION_DESCRIPTION =
      "Collects basic environment and metadata information for the current Mule Runtime";

  private static final TroubleshootingOperationDefinition definition = createOperationDefinition();

  @Override
  public TroubleshootingOperationDefinition getDefinition() {
    return definition;
  }

  @Override
  public TroubleshootingOperationCallback getCallback() {
    return (arguments, writer) -> {
      final var muleManifest = getMuleManifest();
      writer.write("Mule:" + lineSeparator());
      writer.write("  %s %s (build %s)".formatted(muleManifest.getProductName(),
                                                  muleManifest.getProductVersion(),
                                                  muleManifest.getBuildNumber())
          + lineSeparator());

      writer.write("  mule_home: %s".formatted(getMuleHomeFolder().getAbsolutePath())
          + lineSeparator());
      writer.write("  mule_base: %s".formatted(getMuleBaseFolder().getAbsolutePath())
          + lineSeparator());
      writer.write(lineSeparator());

      // get the properties sorted alphabetically
      writer.write("System Properties:" + lineSeparator());
      Map<String, String> muleProperties = getProperties().stringPropertyNames().stream()
          .filter(property -> property.startsWith(SYSTEM_PROPERTY_PREFIX))
          .collect(toMap(identity(), System::getProperty, (v1, v2) -> v1, TreeMap::new));
      for (Entry<String, String> entry : muleProperties.entrySet()) {
        writer.write("  %s: %s".formatted(entry.getKey(), entry.getValue()) + lineSeparator());
      }

      writer.write(lineSeparator());

      writer.write("Java:" + lineSeparator());
      writer.write("  Version:   %s".formatted(getProperty("java.version")) + lineSeparator());
      writer.write("  Vendor:    %s".formatted(getProperty("java.vendor")) + lineSeparator());
      writer.write("  VM name:   %s".formatted(getProperty("java.vm.name")) + lineSeparator());
      writer.write("  JAVA_HOME: %s".formatted(getProperty("java.home")) + lineSeparator());

      writer.write(lineSeparator());
      writer.write("OS:" + lineSeparator());
      writer.write("  Name:      %s".formatted(getProperty("os.name")) + lineSeparator());
      writer.write("  Version:   %s".formatted(getProperty("os.version")) + lineSeparator());
      writer.write("  Arch:      %s".formatted(getProperty("os.arch")) + lineSeparator());

      writer.write(lineSeparator());
      final var runningTime = ProcessHandle.current()
          .info()
          .startInstant()
          .map(i -> between(i, now()))
          .map(d -> formatDuration(d.toMillis(), "d'd' HH:mm:ss.SSS"))
          .orElse("n/a");
      writer.write("Running time: %s".formatted(runningTime));
      writer.write(lineSeparator());

      writer.write(lineSeparator());
      writer.write("Process Information:" + lineSeparator());
      writer.write("  PID: %d".formatted(ProcessHandle.current().pid()) + lineSeparator());

      writer.write(lineSeparator());
      writer.write("Report Generation:" + lineSeparator());
      writer.write("  Report Millis Time: %d".formatted(System.currentTimeMillis()) + lineSeparator());
      writer.write("  Report Nano Time: %d".formatted(System.nanoTime()) + lineSeparator());
    };
  }

  private static TroubleshootingOperationDefinition createOperationDefinition() {
    return new DefaultTroubleshootingOperationDefinition(BASIC_INFO_OPERATION_NAME, BASIC_INFO_OPERATION_DESCRIPTION);
  }

}
