/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model;

import org.mule.api.annotation.NoImplement;

/**
 * Represents a generic resource provider, to be used instead of the artifact class loader.
 *
 * @deprecated since 4.4, use org.mule.runtime.properties.api.ResourceProvider instead.
 */
@NoImplement
@Deprecated
public interface ResourceProvider extends org.mule.runtime.properties.api.ResourceProvider {

}
