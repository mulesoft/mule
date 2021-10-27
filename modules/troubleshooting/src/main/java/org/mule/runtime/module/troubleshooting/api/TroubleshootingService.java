/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.NoImplement;

import java.util.List;
import java.util.Map;

@NoImplement
public interface TroubleshootingService {

  List<TroubleshootingOperationDefinition> getAvailableOperations();

  Object executeOperation(String name, Map<String, String> arguments);
}
