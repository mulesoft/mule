/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.internal.util.splash.SimpleLoggingTable;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.StartupListener;

import java.util.Map;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import org.slf4j.Logger;

/**
 * Prints application status summary table on Mule startup.
 */
public class StartupSummaryDeploymentListener implements StartupListener {

  private static final Logger SPLASH_LOGGER = getLogger("org.mule.runtime.core.internal.logging");

  private static final String APPLICATION_LABEL = "APPLICATION";
  private static final String DOMAIN_OWNER_LABEL = "DOMAIN";
  private static final String STATUS_LABEL = "STATUS";
  private static final int ARTIFACT_NAME_LABEL_LENGTH = 45;
  private static final int STATUS_LABEL_LENGTH = 18;
  private static final int DOMAIN_OWNER_LABEL_LENGTH = 30;

  private static final String UNKNOWN_ARTIFACT_NAME = "UNKNOWN";

  private final DeploymentService deploymentService;

  protected DeploymentStatusTracker tracker;

  public StartupSummaryDeploymentListener(DeploymentStatusTracker tracker, DeploymentService deploymentService) {
    this.tracker = tracker;
    this.deploymentService = deploymentService;
  }

  @Override
  public void onAfterStartup() {
    Multimap<String, String> applicationsPerDomain = LinkedListMultimap.create();

    Map<String, ArtifactDeploymentStatusTracker.DeploymentState> domainDeploymentState =
        tracker.getDomainDeploymentStatusTracker().getDeploymentStates();

    SimpleLoggingTable domainTable = new SimpleLoggingTable();
    domainTable.addColumn(DOMAIN_OWNER_LABEL, ARTIFACT_NAME_LABEL_LENGTH);
    domainTable.addColumn(STATUS_LABEL, STATUS_LABEL_LENGTH);

    for (String domain : domainDeploymentState.keySet()) {
      String[] data = new String[] {domain, domainDeploymentState.get(domain).toString()};
      domainTable.addDataRow(data);
    }

    Map<String, ArtifactDeploymentStatusTracker.DeploymentState> applicationStates =
        tracker.getApplicationDeploymentStatusTracker().getDeploymentStates();

    for (String applicationName : applicationStates.keySet()) {
      Application application = deploymentService.findApplication(applicationName);
      String domainName = UNKNOWN_ARTIFACT_NAME;
      if (application != null) {
        domainName = application.getDomain().getArtifactName();
      }
      applicationsPerDomain.put(domainName, applicationName);
    }

    String message;

    if (!applicationsPerDomain.isEmpty()) {
      SimpleLoggingTable applicationTable = new SimpleLoggingTable();
      applicationTable.addColumn(APPLICATION_LABEL, ARTIFACT_NAME_LABEL_LENGTH);
      applicationTable.addColumn(DOMAIN_OWNER_LABEL, DOMAIN_OWNER_LABEL_LENGTH);
      applicationTable.addColumn(STATUS_LABEL, STATUS_LABEL_LENGTH);

      for (String domainName : applicationsPerDomain.keySet()) {
        for (String app : applicationsPerDomain.get(domainName)) {
          String[] data = new String[] {app, domainName, applicationStates.get(app).toString()};
          applicationTable.addDataRow(data);
        }
      }

      message = format("%n%s%n%s", domainTable, applicationTable);
    } else {
      message = format("%n%s", domainTable);
    }

    SPLASH_LOGGER.info(message);
  }
}
