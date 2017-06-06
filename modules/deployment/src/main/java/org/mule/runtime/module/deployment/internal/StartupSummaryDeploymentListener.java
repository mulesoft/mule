/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.core.internal.util.splash.SimpleLoggingTable;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.StartupListener;
import org.mule.runtime.deployment.model.api.application.Application;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Prints application status summary table on Mule startup.
 */
public class StartupSummaryDeploymentListener implements StartupListener {

  protected transient final Logger logger = LoggerFactory.getLogger(getClass());

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

  public void onAfterStartup() {
    if (!logger.isInfoEnabled()) {
      return;
    }

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

      message = String.format("%n%s%n%s", domainTable, applicationTable);
    } else {
      message = String.format("%n%s", domainTable);
    }

    logger.info(message);
  }
}
