/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.config;

import org.mule.api.annotation.NoImplement;

/**
 * Marker interface for custom configuration extensions defined inside {@link MuleConfiguration}
 *
 * This is required by the new parsers to identify child elements
 *
 * @since 4.0
 */
@NoImplement
public interface ConfigurationExtension {

}
