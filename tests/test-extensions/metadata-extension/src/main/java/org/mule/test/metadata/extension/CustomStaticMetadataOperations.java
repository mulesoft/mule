/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import static java.lang.Thread.currentThread;

import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.metadata.fixed.AttributesJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.AttributesXmlType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.InputXmlType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputJsonType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputXmlType;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.test.metadata.extension.resolver.CsvInputStaticTypeResolver;
import org.mule.test.metadata.extension.resolver.JavaOutputStaticTypeResolver;
import org.mule.test.metadata.extension.resolver.JsonInputStaticTypeResolver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomStaticMetadataOperations {

  private static final ClassLoader cl = currentThread().getContextClassLoader();
  public static final String CSV_VALUE = "Name,LastName\\njuan,desimoni\\nesteban,wasinger";
  public static final String XML_VALUE = IOUtils.toString(cl.getResourceAsStream("order.xml"));
  public static final String JSON_VALUE = "{\"age\":12,\"dni\": 1478231}";
  public static final String JSON_ARRAY_VALUE = "[{\"age\":12,\"dni\": 1478231}, {\"age\":25,\"dni\": 37562148}]";

  @OutputXmlType(schema = "order.xsd", qname = "shiporder")
  public InputStream xmlOutput() {
    return cl.getResourceAsStream("order.xml");
  }

  @OutputXmlType(schema = "order.xsd", qname = "shiporder")
  public List<InputStream> xmlOutputList() {
    ArrayList xmlList = new ArrayList();
    xmlList.add(cl.getResourceAsStream("order.xml"));
    xmlList.add(cl.getResourceAsStream("order.xml"));
    return xmlList;
  }

  @OutputXmlType(schema = "order.xsd", qname = "shiporder")
  public String xmlInput(@InputXmlType(schema = "order.xsd", qname = "shiporder") InputStream xml) {
    return XML_VALUE;
  }

  @OutputJsonType(schema = "person-schema.json")
  public InputStream jsonOutput() {
    return new ByteArrayInputStream(JSON_VALUE.getBytes());
  }

  @OutputJsonType(schema = "persons-schema.json")
  public InputStream jsonOutputArray() {
    return new ByteArrayInputStream(JSON_ARRAY_VALUE.getBytes());
  }

  @OutputJsonType(schema = "person-schema.json")
  public List<String> jsonOutputList() {
    return new ArrayList<>();
  }

  @OutputJsonType(schema = "person-schema.json")
  public PagingProvider<MetadataConnection, String> jsonOutputPagingProvider() {
    return new PagingProvider<MetadataConnection, String>() {

      @Override
      public List<String> getPage(MetadataConnection connection) {
        ArrayList jsonArrayList = new ArrayList<>();
        jsonArrayList.add(JSON_VALUE);
        jsonArrayList.add(JSON_VALUE);
        return jsonArrayList;
      }

      @Override
      public java.util.Optional<Integer> getTotalResults(MetadataConnection connection) {
        return java.util.Optional.empty();
      }

      @Override
      public void close(MetadataConnection connection) {

      }
    };
  }

  @OutputJsonType(schema = "persons-schema.json")
  public List<String> jsonArrayOutputList() {
    ArrayList jsonArrayList = new ArrayList();
    jsonArrayList.add(JSON_ARRAY_VALUE);
    jsonArrayList.add(JSON_ARRAY_VALUE);
    return jsonArrayList;
  }

  @MediaType(value = "application/json")
  public String jsonInputStream(@InputJsonType(schema = "person-schema.json") InputStream json) {
    return IOUtils.toString(json);
  }

  public List<InputStream> jsonInputList(@InputJsonType(schema = "person-schema.json") List<InputStream> jsonList) {
    return jsonList;
  }

  @OutputResolver(output = CsvInputStaticTypeResolver.class)
  public List<Object> customTypeListOutput() {
    ArrayList<Object> csvList = new ArrayList();
    csvList.add(CSV_VALUE);
    csvList.add(CSV_VALUE);
    return csvList;
  }

  public int jsonInputMap(@InputJsonType(schema = "person-schema.json") Map<String, Object> json) {
    return (int) json.get("age");
  }

  @OutputJsonType(schema = "person-schema.json")
  public Result<InputStream, Banana> customTypeOutputWithStaticAttributes() {
    return Result.<InputStream, Banana>builder()
        .output(new ByteArrayInputStream(new byte[] {}))
        .build();
  }

  @OutputResolver(output = CsvInputStaticTypeResolver.class)
  public Object customTypeOutput() {
    return CSV_VALUE;
  }

  @MediaType("application/json")
  public String customTypeInput(@TypeResolver(JsonInputStaticTypeResolver.class) InputStream type) {
    return IOUtils.toString(type);
  }

  @AttributesXmlType(schema = "order.xsd", qname = "shiporder")
  public Result<Integer, InputStream> xmlAttributes() {
    return Result.<Integer, InputStream>builder().output(1).build();
  }

  @OutputResolver(output = CsvInputStaticTypeResolver.class, attributes = JsonStaticAttributesTypeResolver.class)
  public Result<Integer, InputStream> customAttributesOutput() {
    return Result.<Integer, InputStream>builder().output(1).build();
  }

  @AttributesJsonType(schema = "person-schema.json")
  public Result<Integer, InputStream> jsonAttributes() {
    return Result.<Integer, InputStream>builder().output(1).build();
  }

  @AttributesJsonType(schema = "persons-schema.json")
  public Result<Integer, InputStream> jsonArrayAttributes() {
    return Result.<Integer, InputStream>builder().output(1).build();
  }

  @AttributesJsonType(schema = "person-schema.json")
  public Result<Integer, List<InputStream>> jsonAttributesList() {
    return Result.<Integer, List<InputStream>>builder().output(1).build();
  }

  @AttributesJsonType(schema = "persons-schema.json")
  public Result<Integer, ArrayList<InputStream>> jsonArrayAttributesList() {
    return Result.<Integer, ArrayList<InputStream>>builder().output(1).build();
  }

  @AttributesJsonType(schema = "person-schema.json")
  public PagingProvider<MetadataConnection, Result<Integer, InputStream>> jsonAttributesPagingProviderWithResult() {
    return new PagingProvider<MetadataConnection, Result<Integer, InputStream>>() {

      @Override
      public List<Result<Integer, InputStream>> getPage(MetadataConnection connection) {
        return new ArrayList<>();
      }

      @Override
      public java.util.Optional<Integer> getTotalResults(MetadataConnection connection) {
        return java.util.Optional.empty();
      }

      @Override
      public void close(MetadataConnection connection) {

      }
    };
  }

  @OutputXmlType(schema = "", qname = "shiporder")
  public List<InputStream> xmlOutputListWithEmptySchema() {
    ArrayList<InputStream> xmlList = new ArrayList();
    xmlList.add(cl.getResourceAsStream("order.xml"));
    xmlList.add(cl.getResourceAsStream("order.xml"));
    return xmlList;
  }

  @OutputResolver(output = JavaOutputStaticTypeResolver.class)
  public Object customInputAndOutput(@TypeResolver(JsonInputStaticTypeResolver.class) InputStream type) {
    return null;
  }
}
