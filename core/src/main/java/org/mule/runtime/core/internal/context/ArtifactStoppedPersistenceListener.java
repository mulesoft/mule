/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context;

/**
 * Defines a listener to persist stop events of Mule artifacts.
 */
public interface ArtifactStoppedPersistenceListener {

  String ARTIFACT_STOPPED_LISTENER = "artifactStoppedPersistenceListener";

  /**
   * Notifies an artifact has been started.
   */
  void onStart();

  /**
   * Notifies an artifact has been stopped.
   */
  void onStop();

  /**
   * Turns off persistence.
   * <p>
   * The artifact's stopped state should only be persisted if it was stopped by external users. Since external users usually call
   * the artifact's stop() method directly from their own methods, a workaround is to prevent persistence when the artifact is
   * stopped for other reasons.
   */
  void doNotPersist();

  /**
   * Deletes stopped persistence properties
   */
  void deletePersistenceProperties();
}
