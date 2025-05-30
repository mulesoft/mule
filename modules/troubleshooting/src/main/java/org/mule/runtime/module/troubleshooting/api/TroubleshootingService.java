/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;

import java.util.List;
import java.util.Map;

/**
 * A container level object that can be used to execute troubleshooting related operations.
 *
 * @since 4.5
 */
@NoImplement
@Experimental
public interface TroubleshootingService {

  /**
   * Gets the available operation definitions. User should retrieve these definitions to know what are the possible parameters for
   * {@link #executeOperation(String, Map)}.
   *
   * @return the available operation definitions.
   */
  List<TroubleshootingOperationDefinition> getAvailableOperations();

  /**
   * Invokes all registered operations with the given parameters. User must respect the definitions retrieved with
   * {@link #getAvailableOperations()}
   *
   * @param arguments A dictionary with the arguments.
   * @return the return value of the operation.
   * @throws TroubleshootingOperationException if it couldn't execute the operation because it wasn't available or there is an
   *                                           error in the arguments.
   */
  String executeAllOperations(Map<String, String> arguments) throws TroubleshootingOperationException;

  /**
   * Invokes an operation with the given parameters. User must respect the definitions retrieved with
   * {@link #getAvailableOperations()}
   *
   * @param name      The name of the operation to execute.
   * @param arguments A dictionary with the arguments.
   * @return the return value of the operation.
   * @throws TroubleshootingOperationException if it couldn't execute the operation because it wasn't available or there is an
   *                                           error in the arguments.
   */
  String executeOperation(String name, Map<String, String> arguments) throws TroubleshootingOperationException;

  /**
   * Registers a new {@link TroubleshootingOperation}.
   * 
   * @param operation the operation to be registered.
   */
  void registerOperation(TroubleshootingOperation operation);

  /**
   * Unregisters a new {@link TroubleshootingOperation}.
   * 
   * @param name the name of the operation to be registered.
   */
  void unregisterOperation(String name);
}
