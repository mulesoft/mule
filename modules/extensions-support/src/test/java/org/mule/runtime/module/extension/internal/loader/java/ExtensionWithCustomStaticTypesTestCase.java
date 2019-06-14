/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mule.metadata.api.model.MetadataFormat.CSV;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.api.model.MetadataFormat.JSON;
import static org.mule.metadata.api.model.MetadataFormat.XML;
import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;

import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.OutputModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.test.metadata.extension.MetadataExtension;

import org.junit.Test;

public class ExtensionWithCustomStaticTypesTestCase extends AbstractMuleTestCase {

  private static final String PERSON_TYPE_ID = "http://example.com/example.json";

  private ExtensionModel extension = loadExtension(MetadataExtension.class);

  @Test
  public void withInputXmlStaticType() throws Exception {
    OperationModel o = getOperation("xmlInput");
    ParameterModel param = o.getAllParameterModels().get(0);
    assertXmlOrder(param);
  }

  @Test
  public void withOutputXmlStaticType() throws Exception {
    OperationModel o = getOperation("xmlOutput");
    assertXmlOrder(o.getOutput());
  }

  @Test
  public void withOutputAttributesXmlStaticType() throws Exception {
    OperationModel o = getOperation("xmlAttributes");
    assertXmlOrder(o.getOutputAttributes());
  }

  @Test
  public void withInputJsonType() throws Exception {
    OperationModel o = getOperation("jsonInputStream");
    ParameterModel param = o.getAllParameterModels().get(0);
    assertJsonPerson(param);
  }

  @Test
  public void withOutputJsonType() throws Exception {
    OperationModel o = getOperation("jsonOutput");
    assertJsonPerson(o.getOutput());
  }

  @Test
  public void withArrayOutputJsonType() throws Exception {
    OperationModel o = getOperation("jsonOutputArray");

    MetadataType type = o.getOutput().getType();
    assertThat(type, is(instanceOf(ArrayType.class)));

    ArrayType arrayType = (ArrayType) type;
    assertThat(getTypeId(type).get(), equalTo(PERSON_TYPE_ID));

    assertJsonPerson(arrayType.getType());
  }

  @Test
  public void withOutputAttributesJsonType() throws Exception {
    OperationModel o = getOperation("jsonAttributes");
    assertJsonPerson(o.getOutputAttributes());
  }

  @Test
  public void customTypeOutput() throws Exception {
    OperationModel o = getOperation("customTypeOutput");
    OutputModel output = o.getOutput();
    MetadataType type = output.getType();
    assertThat(output.hasDynamicType(), is(false));
    assertThat(type.getMetadataFormat(), is(CSV));
    assertThat(type.toString(), is("csv-object"));
  }

  @Test
  public void customTypeInput() throws Exception {
    OperationModel o = getOperation("customTypeInput");
    ParameterModel param = o.getAllParameterModels().get(0);
    assertCustomJsonType(param);
  }

  @Test
  public void customTypeAttributes() throws Exception {
    OperationModel o = getOperation("customAttributesOutput");
    assertCustomJsonType(o.getOutputAttributes());
  }

  @Test
  public void customTypeInputAndOutput() throws Exception {
    OperationModel o = getOperation("customInputAndOutput");
    assertCustomJsonType(o.getAllParameterModels().get(0));
    assertCustomJavaType(o.getOutput());
  }

  @Test
  public void customTypeOutputWithStaticAttributes() throws Exception {
    OperationModel o = getOperation("customTypeOutputWithStaticAttributes");
    assertJsonPerson(o.getOutput());
    assertThat(getTypeId(o.getOutputAttributes().getType()).get(), is(Banana.class.getName()));
  }

  @Test
  public void sourceXmlOutput() {
    SourceModel s = getSource("xml-static-metadata");
    assertXmlOrder(s.getOutput());
    assertXmlOrder(s.getOutputAttributes());
  }

  @Test
  public void sourceCustomOutput() {
    SourceModel s = getSource("custom-static-metadata");
    assertCustomJavaType(s.getOutput());
  }

  @Test
  public void sourceOnErrorCustomType() {
    SourceModel s = getSource("custom-static-metadata");
    assertJsonPerson(s.getErrorCallback().get().getAllParameterModels().get(0));
  }

  @Test
  public void sourceOnSuccessCustomType() {
    SourceModel s = getSource("custom-static-metadata");
    assertXmlOrder(s.getSuccessCallback().get().getAllParameterModels().get(0));
  }

  private SourceModel getSource(String name) {
    return extension.getSourceModel(name).orElseThrow(() -> new RuntimeException("Source Not found"));
  }

  private void assertCustomJavaType(Typed t) {
    assertThat(t.hasDynamicType(), is(false));
    assertThat(t.getType().getMetadataFormat(), is(JAVA));
    assertThat(t.getType().getAnnotation(TypeIdAnnotation.class).get().getValue(), is("custom-java"));
    assertThat(t.getType(), is(instanceOf(ObjectType.class)));
  }

  private void assertXmlOrder(Typed typed) {
    MetadataType type = typed.getType();
    assertThat(typed.hasDynamicType(), is(false));
    assertThat(type.getMetadataFormat(), is(XML));
    assertThat(type.toString(), is("#root:shiporder"));
  }

  private void assertJsonPerson(Typed typed) {
    assertJsonPerson(typed.getType());
    assertThat(typed.hasDynamicType(), is(false));
  }

  private void assertJsonPerson(MetadataType type) {
    assertThat(type.getMetadataFormat(), is(JSON));
    assertThat(type, instanceOf(ObjectType.class));
    assertThat(getTypeId(type).get(), equalTo(PERSON_TYPE_ID));
    assertThat(((ObjectType) type).getFields(), hasSize(3));
  }

  private OperationModel getOperation(String ope) {
    return extension.getOperationModel(ope).orElseThrow(() -> new RuntimeException(ope + " not found"));
  }

  private void assertCustomJsonType(Typed typed) {
    MetadataType type = typed.getType();
    assertThat(typed.hasDynamicType(), is(false));
    assertThat(type.getMetadataFormat(), is(JSON));
    assertThat(type.toString(), is("json-object"));
  }
}
