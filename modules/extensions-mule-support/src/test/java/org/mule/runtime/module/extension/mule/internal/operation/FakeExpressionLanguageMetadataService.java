/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.operation;

import org.mule.metadata.api.TypeLoader;
import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.message.api.MuleEventMetadataTypeBuilder;
import org.mule.metadata.message.api.el.ModuleDefinition;
import org.mule.metadata.message.api.el.ModuleIdentifier;
import org.mule.metadata.message.api.el.TypeBindings;
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fake implementation which only implements a rudimentary {@link #evaluateTypeExpression(String, Collection)}.
 */
public class FakeExpressionLanguageMetadataService implements ExpressionLanguageMetadataService {

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public void getInputType(String expression, MetadataType output, MuleEventMetadataTypeBuilder builder,
                           MessageCallback callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType getOutputType(TypeBindings typeBindings, String expression, MessageCallback callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType getOutputType(TypeBindings typeBindings, String expression, String outputMimeType,
                                    MessageCallback callback) {
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
  public ModuleDefinition moduleDefinition(String nameIdentifier, Collection<ModuleDefinition> modules)
      throws ExpressionCompilationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public MetadataType evaluateTypeExpression(String typeExpression, Collection<ModuleDefinition> modules)
      throws ExpressionCompilationException {
    // mule!ExtensionPrefix::TypeAlias
    String[] muleAndRest = typeExpression.split("!");
    assert muleAndRest.length == 2;

    String mulePrefix = muleAndRest[0];
    String extensionPlusAlias = muleAndRest[1];
    assert mulePrefix.equals("mule");

    // ExtensionPrefix::TypeAlias
    String[] extensionAndAlias = extensionPlusAlias.split("::");
    assert extensionAndAlias.length == 2;

    String extensionPrefix = extensionAndAlias[0];
    String typeAlias = extensionAndAlias[1];

    ModuleDefinition module = getModuleForPrefix(modules, extensionPrefix);
    return module.declaredTypes().stream().filter(mt -> matchesAlias(mt, typeAlias)).findFirst().get();
  }

  private boolean matchesAlias(MetadataType metadataType, String typeAlias) {
    Optional<TypeAliasAnnotation> optionalAnnotation = metadataType.getAnnotation(TypeAliasAnnotation.class);
    return optionalAnnotation.isPresent() && optionalAnnotation.get().getValue().equals(typeAlias);
  }

  private ModuleDefinition getModuleForPrefix(Collection<ModuleDefinition> modules, String extensionPrefix) {
    return modules.stream().filter(moduleDefinition -> matchesModuleIdentifier(moduleDefinition.getName(), extensionPrefix))
        .findFirst().get();
  }

  private boolean matchesModuleIdentifier(ModuleIdentifier name, String extensionPrefix) {
    return extensionPrefix.equals(name.toString());
  }
}
