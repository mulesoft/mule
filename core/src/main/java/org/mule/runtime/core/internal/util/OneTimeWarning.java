/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.slf4j.Logger;

/**
 * Utility class to log a warning message only once during the lifecycle of a component. The {@link #warn()} message will log that
 * message only once, in a thread-safe manner. This component is conceived with the intent of allowing deprecated components to
 * warn that they should not be used anymore.
 *
 * @since 3.6.0
 * @deprecated Use {@link org.mule.runtime.core.internal.util.log.OneTimeWarning} instead
 */
@Deprecated
public class OneTimeWarning extends org.mule.runtime.core.internal.util.log.OneTimeWarning {

  public OneTimeWarning(Logger logger, String message) {
    super(logger, message);
  }

}
