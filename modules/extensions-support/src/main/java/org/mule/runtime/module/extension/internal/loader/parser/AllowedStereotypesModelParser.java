package org.mule.runtime.module.extension.internal.loader.parser;

import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;

import java.util.List;

public interface AllowedStereotypesModelParser {

  List<StereotypeModel> getAllowedStereotypes(StereotypeModelFactory factory);
}
