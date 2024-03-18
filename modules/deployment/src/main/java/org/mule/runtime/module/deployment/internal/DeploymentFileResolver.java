/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;

/**
 * A resolver for the deployment file.
 */
public interface DeploymentFileResolver {

  File resolve(URI appArchiveUri) throws MalformedURLException;
}
