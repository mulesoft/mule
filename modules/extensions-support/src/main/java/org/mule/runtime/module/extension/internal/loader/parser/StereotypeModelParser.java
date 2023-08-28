/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;

import java.util.Optional;

public interface StereotypeModelParser {

  Optional<StereotypeModel> getStereotype(StereotypeModelFactory factory);

}
