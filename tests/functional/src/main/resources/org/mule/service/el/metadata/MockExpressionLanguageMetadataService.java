/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.el.metadata;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MuleEventMetadataTypeBuilder;
import org.mule.metadata.message.api.el.ModuleDefinition;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MockExpressionLanguageMetadataService implements ExpressionLanguageMetadataService {

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }


  @Override
  public void getInputType(String expression, MetadataType output, MuleEventMetadataTypeBuilder builder, MessageCallback callback) {
  }

  @Override
  public MetadataType getOutputType(TypeBindings typeBindings, String expression, MessageCallback callback) {
    return null;
  }

  @Override
  public MetadataType getOutputType(TypeBindings typeBindings, String expression, String outputMimeType, MessageCallback callback) {
    return null;
  }

  @Override
  public MetadataType getMetadataFromSample(InputStream sample, Map<String, Object> readerProperties, String mimeType) {
    return null;
  }

  @Override
  public boolean isAssignable(MetadataType assignment, MetadataType expected, MessageCallback callback) {
    return false;
  }

  @Override
  public Map<String, MetadataType> resolveAssignment(MetadataType assignment, MetadataType expected, MessageCallback callback) {
    return null;
  }

  @Override
  public MetadataType substitute(MetadataType assignment, Map<String, MetadataType> substitution) {
    return null;
  }

  @Override
  public MetadataType unify(List<MetadataType> metadataTypes) {
    return null;
  }

  @Override
  public MetadataType intersect(List<MetadataType> metadataTypes) {
    return null;
  }

  @Override
  public MetadataTypeSerializer getTypeSerializer() {
    return null;
  }

  @Override
  public TypeLoader createTypeLoader(String content, MetadataFormat metadataFormat) {
    return null;
  }

  @Override
  public TypeLoader createTypeLoader(String content, MetadataFormat metadataFormat, Collection<ModuleDefinition> modules) {
    return null;
  }

  @Override
  public ModuleDefinition moduleDefinition(String nameIdentifier, Collection<ModuleDefinition> modules) throws ExpressionCompilationException {
    return null;
  }

  @Override
  public MetadataType evaluateTypeExpression(String typeExpression, Collection<ModuleDefinition> modules) throws ExpressionCompilationException {
    return null;
  }
}
