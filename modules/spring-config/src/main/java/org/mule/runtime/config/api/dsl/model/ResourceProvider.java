/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
