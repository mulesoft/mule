/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.catalog.model.resolver;

import static java.lang.String.format;
import com.google.common.base.Preconditions;
import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.annotation.TypeAnnotation;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.impl.BaseMetadataType;
import org.mule.metadata.json.JsonTypeLoader;
import org.mule.metadata.xml.SchemaCollector;
import org.mule.metadata.xml.XmlTypeLoader;
import org.mule.runtime.core.api.util.IOUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single type (commonly used in JSON schemas).
 * TODO(fernandezlautaro: MULE-11501 this class must be moved to a separate module
 *
 * @since 4.0
 */
public class SingleTypeResolver implements TypeResolver {

  private static final String JSON_SUFFIX = "json";
  private static final String XSD_SUFFIX = "xsd";
  private String typeIdentifier;
  private final TypeLoader typeLoader;
  private final String element;

  public SingleTypeResolver(String typeIdentifier, URL schemaUrl, String element) {
    Preconditions.checkNotNull(typeIdentifier);
    Preconditions.checkNotNull(schemaUrl);
    this.element = element;
    typeLoader = getTypeLoader(schemaUrl);
    this.typeIdentifier = typeIdentifier;
  }

  private TypeLoader getTypeLoader(URL schemaUrl) {
    final TypeLoader loader;
    if (schemaUrl.toString().endsWith(JSON_SUFFIX)) {
      loader = new JsonTypeLoader(getSchemaData(schemaUrl));
    } else if (schemaUrl.toString().endsWith(XSD_SUFFIX)) {
      final SchemaCollector instance = SchemaCollector.getInstance();
      instance.addSchema(schemaUrl);
      loader = new XmlTypeLoader(instance);
    } else {
      throw new RuntimeException(format("The schema trying to be read [%s] is of a unknown type. Currently JSON or XML schemas are supported, and the must end with the suffix [%s] or [%s], respectively",
                                        schemaUrl.toString(),
                                        JSON_SUFFIX,
                                        XSD_SUFFIX));
    }
    return loader;
  }

  private String getSchemaData(URL schemaUrl) {
    try {
      return IOUtils.toString(schemaUrl.openStream());
    } catch (IOException e) {
      throw new RuntimeException(format("There was an issue while trying to read the schema from [%s]", schemaUrl.toString()), e);
    }
  }

  @Override
  public Optional<MetadataType> resolveType(String typeIdentifier) {
    return this.typeIdentifier.equals(typeIdentifier) ? getTypeWhileAddingIDToMakeItSerializable(typeIdentifier)
        : Optional.empty();
  }

  /**
   * TODO(fernandezlautaro): MULE-11508 this method is needed for Mozart consumption of the serialized ExtensionModel, we need to force an ID on the type or it fails when doing the ExtensionModelJsonSerializer#serialize
   * @param typeIdentifier
   * @return
   */
  private Optional<MetadataType> getTypeWhileAddingIDToMakeItSerializable(String typeIdentifier) {
    final Optional<MetadataType> load = loadMetadataTypeWithoutNPE();
    load.ifPresent(metadataType -> {
      if (metadataType instanceof ObjectType) {
        try {
          final Field annotationsField = BaseMetadataType.class.getDeclaredField("annotations");
          annotationsField.setAccessible(true);
          Map<Class<? extends TypeAnnotation>, TypeAnnotation> mapa =
              (Map<Class<? extends TypeAnnotation>, TypeAnnotation>) annotationsField.get(metadataType);
          mapa.put(TypeIdAnnotation.class, new TypeIdAnnotation(typeIdentifier));
        } catch (NoSuchFieldException | IllegalAccessException e) {
          e.printStackTrace();
          throw new RuntimeException("this code must be removed", e);
        }
      }
    });
    return load;
  }

  private Optional<MetadataType> loadMetadataTypeWithoutNPE() {
    try {
      return typeLoader.load(element);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof NullPointerException) {
        throw new RuntimeException(format(
                                          "the Smart Connector catalog's is missing the 'element' attribute due to MDM-42, which throws an NPE.\n"
                                              + "To workaround this issue, add an 'element' attribute with the proper QName for the type [%s] with the convention \"{<targetNamespace>}<rootElement>\"\n"
                                              + "e.g: if targetNamespace=\"http://validationnamespace.raml.org\" and element name is \"User\", then the following value should be used"
                                              + "element=\"{http://validationnamespace.raml.org}User\"",
                                          typeIdentifier),
                                   e.getCause());
      }
      throw e;
    }
  }
}
