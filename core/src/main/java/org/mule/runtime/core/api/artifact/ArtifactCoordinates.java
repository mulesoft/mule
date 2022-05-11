/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.artifact;

/**
 * The coordinates that identify an artifact, in the form of a Maven GAV
 *
 * @since 4.5.0
 * @deprecated moved to {@link org.mule.runtime.api.artifact.ArtifactCoordinates}
 */
@Deprecated
public interface ArtifactCoordinates extends org.mule.runtime.api.artifact.ArtifactCoordinates {
}
