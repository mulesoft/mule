/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import static org.mule.runtime.core.expression.DefaultExpressionManager.OBJECT_FOR_ENRICHMENT;
import static org.mule.runtime.core.expression.DefaultExpressionManager.removeExpressionMarker;

import org.mule.mvel2.CompileException;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ast.Function;
import org.mule.mvel2.compiler.ExpressionCompiler;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.integration.impl.CachedMapVariableResolverFactory;
import org.mule.mvel2.util.CompilerTools;
import org.mule.runtime.api.metadata.AbstractDataTypeBuilderFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.expression.InvalidExpressionException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.el.mvel.datatype.MvelDataTypeResolver;
import org.mule.runtime.core.el.mvel.datatype.MvelEnricherDataTypePropagator;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.MimeType;

/**
 * Expression language that uses MVEL (http://mvel.codehaus.org/).
 */
public class MVELExpressionLanguage implements ExpressionLanguage, Initialisable {

  protected ParserConfiguration parserConfiguration;
  protected MuleContext muleContext;
  protected MVELExpressionExecutor expressionExecutor;

  protected VariableResolverFactory staticContext;
  protected VariableResolverFactory globalContext;

  // Configuration
  protected String globalFunctionsString;
  protected String globalFunctionsFile;
  protected Map<String, Function> globalFunctions = new HashMap<>();
  protected Map<String, String> aliases = new HashMap<>();
  protected Map<String, Class<?>> imports = new HashMap<>();
  protected boolean autoResolveVariables = true;
  protected MvelDataTypeResolver dataTypeResolver = new MvelDataTypeResolver();
  protected MvelEnricherDataTypePropagator dataTypePropagator = new MvelEnricherDataTypePropagator();

