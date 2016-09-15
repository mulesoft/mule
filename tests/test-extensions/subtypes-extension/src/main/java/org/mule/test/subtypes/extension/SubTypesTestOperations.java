/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;


import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.test.vegan.extension.VeganCookBook;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

public class SubTypesTestOperations {

  public ParentShape shapeRetriever(ParentShape shape) {
    return shape;
  }

  public Door doorRetriever(Door door) {
    return door;
  }

  public SubTypesMappingConnector configRetriever(@UseConfig SubTypesMappingConnector config) {
    return config;
  }

  public SubTypesConnectorConnection connectionRetriever(@Connection SubTypesConnectorConnection connection) {
    return connection;
  }

  public List<Object> subtypedAndConcreteParameters(ParentShape baseShape, Door door, FinalPojo finalPojo, VeganCookBook cookBook,
                                                    ParentShape squareShape, ParentShape triangle,
                                                    @XmlHints(allowReferences = false) NoReferencePojo noRef) {
    return asList(baseShape, door, finalPojo, cookBook, squareShape, triangle, noRef);
  }

  public NoGlobalPojo noGlobalPojo(@XmlHints(allowInlineDefinition = false) NoGlobalPojo noGlobalPojo) {
    return noGlobalPojo;
  }

  public Map<Door, Map<String, Door>> processDoor(Door door, @Optional Map<String, Door> doorRegistry) {
    return singletonMap(door, doorRegistry);
  }
}
