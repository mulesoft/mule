/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.classification;

import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link RepositoryListener} that logs events from repository system.
 *
 * @since 4.0
 */
public class LoggerRepositoryListener extends AbstractRepositoryListener {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Override
  public void artifactDeployed(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Deployed {} to {}", event.getArtifact(), event.getRepository());
    }
  }

  @Override
  public void artifactDeploying(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Deploying {} to {}", event.getArtifact(), event.getRepository());
    }
  }

  @Override
  public void artifactDescriptorInvalid(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Invalid artifact descriptor for {}: {}", event.getArtifact(), event.getException().getMessage());
    }
  }

  @Override
  public void artifactDescriptorMissing(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Missing artifact descriptor for {}", event.getArtifact());
    }
  }

  @Override
  public void artifactInstalled(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Installed {} to {}", event.getArtifact(), event.getFile());
    }
  }

  @Override
  public void artifactInstalling(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Installing {} to {}", event.getArtifact(), event.getFile());
    }
  }

  @Override
  public void artifactResolved(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Resolved artifact {} from {}", event.getArtifact(), event.getRepository());
    }
  }

  @Override
  public void artifactDownloading(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Downloading artifact {} from {}", event.getArtifact(), event.getRepository());
    }
  }

  @Override
  public void artifactDownloaded(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Downloaded artifact {} from {}", event.getArtifact(), event.getRepository());
    }
  }

  @Override
  public void artifactResolving(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Resolving artifact {}", event.getArtifact());
    }
  }

  @Override
  public void metadataDeployed(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Deployed {} to {}", event.getMetadata(), event.getRepository());
    }
  }

  @Override
  public void metadataDeploying(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Deploying {} to {}", event.getMetadata(), event.getRepository());
    }
  }

  @Override
  public void metadataInstalled(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Installed {} to {}", event.getMetadata(), event.getFile());
    }
  }

  @Override
  public void metadataInstalling(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Installing {} to {}", event.getMetadata(), event.getFile());
    }
  }

  @Override
  public void metadataInvalid(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Invalid metadata {}", event.getMetadata());
    }
  }

  @Override
  public void metadataResolved(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Resolved metadata {} from {}", event.getMetadata(), event.getRepository());
    }
  }

  @Override
  public void metadataResolving(RepositoryEvent event) {
    if (logger.isTraceEnabled()) {
      logger.trace("Resolving metadata {} from {}", event.getMetadata(), event.getRepository());
    }
  }

}
