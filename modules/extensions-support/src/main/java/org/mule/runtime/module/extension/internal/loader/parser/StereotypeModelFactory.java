/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.sdk.api.stereotype.StereotypeDefinition;

public interface StereotypeModelFactory {

  StereotypeModel createStereotype(StereotypeDefinition stereotypeDefinition);

  StereotypeModel createStereotype(String name, StereotypeModel parent);

  StereotypeModel getProcessorParentStereotype();

  StereotypeModel getSourceParentStereotype();

  StereotypeModel getValidatorStereotype();
}
