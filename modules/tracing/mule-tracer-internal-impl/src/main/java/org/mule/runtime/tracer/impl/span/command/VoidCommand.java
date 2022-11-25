/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

/**
 * A command interface.
 *
 * @since 4.5.0
 */
public interface VoidCommand {

  /**
   * executes the command.
   */
  void execute();
}
