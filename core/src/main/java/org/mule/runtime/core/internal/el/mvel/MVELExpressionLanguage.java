/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.replace;
import static org.mule.runtime.api.el.ValidationResult.failure;
import static org.mule.runtime.api.el.ValidationResult.success;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.MEL_PREFIX;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.PREFIX_EXPR_SEPARATOR;

import org.mule.mvel2.CompileException;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ast.Function;
import org.mule.mvel2.compiler.ExpressionCompiler;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.integration.impl.CachedMapVariableResolverFactory;
import org.mule.mvel2.util.CompilerTools;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.AbstractDataTypeBuilderFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.core.internal.el.ExtendedExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.el.mvel.datatype.MvelDataTypeResolver;
import org.mule.runtime.core.internal.el.mvel.datatype.MvelEnricherDataTypePropagator;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.MimeType;
import javax.inject.Inject;

/**
 * Expression language that uses MVEL (http://mvel.codehaus.org/).
 */
public class MVELExpressionLanguage extends AbstractComponent implements ExtendedExpressionLanguageAdaptor, Initialisable {

  private static final String OBJECT_FOR_ENRICHMENT = "__object_for_enrichment";

  protected ParserConfiguration parserConfiguration;
  protected MuleContext muleContext;
  private MVELExpressionExecutor expressionExecutor;

  private VariableResolverFactory staticContext;
  private VariableResolverFactory globalContext;

  // Configuration
  private String globalFunctionsString;
  private String globalFunctionsFile;
  private Map<String, Function> globalFunctions = new HashMap<>();
  protected Map<String, String> aliases = new HashMap<>();
  protected Map<String, Class<?>> imports = new HashMap<>();
  private boolean autoResolveVariables = true;
  private MvelDataTypeResolver dataTypeResolver = new MvelDataTypeResolver();
  private MvelEnricherDataTypePropagator dataTypePropagator = new MvelEnricherDataTypePropagator();

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

  private void createStaticContext() {
    staticContext = new StaticVariableResolverFactory(parserConfiguration, muleContext);
    globalContext = new GlobalVariableResolverFactory(getAliases(), getGlobalFunctions(), parserConfiguration, muleContext);
  }

