/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.api.loader.xml.declaration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.persistence.MetadataTypeGsonTypeAdapter;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.processor.Processor;

import java.util.Map;

/**
 * Declaration of a {@link OperationModel} inferred by the chain of {@link Processor}s within a <operation/>s body
 *
 * @since 4.0
 */
public class DeclarationOperation {

  private MetadataType output;
  private MetadataType outputAttributes;

  /**
   * Declaration Operation represents an operation of the <module/>
   *
   * @param output {@link MetadataType}'s output of an <operation/>
   * @param outputAttributes {@link MetadataType}'s output's attribute of an <operation/>
   */
  public DeclarationOperation(MetadataType output, MetadataType outputAttributes) {
    this.output = output;
    this.outputAttributes = outputAttributes;
  }

  /**
   * @return {@link MetadataType} for the <operation/>'s output.
   */
  public MetadataType getOutput() {
    return output;
  }

  /**
   * @return {@link MetadataType} for the <operation/>'s output's attributes.
   */
  public MetadataType getOutputAttributes() {
    return outputAttributes;
  }


  /**
   * Serializer of the declaration for a whole map of declarations, where each key of {@link Map#keySet()} represents an
   * <operation/> of the current <module/>
   *
   * @param operationMap map with operations of the current <module/>
   * @return a serialized JSON string
   */
  public static String toString(Map<String, DeclarationOperation> operationMap) {
    final Gson gson = new GsonBuilder().registerTypeAdapter(MetadataType.class, new MetadataTypeGsonTypeAdapter()).create();
    return gson.toJson(operationMap);
  }

  /**
   * Deserializer of the declaration for a whole map of declarations, where the result represents a map with all the
   * <operation/>s of the current <module/>
   *
   * @param json String representation of a {@link Map<String,DeclarationOperation>}
   * @return a {@link Map<String, DeclarationOperation>} where each key is an operation of the current <module/>
   */
  public static Map<String, DeclarationOperation> fromString(String json) {
    try {
      final Gson gson = new GsonBuilder().registerTypeAdapter(MetadataType.class, new MetadataTypeGsonTypeAdapter()).create();
      return gson.fromJson(json, new TypeToken<Map<String, DeclarationOperation>>() {}.getType());
    } catch (JsonParseException jpe) {
      throw new IllegalArgumentException("The declarations content does not match the expected format", jpe);
    }
  }
}
