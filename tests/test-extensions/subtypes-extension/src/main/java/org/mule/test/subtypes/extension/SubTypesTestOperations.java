/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.dsl.xml.ParameterDsl;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.test.vegan.extension.VeganCookBook;

import java.util.List;
import java.util.Map;

@MetadataScope(outputResolver = SubtypesOutputResolver.class)
public class SubTypesTestOperations {

  public ParentShape shapeRetriever(ParentShape shape) {
    return shape;
  }

  public Door doorRetriever(Door door) {
    return door;
  }

  public SubTypesMappingConnector configRetriever(@Config SubTypesMappingConnector config) {
    return config;
  }

  public SubTypesConnectorConnection connectionRetriever(@Connection SubTypesConnectorConnection connection) {
    return connection;
  }

  public List<Object> subtypedAndConcreteParameters(@Optional ParentShape baseShape, @Optional Door door,
                                                    @Optional FinalPojo finalPojo, @Optional VeganCookBook cookBook,
                                                    @Optional ParentShape squareShape, @Optional ParentShape triangle,
                                                    @Optional @ParameterDsl(allowReferences = false) NoReferencePojo noRef) {
    return asList(baseShape, door, finalPojo, cookBook, squareShape, triangle, noRef);
  }

  public NoGlobalPojo noGlobalPojo(@ParameterDsl(allowInlineDefinition = false) NoGlobalPojo noGlobalPojo) {
    return noGlobalPojo;
  }

  public Map<Door, Map<String, Door>> processDoor(Door door, @Optional Map<String, Door> doorRegistry) {
    return singletonMap(door, doorRegistry);
  }

  @MediaType(TEXT_PLAIN)
  public String pojosWithCommonNameInnerField(HasDoor hasDoor, HasShape hasShape) {
    return hasDoor.getCommonName().getHandle().concat(hasShape.getCommonName().getArea().toString());
  }

  @MediaType(TEXT_PLAIN)
  public String kill(Deadly deadly) {
    return deadly.getWeapon().kill();
  }

}
