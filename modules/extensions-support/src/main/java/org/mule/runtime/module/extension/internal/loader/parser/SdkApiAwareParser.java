/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

/**
 * Contract for a model parser with information on whether the component was written with the extensions-api or sdk-api
 *
 * @since 4.8.0
 */
public interface SdkApiAwareParser {

  boolean isSdkApiDefined();
}
