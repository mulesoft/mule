/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
