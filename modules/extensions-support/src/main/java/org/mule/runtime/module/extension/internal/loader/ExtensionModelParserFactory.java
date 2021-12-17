package org.mule.runtime.module.extension.internal.loader;

import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionModelParser;

public interface ExtensionModelParserFactory {

  ExtensionModelParser createParser(ExtensionLoadingContext context);


}
