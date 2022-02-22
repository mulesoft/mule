/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.artifact;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.xml.AstXmlParser;

import java.util.Set;

@FunctionalInterface
public interface AstXmlParserSupplier {

  AstXmlParser getParser(Set<ExtensionModel> extensions, boolean disableValidations);
}
