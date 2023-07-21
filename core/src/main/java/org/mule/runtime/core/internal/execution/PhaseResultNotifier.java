/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.execution;

/**
 * Notifier used by {@link MessageProcessPhase} in order to define the result of the phase execution
 */
public interface PhaseResultNotifier {

  /**
   * This method must be called when the phase complete successfully
   */
  void phaseSuccessfully();

  /**
   * This message must be called when a phase execution throw an exception
   *
   * @param reason exception that represents the failure in the phase
   */
  void phaseFailure(Exception reason);
}
