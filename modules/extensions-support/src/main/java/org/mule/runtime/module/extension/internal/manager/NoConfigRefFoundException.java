/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.manager;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;

import static java.lang.String.format;

public class NoConfigRefFoundException extends IllegalStateException {

  NoConfigRefFoundException(ExtensionModel extension, ComponentModel component) {
    super(format("No config-ref was specified for component '%s' of extension '%s'. Please specify which to use",
                 component.getName(), extension.getName()));
  }
}
