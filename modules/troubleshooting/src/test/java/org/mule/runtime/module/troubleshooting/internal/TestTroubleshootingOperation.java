/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.internal;

import static java.lang.String.format;

import org.mule.runtime.module.troubleshooting.api.ArgumentDefinition;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperation;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationCallback;
import org.mule.runtime.module.troubleshooting.api.TroubleshootingOperationDefinition;

public class TestTroubleshootingOperation implements TroubleshootingOperation {

  private static final TroubleshootingOperationDefinition definition = createDefinition();
  static final String TEST_OPERATION_NAME = "test";
  private static final String TEST_OPERATION_DESCRIPTION = "This is a test troubleshooting operation";

  static final String REQUIRED_ARGUMENT_NAME = "required";
  private static final String REQUIRED_ARGUMENT_DESCRIPTION = "This is a required argument";

  private static final String OPTIONAL_ARGUMENT_NAME = "optional";
  private static final String OPTIONAL_ARGUMENT_DESCRIPTION = "This is an optional argument";

  private static TroubleshootingOperationDefinition createDefinition() {
    ArgumentDefinition requiredArgument =
        new DefaultArgumentDefinition(REQUIRED_ARGUMENT_NAME, REQUIRED_ARGUMENT_DESCRIPTION, true);
    ArgumentDefinition optionalArgument =
        new DefaultArgumentDefinition(OPTIONAL_ARGUMENT_NAME, OPTIONAL_ARGUMENT_DESCRIPTION, false);
    return new DefaultTroubleshootingOperationDefinition(TEST_OPERATION_NAME, TEST_OPERATION_DESCRIPTION, requiredArgument,
                                                         optionalArgument);
  }

  @Override
  public TroubleshootingOperationDefinition getDefinition() {
    return definition;
  }

  @Override
  public TroubleshootingOperationCallback getCallback() {
    return (args, writer) -> writer.write(format("%s with these arguments: %s", TEST_OPERATION_DESCRIPTION, args));
  }
}
