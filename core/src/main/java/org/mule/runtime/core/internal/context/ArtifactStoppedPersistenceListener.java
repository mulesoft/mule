/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
