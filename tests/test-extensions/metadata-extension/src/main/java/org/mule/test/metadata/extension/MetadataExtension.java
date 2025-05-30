/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.test.metadata.extension.model.animals.Animal;
import org.mule.test.metadata.extension.model.animals.Bear;
import org.mule.test.metadata.extension.model.attribute.AbstractOutputAttributes;
import org.mule.test.metadata.extension.model.attribute.AnimalsOutputAttributes;
import org.mule.test.metadata.extension.model.attribute.ShapeOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Circle;
import org.mule.test.metadata.extension.model.shapes.Rectangle;
import org.mule.test.metadata.extension.model.shapes.Shape;
import org.mule.test.metadata.extension.model.shapes.Square;
import org.mule.test.metadata.extension.query.NativeQueryOutputResolver;
import org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils;

@Extension(name = "Metadata")
@JavaVersionSupport({JAVA_21, JAVA_17})
@Operations({MetadataOperations.class, MetadataFailureOperations.class,
    MetadataInheritedOperationResolversOperations.class, CustomStaticMetadataOperations.class})
@ConnectionProviders(MetadataConnectionProvider.class)
@Sources({MetadataSource.class, MetadataSourceWithMultilevel.class, MetadataSourceWithPartialMultilevel.class,
    CustomStaticMetadataSource.class, CustomXmlStaticMetadataSource.class,
    MetadataSourceWithCallbackParameters.class, MetadataSourceWithPartialMultiLevelShowInDsl.class})
@SubTypeMapping(baseType = Animal.class, subTypes = Bear.class)
@SubTypeMapping(baseType = Shape.class, subTypes = {Circle.class, Rectangle.class})
@SubTypeMapping(baseType = AbstractOutputAttributes.class,
    subTypes = {AnimalsOutputAttributes.class, ShapeOutputAttributes.class})
@SubTypeMapping(baseType = Rectangle.class, subTypes = {Square.class})
@Xml(namespace = "http://www.mulesoft.org/schema/mule/metadata", prefix = "metadata")
@Export(classes = {TestMetadataResolverUtils.class, NativeQueryOutputResolver.class})
public class MetadataExtension {

  @Parameter
  @Optional(defaultValue = "noExpression")
  private String data;
  @Parameter
  @Optional(defaultValue = "#['defaultString']")
  private String dataWithDefault;

  public String getData() {
    return data;
  }

  public String getDataWithDefault() {
    return dataWithDefault;
  }
}
