/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.application;

import org.mule.api.annotation.NoImplement;

/**
 * Identifies a classLoader created for a given Mule application
 *
 * @deprecated no longer used in Mule Runtime
 */
@NoImplement
@Deprecated(forRemoval = true, since = "4.10")
public interface ApplicationClassLoader {
}
