/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;

import static java.lang.String.format;

/**
 * {@link IllegalStateException} implementation that aims to be thrown when no configuration reference was specified for a
 * component that requires one.
 *
 * @since 4.2
 */
public class NoConfigRefFoundException extends IllegalStateException {

  NoConfigRefFoundException(ExtensionModel extension, ComponentModel component) {
    super(format("No config-ref was specified for component '%s' of extension '%s'. Please specify which to use",
                 component.getName(), extension.getName()));
  }
}
