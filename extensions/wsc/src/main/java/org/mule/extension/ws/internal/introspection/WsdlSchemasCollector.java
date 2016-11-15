/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.introspection;

import static java.lang.String.format;
import static org.mule.extension.ws.internal.util.TransformationUtils.nodeToString;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.extension.ws.internal.util.WscTransformationException;
import org.mule.metadata.xml.SchemaCollector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
final class WsdlSchemasCollector {

  SchemaCollector collect(Definition wsdlDefinition) {
    SchemaCollector collector = SchemaCollector.getInstance();

    collectTypes(wsdlDefinition.getTypes(), collector);

    wsdlDefinition.getImports().values().forEach(wsdlImport -> {
      Definition definition = ((Import) wsdlImport).getDefinition();
      collectTypes(definition.getTypes(), collector);
    });

    return collector;
  }

  private void collectTypes(Types types, SchemaCollector collector) {
    if (types != null) {
      types.getExtensibilityElements().forEach(element -> {
        if (element instanceof Schema) {
          Schema schema = (Schema) element;
          String schemaUri = schema.getDocumentBaseURI();
          try {
            collector.addSchema(schemaUri, nodeToString(schema.getElement()));
          } catch (WscTransformationException e) {
            throw new WscException(format("Cannot collect schema [%s], error while processing content", schemaUri), e);
          }
          getSchemaImportsUrls(schema).forEach(collector::addSchema);
        }
      });
    }
  }

  private List<String> getSchemaImportsUrls(Schema schema) {
    List<String> schemas = new ArrayList<>();
    Collection imports = schema.getImports().values();
    imports.forEach(i -> {
      Vector vector = (Vector) i;
      vector.forEach(element -> schemas.add(((SchemaImport) element).getSchemaLocationURI()));
    });
    return schemas;
  }
}
