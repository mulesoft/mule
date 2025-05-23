/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal.operations;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

import static org.apache.commons.lang3.StringUtils.leftPad;

import org.mule.runtime.api.alert.TimedDataAggregation;
import org.mule.runtime.core.api.alert.MuleAlertingSupport;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultArgumentDefinition;
import org.mule.runtime.module.troubleshooting.internal.DefaultTroubleshootingOperationDefinition;

import java.io.IOException;
import java.io.Writer;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Operation used to collect a summary of the alerts for predefined time intervals.
 * <p>
 * The name of the operation is "alertFuseboard".
 * <p>
 * Arguments:
 * <ul>
 * <li>application (Optional): The application to collect the alert fuseboard from</li>
 * </ul>
 */
public class AlertFuseboardOperation implements TroubleshootingOperation {

  public static final String ALERT_FUSEBOARD_OPERATION_NAME = "alertFuseboard";
  public static final String ALERT_FUSEBOARD_OPERATION_DESCRIPTION =
      "Collects a summary of the alerts for predefined time intervals";

  public static final String APPLICATION_ARGUMENT_NAME = "application";
  public static final String APPLICATION_ARGUMENT_DESCRIPTION = "Application to collect the event dump from";

  private static final TroubleshootingOperationDefinition definition = createOperationDefinition();

  private final DeploymentService deploymentService;

  public AlertFuseboardOperation(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  @Override
  public TroubleshootingOperationDefinition getDefinition() {
    return definition;
  }

  @Override
  public TroubleshootingOperationCallback getCallback() {
    return (arguments, writer) -> {
      final String applicationName = arguments.get(APPLICATION_ARGUMENT_NAME);
      if (applicationName == null) {
        writeAlertFuseboardsForAllApplications(writer);
      } else {
        Application application = deploymentService.findApplication(applicationName);
        writeAlertFuseboardEntries(application, writer);
      }
    };
  }

  private static void writeAlertFuseboardFor(Application application, Writer writer)
      throws IOException {
    final var appsTitle = "Alert Fuseboard for application '" + application.getArtifactName() + "'";
    writer.write(appsTitle + lineSeparator());
    writer.write(leftPad("", appsTitle.length(), "-") + lineSeparator());
    writer.write(lineSeparator());

    writeAlertFuseboardEntries(application, writer);
  }

  private void writeAlertFuseboardsForAllApplications(Writer writer) throws IOException {
    for (Application application : deploymentService.getApplications()) {
      writeAlertFuseboardFor(application, writer);
    }
  }

  private static void writeAlertFuseboardEntries(Application application, Writer writer) throws IOException {
    MuleAlertingSupport muleAlertingSupport = application
        .getArtifactContext()
        .getRegistry()
        .lookupByType(MuleAlertingSupport.class)
        .orElseThrow(() -> new IllegalArgumentException(format("Could not get MuleAlertingSupport for application %s.",
                                                               application.getArtifactName())));

    final var alertSummary = muleAlertingSupport.alertsCountAggregation();
    if (alertSummary.isEmpty()) {
      writer.write("  No alerts triggered during the last hour.");
      writer.write(lineSeparator());
      return;
    }

    final Optional<Integer> maxAlertNameLength = alertSummary.keySet().stream()
        .map(String::length)
        .max((o1, o2) -> o1 - o2);
    String formatString = "  * %-" + maxAlertNameLength.orElse(30) + "s    %3d / %3d / %4d / %4d%n";

    for (Entry<String, TimedDataAggregation<Integer>> alertData : alertSummary.entrySet()) {
      writer.write(formatString
          .formatted(alertData.getKey(),
                     alertData.getValue().forLast1MinInterval(),
                     alertData.getValue().forLast5MinsInterval(),
                     alertData.getValue().forLast15MinsInterval(),
                     alertData.getValue().forLast60MinsInterval()));
    }
    writer.write(lineSeparator());
  }

  private static TroubleshootingOperationDefinition createOperationDefinition() {
    return new DefaultTroubleshootingOperationDefinition(ALERT_FUSEBOARD_OPERATION_NAME, ALERT_FUSEBOARD_OPERATION_DESCRIPTION,
                                                         createApplicationArgumentDefinition());
  }

  private static ArgumentDefinition createApplicationArgumentDefinition() {
    return new DefaultArgumentDefinition(APPLICATION_ARGUMENT_NAME, APPLICATION_ARGUMENT_DESCRIPTION, false);
  }
}
