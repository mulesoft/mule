package org.mule.runtime.module.extension.internal.loader.parser;

import java.util.Optional;

public interface ErrorModelParser {

  String getType();

  String getNamespace();

  Optional<ErrorModelParser> getParent();
}
