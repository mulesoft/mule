/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.parseRepeatableAnnotation;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.ImportedTypes;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;

import java.util.List;
import java.util.Optional;

/**
 * Test the extension type to be annotated with {@link Import}, in which case it adds an {@link ImportedTypeModel} on the
 * extension level.
 *
 * @since 4.0
 */
public final class ImportedTypesDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    ExtensionDeclarer descriptor = extensionLoadingContext.getExtensionDeclarer();
    ExtensionDeclaration extensionDeclaration = descriptor.getDeclaration();

    final Optional<ImplementingTypeModelProperty> implementingType = extractImplementingTypeProperty(extensionDeclaration);
    if (!implementingType.isPresent()) {
      return;
    }
    final Class<?> type = implementingType.get().getType();
    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(type.getClassLoader());

    final List<Import> importTypes = parseRepeatableAnnotation(type, Import.class, c -> ((ImportedTypes) c).value());

    if (!importTypes.isEmpty()) {
      if (importTypes.stream().map(Import::type).distinct().collect(toList()).size() != importTypes.size()) {
        throw new IllegalModelDefinitionException(
                                                  format("There should be only one Import declaration for any given type in extension [%s]."
                                                      + " Multiple imports of the same type are not allowed",
                                                         extensionDeclaration.getName()));
      }

      importTypes.forEach(imported -> {
        MetadataType importedType = typeLoader.load(imported.type());

        if (!(importedType instanceof ObjectType)) {
          throw new IllegalArgumentException(format("Type '%s' is not complex. Only complex types can be imported from other extensions.",
                                                    type.getTypeName()));
        }

        extensionDeclaration
            .addImportedType(new ImportedTypeModel((ObjectType) typeLoader.load(imported.type())));
      });
    }
  }
}
