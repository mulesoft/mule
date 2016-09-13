/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classification;

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
    logger.trace("Deployed {} to {}", event.getArtifact(), event.getRepository());
  }

  @Override
  public void artifactDeploying(RepositoryEvent event) {
    logger.trace("Deploying {} to {}", event.getArtifact(), event.getRepository());
  }

  @Override
  public void artifactDescriptorInvalid(RepositoryEvent event) {
    logger.trace("Invalid artifact descriptor for {}: {}", event.getArtifact(), event.getException().getMessage());
  }

  @Override
  public void artifactDescriptorMissing(RepositoryEvent event) {
    logger.trace("Missing artifact descriptor for {}", event.getArtifact());
  }

  @Override
  public void artifactInstalled(RepositoryEvent event) {
    logger.trace("Installed {} to {}", event.getArtifact(), event.getFile());
  }

  @Override
  public void artifactInstalling(RepositoryEvent event) {
    logger.trace("Installing {} to {}", event.getArtifact(), event.getFile());
  }

  @Override
  public void artifactResolved(RepositoryEvent event) {
    logger.trace("Resolved artifact {} from {}", event.getArtifact(), event.getRepository());
  }

  @Override
  public void artifactDownloading(RepositoryEvent event) {
    logger.trace("Downloading artifact {} from {}", event.getArtifact(), event.getRepository());
  }

  @Override
  public void artifactDownloaded(RepositoryEvent event) {
    logger.trace("Downloaded artifact {} from {}", event.getArtifact(), event.getRepository());
  }

  @Override
  public void artifactResolving(RepositoryEvent event) {
    logger.trace("Resolving artifact {}", event.getArtifact());
  }

  @Override
  public void metadataDeployed(RepositoryEvent event) {
    logger.trace("Deployed {} to {}", event.getMetadata(), event.getRepository());
  }

  @Override
  public void metadataDeploying(RepositoryEvent event) {
    logger.trace("Deploying {} to {}", event.getMetadata(), event.getRepository());
  }

  @Override
  public void metadataInstalled(RepositoryEvent event) {
    logger.trace("Installed {} to {}", event.getMetadata(), event.getFile());
  }

  @Override
  public void metadataInstalling(RepositoryEvent event) {
    logger.trace("Installing {} to {}", event.getMetadata(), event.getFile());
  }

  @Override
  public void metadataInvalid(RepositoryEvent event) {
    logger.trace("Invalid metadata {}", event.getMetadata());
  }

  @Override
  public void metadataResolved(RepositoryEvent event) {
    logger.trace("Resolved metadata {} from {}", event.getMetadata(), event.getRepository());
  }

  @Override
  public void metadataResolving(RepositoryEvent event) {
    logger.trace("Resolving metadata {} from {}", event.getMetadata(), event.getRepository());
  }

}
