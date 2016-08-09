/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.descriptor;

import java.io.File;
import java.io.IOException;

/**
 * Parses an artifact descriptor
 */
public interface DescriptorParser<D extends DeployableArtifactDescriptor> {

  D parse(File descriptor, String artifactName) throws IOException;
}