  private void loadGlobalFunctions() throws InitialisationException {
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

  public <T> T evaluateUntyped(String expression, PrivilegedEvent event, PrivilegedEvent.Builder eventBuilder,
                               ComponentLocation componentLocation,
                               Map<String, Object> vars) {
    if (event == null) {
      return evaluateUntyped(expression, vars);
    }
    MVELExpressionLanguageContext context = createExpressionLanguageContext();
    final DelegateVariableResolverFactory innerDelegate =
        new DelegateVariableResolverFactory(globalContext, createVariableVariableResolverFactory(event, eventBuilder));
    final DelegateVariableResolverFactory delegate =
        new DelegateVariableResolverFactory(staticContext, new EventVariableResolverFactory(parserConfiguration, muleContext,
                                                                                            event, eventBuilder,
                                                                                            componentLocation,
                                                                                            innerDelegate));
    if (vars != null) {
      context.setNextFactory(new CachedMapVariableResolverFactory(vars, delegate));
    } else {
      context.setNextFactory(delegate);
    }
    return evaluateInternal(expression, context);
  }

  @Override
  public void enrich(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                     ComponentLocation componentLocation,
                     Object object) {
    expression = removeExpressionMarker(expression);
    expression = createEnrichmentExpression(expression);
    evaluateUntyped(expression, (PrivilegedEvent) event, (PrivilegedEvent.Builder) eventBuilder, componentLocation,
                    singletonMap(OBJECT_FOR_ENRICHMENT, object));
  }

  @Override
  public void enrich(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                     ComponentLocation componentLocation,
                     TypedValue typedValue) {
    expression = removeExpressionMarker(expression);
    expression = createEnrichmentExpression(expression);
    evaluateUntyped(expression, (PrivilegedEvent) event, (PrivilegedEvent.Builder) eventBuilder, componentLocation,
                    singletonMap(OBJECT_FOR_ENRICHMENT, typedValue.getValue()));

    final Serializable compiledExpression = expressionExecutor.getCompiledExpression(expression);

    event = eventBuilder.build();
    dataTypePropagator.propagate(typedValue, (PrivilegedEvent) event, (PrivilegedEvent.Builder) eventBuilder, compiledExpression);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, ComponentLocation componentLocation,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    TypedValue evaluate = evaluate(expression, event, componentLocation, bindingContext);
    return MVELSplitDataIterator.createFrom(evaluate.getValue());

  }

  @Override
  public Iterator<TypedValue<?>> split(String expression, CoreEvent event, BindingContext bindingContext)
      throws ExpressionRuntimeException {
    TypedValue evaluate = evaluate(expression, event, bindingContext);
    return MVELSplitDataIterator.createFrom(evaluate.getValue());
  }


  @Override
  public void addGlobalBindings(BindingContext bindingContext) {
    // Do nothing
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, BindingContext context) {
    return evaluate(expression, event, CoreEvent.builder(event), null, context);
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event, BindingContext context)
      throws ExpressionRuntimeException {
    return evaluate(expression, expectedOutputType, event, null, context, false);
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, CoreEvent event,
                             ComponentLocation componentLocation,
                             BindingContext context, boolean failOnNull)
      throws ExpressionRuntimeException {
    return evaluate(expression, event, componentLocation, context);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, ComponentLocation componentLocation,
                             BindingContext bindingContext) {
    return evaluate(expression, event, CoreEvent.builder(event), componentLocation, bindingContext);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event, CoreEvent.Builder eventBuilder,
                             ComponentLocation componentLocation,
                             BindingContext bindingContext) {
    expression = removeExpressionMarker(expression);
    Map<String, Object> bindingMap = bindingContext.identifiers().stream().collect(toMap(id -> id,
                                                                                         id -> bindingContext.lookup(id).get()
                                                                                             .getValue()));

    final Object value = evaluateUntyped(expression, (PrivilegedEvent) event, (PrivilegedEvent.Builder) eventBuilder,
                                         componentLocation, bindingMap);
    if (value instanceof TypedValue) {
      return (TypedValue) value;
    } else {
      final Serializable compiledExpression = expressionExecutor.getCompiledExpression(expression);
      DataType dataType = event != null ? dataTypeResolver.resolve(value, (PrivilegedEvent) event, compiledExpression) : OBJECT;

      return new TypedValue(value, dataType);
    }
  }

  @Override
  public TypedValue<?> evaluateLogExpression(String expression, CoreEvent event, ComponentLocation componentLocation,
                                             BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return evaluate(expression, event, componentLocation, bindingContext);
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


  private MVELExpressionLanguageContext createExpressionLanguageContext() {
    return new MVELExpressionLanguageContext(parserConfiguration, muleContext);
  }

  public static ParserConfiguration createParserConfiguration(Map<String, Class<?>> imports) {
    ParserConfiguration ParserConfiguration = new ParserConfiguration();
    configureParserConfiguration(ParserConfiguration, imports);
    return ParserConfiguration;
  }

  private static void configureParserConfiguration(ParserConfiguration parserConfiguration, Map<String, Class<?>> imports) {
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

  protected VariableResolverFactory createVariableVariableResolverFactory(PrivilegedEvent event,
                                                                          PrivilegedEvent.Builder eventBuilder) {
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

  private static String removeExpressionMarker(String expression) {
    if (expression == null) {
      throw new IllegalArgumentException(objectIsNull("expression").getMessage());
    }
    if (expression.startsWith(DEFAULT_EXPRESSION_PREFIX)) {
      expression = expression.substring(2, expression.length() - 1);
    }
    if (expression.startsWith(MEL_PREFIX + PREFIX_EXPR_SEPARATOR)) {
      expression = expression.substring((MEL_PREFIX + PREFIX_EXPR_SEPARATOR).length());
    }
    return expression;
  }

  private String createEnrichmentExpression(String expression) {
    if (expression.contains("$")) {
      expression = replace(expression, "$", OBJECT_FOR_ENRICHMENT);
    } else {
      expression = expression + "=" + OBJECT_FOR_ENRICHMENT;
    }
    return expression;
  }
}
