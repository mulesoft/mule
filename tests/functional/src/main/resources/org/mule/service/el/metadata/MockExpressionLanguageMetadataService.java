/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType getOutputType(TypeBindings typeBindings, String expression, MessageCallback callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType getOutputType(TypeBindings typeBindings, String expression, String outputMimeType, MessageCallback callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType getMetadataFromSample(InputStream sample, Map<String, Object> readerProperties, String mimeType) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isAssignable(MetadataType assignment, MetadataType expected, MessageCallback callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Map<String, MetadataType> resolveAssignment(MetadataType assignment, MetadataType expected, MessageCallback callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType substitute(MetadataType assignment, Map<String, MetadataType> substitution) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType unify(List<MetadataType> metadataTypes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType intersect(List<MetadataType> metadataTypes) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataTypeSerializer getTypeSerializer() {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeLoader createTypeLoader(String content, MetadataFormat metadataFormat) {
    throw new UnsupportedOperationException();
  }

  @Override
  public TypeLoader createTypeLoader(String content, MetadataFormat metadataFormat, Collection<ModuleDefinition> modules) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ModuleDefinition moduleDefinition(String nameIdentifier, Collection<ModuleDefinition> modules) throws ExpressionCompilationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType evaluateTypeExpression(String typeExpression, Collection<ModuleDefinition> modules) throws ExpressionCompilationException {
    throw new UnsupportedOperationException();
  }
}
