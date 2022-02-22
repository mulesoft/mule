package org.mule.runtime.core.api.artifact;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.xml.AstXmlParser;

import java.util.Set;

@FunctionalInterface
public interface AstXmlParserSupplier {

  AstXmlParser getParser(Set<ExtensionModel> extensions, boolean disableValidations);
}
