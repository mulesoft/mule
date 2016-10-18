/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.introspection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;

/**
 * The purpose of this class is to find all the schema URLs, both local or remote, for a given WSDL definition. This includes
 * imports and includes in the WSDL file and recursively in each schema found.
 *
 * @since 4.0
 */
@SuppressWarnings("unchecked")
final class WsdlSchemaCollector {

  Set<String> getSchemas(Definition wsdlDefinition) {
    Set<String> schemas = new LinkedHashSet<>();

    Types types = wsdlDefinition.getTypes();
    if (types != null) {
      types.getExtensibilityElements().forEach(element -> {
        if (element instanceof Schema) {
          Schema schema = (Schema) element;
          schemas.add(schema.getDocumentBaseURI());
          schemas.addAll(getSchemaImports(schema));
        }
      });
    }

    wsdlDefinition.getImports().values().forEach(wsdlImport -> {
      Definition definition = ((Import) wsdlImport).getDefinition();
      schemas.addAll(getSchemas(definition));
    });

    return schemas;
  }

  private List<String> getSchemaImports(Schema schema) {
    List<String> schemas = new ArrayList<>();
    Collection imports = schema.getImports().values();
    imports.forEach(i -> {
      Vector vector = (Vector) i;
      vector.forEach(element -> schemas.add(((SchemaImport) element).getSchemaLocationURI()));
    });
    return schemas;
  }
}
