/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;

/**
 * Indicates that the classes that implement this interface rely on an ExtensionParameter
 *
 * @since 4.5
 */
public interface HasExtensionParameter {

  ExtensionParameter getExtensionParameter();

}
