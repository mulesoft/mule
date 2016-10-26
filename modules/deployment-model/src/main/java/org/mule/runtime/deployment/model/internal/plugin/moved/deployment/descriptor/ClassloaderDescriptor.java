/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.plugin.moved.deployment.descriptor;

import org.mule.runtime.deployment.model.api.plugin.moved.deployment.DeploymentModel;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.MalformedDeploymentModelException;

import java.net.URL;
import java.util.Map;

/**
 * Represents a TODO-REMOVE_OR_COMPLETE
 *
 * @since 4.0
 */
public interface ClassloaderDescriptor {

  String getId();

  DeploymentModel load(URL location, Map<String, Object> attributes) throws MalformedDeploymentModelException;
}