  public MVELExpressionLanguage(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public void initialise() throws InitialisationException {
    parserConfiguration = createParserConfiguration(imports);
    expressionExecutor = new MVELExpressionExecutor(parserConfiguration);

    loadGlobalFunctions();
    createStaticContext();
  }

  protected void createStaticContext() {
    staticContext = new StaticVariableResolverFactory(parserConfiguration, muleContext);
    globalContext = new GlobalVariableResolverFactory(getAliases(), getGlobalFunctions(), parserConfiguration, muleContext);
  }

  protected void loadGlobalFunctions() throws InitialisationException {
    // Global functions defined in external file
    if (globalFunctionsFile != null) {
      try {
        globalFunctions.putAll(CompilerTools
            .extractAllDeclaredFunctions(new ExpressionCompiler(IOUtils.getResourceAsString(globalFunctionsFile, getClass()))
                .compile()));
      } catch (IOException e) {
        throw new InitialisationException(CoreMessages.failedToLoad(globalFunctionsFile), e, this);
      }
    }

    // Global functions defined in configuration file (take precedence over functions in file)
    globalFunctions.putAll(CompilerTools.extractAllDeclaredFunctions(new ExpressionCompiler(globalFunctionsString).compile()));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T evaluate(String expression) {
    return (T) evaluate(expression, (Map<String, Object>) null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T evaluate(String expression, Map<String, Object> vars) {
    MVELExpressionLanguageContext context = createExpressionLanguageContext();
    if (vars != null) {
      context.setNextFactory(new CachedMapVariableResolverFactory(vars, new DelegateVariableResolverFactory(staticContext,
                                                                                                            globalContext)));
    } else {
      context.setNextFactory(new DelegateVariableResolverFactory(staticContext, globalContext));
    }
    return (T) evaluateInternal(expression, context);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T evaluate(String expression, MuleEvent event) {
    return (T) evaluate(expression, event, null);
  }

  @Override
  public <T> T evaluate(String expression, MuleEvent event, Map<String, Object> vars) {
    if (event == null) {
      return evaluate(expression, vars);
    }
    MVELExpressionLanguageContext context = createExpressionLanguageContext();
    final DelegateVariableResolverFactory innerDelegate =
        new DelegateVariableResolverFactory(globalContext, createVariableVariableResolverFactory(event));
    final DelegateVariableResolverFactory delegate =
        new DelegateVariableResolverFactory(staticContext, new EventVariableResolverFactory(parserConfiguration, muleContext,
                                                                                            event, innerDelegate));
    if (vars != null) {
      context.setNextFactory(new CachedMapVariableResolverFactory(vars, delegate));
    } else {
      context.setNextFactory(delegate);
    }
    return evaluateInternal(expression, context);
  }

  @Override
  public void enrich(String expression, MuleEvent event, TypedValue typedValue) {
    evaluate(expression, event, Collections.singletonMap(OBJECT_FOR_ENRICHMENT, typedValue.getValue()));

    expression = removeExpressionMarker(expression);

    final Serializable compiledExpression = expressionExecutor.getCompiledExpression(expression);

    dataTypePropagator.propagate(typedValue, event, compiledExpression);
  }

  @Override
  public TypedValue evaluateTyped(String expression, MuleEvent event) {
    expression = removeExpressionMarker(expression);

    final Object value = evaluate(expression, event);
    final Serializable compiledExpression = expressionExecutor.getCompiledExpression(expression);
    final DataType dataType = dataTypeResolver.resolve(value, event, compiledExpression);

    return new TypedValue(value, dataType);
  }

  @SuppressWarnings("unchecked")
  protected <T> T evaluateInternal(String expression, MVELExpressionLanguageContext variableResolverFactory) {
    validate(expression);

    expression = removeExpressionMarker(expression);

    try {
      return (T) expressionExecutor.execute(expression, variableResolverFactory);
    } catch (Exception e) {
      throw new ExpressionRuntimeException(CoreMessages.expressionEvaluationFailed(expression), e);
    }
  }

  @Override
  public boolean isValid(String expression) {
    try {
      validate(expression);
      return true;
    } catch (InvalidExpressionException e) {
      return false;
    }
  }

  @Override
  public void validate(String expression) throws InvalidExpressionException {
    if (expression.startsWith(ExpressionManager.DEFAULT_EXPRESSION_PREFIX)) {
      if (!expression.endsWith(ExpressionManager.DEFAULT_EXPRESSION_POSTFIX)) {
        throw new InvalidExpressionException(expression, "Expression string is not an expression");
      }
      expression = expression.substring(2, expression.length() - 1);
    }

    try {
      expressionExecutor.validate(expression);
    } catch (CompileException e) {
      throw new InvalidExpressionException(expression, e.getMessage());
    }
  }

  protected MVELExpressionLanguageContext createExpressionLanguageContext() {
    return new MVELExpressionLanguageContext(parserConfiguration, muleContext);
  }

  public static ParserConfiguration createParserConfiguration(Map<String, Class<?>> imports) {
    ParserConfiguration ParserConfiguration = new ParserConfiguration();
    configureParserConfiguration(ParserConfiguration, imports);
    return ParserConfiguration;
  }

  protected static void configureParserConfiguration(ParserConfiguration parserConfiguration, Map<String, Class<?>> imports) {
    // defaults imports

    // JRE
    parserConfiguration.addPackageImport("java.io");
    parserConfiguration.addPackageImport("java.lang");
    parserConfiguration.addPackageImport("java.net");
    parserConfiguration.addPackageImport("java.util");

    parserConfiguration.addImport(BigDecimal.class);
    parserConfiguration.addImport(BigInteger.class);
    parserConfiguration.addImport(DataHandler.class);
    parserConfiguration.addImport(MimeType.class);
    parserConfiguration.addImport(Pattern.class);

    // Mule
    parserConfiguration.addImport(DataType.class);
    parserConfiguration.addImport(AbstractDataTypeBuilderFactory.class);

    // Global imports
    for (Entry<String, Class<?>> importEntry : imports.entrySet()) {
      parserConfiguration.addImport(importEntry.getKey(), importEntry.getValue());
    }

  }

  public void setGlobalFunctionsString(String globalFunctionsString) {
    this.globalFunctionsString = globalFunctionsString;
  }

  public void setAliases(Map<String, String> aliases) {
    this.aliases = aliases;
  }

  public void setImports(Map<String, Class<?>> imports) {
    this.imports = imports;
  }

  public void setAutoResolveVariables(boolean autoResolveVariables) {
    this.autoResolveVariables = autoResolveVariables;
  }

  public void setDataTypeResolver(MvelDataTypeResolver dataTypeResolver) {
    this.dataTypeResolver = dataTypeResolver;
  }

  public void addGlobalFunction(String name, Function function) {
    this.globalFunctions.put(name, function);
  }

  public void addImport(String name, Class<?> clazz) {
    this.imports.put(name, clazz);
  }

  public void addAlias(String name, String expression) {
    this.aliases.put(name, expression);
  }

  public void setGlobalFunctionsFile(String globalFunctionsFile) {
    this.globalFunctionsFile = globalFunctionsFile;
  }

  protected VariableResolverFactory createVariableVariableResolverFactory(MuleEvent event) {
    if (autoResolveVariables) {
      return new VariableVariableResolverFactory(parserConfiguration, muleContext, event);
    } else {
      return new NullVariableResolverFactory();
    }
  }

  public Map<String, String> getAliases() {
    return aliases;
  }

  public Map<String, Function> getGlobalFunctions() {
    return globalFunctions;
  }

  public ParserConfiguration getParserConfiguration() {
    return parserConfiguration;
  }



}
