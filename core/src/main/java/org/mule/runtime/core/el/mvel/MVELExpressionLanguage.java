/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.replace;
import static org.mule.runtime.api.el.ValidationResult.failure;
import static org.mule.runtime.api.el.ValidationResult.success;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.el.DefaultExpressionManager.MEL_PREFIX;
import static org.mule.runtime.core.el.DefaultExpressionManager.PREFIX_EXPR_SEPARATOR;

import org.mule.mvel2.CompileException;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ast.Function;
import org.mule.mvel2.compiler.ExpressionCompiler;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.integration.impl.CachedMapVariableResolverFactory;
import org.mule.mvel2.util.CompilerTools;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.AbstractDataTypeBuilderFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExtendedExpressionLanguage;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.el.mvel.datatype.MvelDataTypeResolver;
import org.mule.runtime.core.el.mvel.datatype.MvelEnricherDataTypePropagator;
import org.mule.runtime.core.util.IOUtils;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.MimeType;
import javax.inject.Inject;

/**
 * Expression language that uses MVEL (http://mvel.codehaus.org/).
 */
public class MVELExpressionLanguage implements ExtendedExpressionLanguage, Initialisable {

  public static final String OBJECT_FOR_ENRICHMENT = "__object_for_enrichment";

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

  @Inject
  public MVELExpressionLanguage(MuleContext muleContext) {
    this.muleContext = muleContext;
    parserConfiguration = createParserConfiguration(imports);
    expressionExecutor = new MVELExpressionExecutor(parserConfiguration);
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

  @SuppressWarnings("unchecked")
  public <T> T evaluateUntyped(String expression, Map<String, Object> vars) {
    MVELExpressionLanguageContext context = createExpressionLanguageContext();
    if (vars != null) {
      context.setNextFactory(new CachedMapVariableResolverFactory(vars, new DelegateVariableResolverFactory(staticContext,
                                                                                                            globalContext)));
    } else {
      context.setNextFactory(new DelegateVariableResolverFactory(staticContext, globalContext));
    }
    return (T) evaluateInternal(expression, context);
  }

  public <T> T evaluateUntyped(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                               Map<String, Object> vars) {
    if (event == null) {
      return evaluateUntyped(expression, vars);
    }
    MVELExpressionLanguageContext context = createExpressionLanguageContext();
    final DelegateVariableResolverFactory innerDelegate =
        new DelegateVariableResolverFactory(globalContext, createVariableVariableResolverFactory(event, eventBuilder));
    final DelegateVariableResolverFactory delegate =
        new DelegateVariableResolverFactory(staticContext, new EventVariableResolverFactory(parserConfiguration, muleContext,
                                                                                            event, eventBuilder, flowConstruct,
                                                                                            innerDelegate));
    if (vars != null) {
      context.setNextFactory(new CachedMapVariableResolverFactory(vars, delegate));
    } else {
      context.setNextFactory(delegate);
    }
    return evaluateInternal(expression, context);
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                     Object object) {
    expression = removeExpressionMarker(expression);
    expression = createEnrichmentExpression(expression);
    evaluateUntyped(expression, event, eventBuilder, flowConstruct, singletonMap(OBJECT_FOR_ENRICHMENT, object));
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                     TypedValue typedValue) {
    expression = removeExpressionMarker(expression);
    expression = createEnrichmentExpression(expression);
    evaluateUntyped(expression, event, eventBuilder, flowConstruct, singletonMap(OBJECT_FOR_ENRICHMENT, typedValue.getValue()));

    final Serializable compiledExpression = expressionExecutor.getCompiledExpression(expression);

    event = eventBuilder.build();
    dataTypePropagator.propagate(typedValue, event, eventBuilder, compiledExpression);
  }

  @Override
  public void registerGlobalContext(BindingContext bindingContext) {
    // Do nothing
  }

  @Override
  public TypedValue evaluate(String expression, Event event, BindingContext context) {
    return evaluate(expression, event, Event.builder(event), null, context);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, FlowConstruct flowConstruct, BindingContext bindingContext) {
    return evaluate(expression, event, Event.builder(event), flowConstruct, bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                             BindingContext bindingContext) {
    expression = removeExpressionMarker(expression);
    Map<String, Object> bindingMap = bindingContext.identifiers().stream().collect(toMap(id -> id,
                                                                                         id -> bindingContext.lookup(id).get()
                                                                                             .getValue()));

    final Object value = evaluateUntyped(expression, event, eventBuilder, flowConstruct, bindingMap);
    if (value instanceof TypedValue) {
      return (TypedValue) value;
    } else {
      final Serializable compiledExpression = expressionExecutor.getCompiledExpression(expression);
      DataType dataType = event != null ? dataTypeResolver.resolve(value, event, compiledExpression) : OBJECT;

      return new TypedValue(value, dataType);
    }
  }

  @SuppressWarnings("unchecked")
  protected <T> T evaluateInternal(String expression, MVELExpressionLanguageContext variableResolverFactory) {
    validate(expression);

    expression = removeExpressionMarker(expression);

    try {
      return (T) expressionExecutor.execute(expression, variableResolverFactory);
    } catch (Exception e) {
      throw new ExpressionRuntimeException(CoreMessages.expressionEvaluationFailed(e.getMessage(), expression), e);
    }
  }

  @Override
  public ValidationResult validate(String expression) {
    if (expression.startsWith(DEFAULT_EXPRESSION_PREFIX)) {
      if (!expression.endsWith(DEFAULT_EXPRESSION_POSTFIX)) {
        return failure("Expression string is not an expression", expression);
      }
      expression = expression.substring(2, expression.length() - 1);
    }

    try {
      expressionExecutor.validate(expression);
    } catch (CompileException e) {
      return failure(e.getMessage(), expression);
    }
    return success();
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

  protected VariableResolverFactory createVariableVariableResolverFactory(Event event, Event.Builder eventBuilder) {
    if (autoResolveVariables) {
      return new VariableVariableResolverFactory(parserConfiguration, muleContext, event, eventBuilder);
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

  public static String removeExpressionMarker(String expression) {
    if (expression == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
    }
    if (expression.startsWith(DEFAULT_EXPRESSION_PREFIX)) {
      expression = expression.substring(2, expression.length() - 1);
    }
    if (expression.startsWith(MEL_PREFIX + PREFIX_EXPR_SEPARATOR)) {
      expression = expression.substring((MEL_PREFIX + PREFIX_EXPR_SEPARATOR).length());
    }
    return expression;
  }

  protected String createEnrichmentExpression(String expression) {
    if (expression.contains("$")) {
      expression = replace(expression, "$", OBJECT_FOR_ENRICHMENT);
    } else {
      expression = expression + "=" + OBJECT_FOR_ENRICHMENT;
    }
    return expression;
  }
}
