/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import static java.util.Collections.singletonMap;
import static org.apache.commons.lang.StringUtils.replace;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ast.Function;
import org.mule.mvel2.compiler.ExpressionCompiler;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.integration.impl.CachedMapVariableResolverFactory;
import org.mule.mvel2.util.CompilerTools;
import org.mule.runtime.api.metadata.AbstractDataTypeBuilderFactory;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.el.ExpressionLanguage;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.expression.InvalidExpressionException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.el.mvel.datatype.MvelDataTypeResolver;
import org.mule.runtime.core.el.mvel.datatype.MvelEnricherDataTypePropagator;
import org.mule.runtime.core.metadata.DefaultTypedValue;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.TemplateParser;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.MimeType;
import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Expression language that uses MVEL (http://mvel.codehaus.org/).
 */
public class MVELExpressionLanguage implements ExpressionLanguage, Initialisable {

  public static final String OBJECT_FOR_ENRICHMENT = "__object_for_enrichment";
  private static final Logger logger = getLogger(MVELExpressionLanguage.class);

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

  // default style parser
  private TemplateParser parser = TemplateParser.createMuleStyleParser();

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
  public <T> T evaluate(String expression, Event event, FlowConstruct flowConstruct) {
    return (T) evaluate(expression, event, Event.builder(event), flowConstruct);
  }

  @Override
  public <T> T evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct) {
    return (T) evaluate(expression, event, eventBuilder, flowConstruct, null);
  }

  @Override
  public <T> T evaluate(String expression, Event event, FlowConstruct flowConstruct, Map<String, Object> vars) {
    return evaluate(expression, event, Event.builder(event), flowConstruct, vars);
  }

  @Override
  public <T> T evaluate(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                        Map<String, Object> vars) {
    if (event == null) {
      return evaluate(expression, vars);
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
    evaluate(expression, event, eventBuilder, flowConstruct, singletonMap(OBJECT_FOR_ENRICHMENT, object));
  }

  @Override
  public void enrich(String expression, Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct,
                     DefaultTypedValue typedValue) {
    expression = removeExpressionMarker(expression);
    expression = createEnrichmentExpression(expression);
    evaluate(expression, event, eventBuilder, flowConstruct, singletonMap(OBJECT_FOR_ENRICHMENT, typedValue.getValue()));

    final Serializable compiledExpression = expressionExecutor.getCompiledExpression(expression);

    event = eventBuilder.build();
    dataTypePropagator.propagate(typedValue, event, eventBuilder, compiledExpression);
  }

  @Override
  public DefaultTypedValue evaluateTyped(String expression, Event event, FlowConstruct flowConstruct) {
    return evaluateTyped(expression, event, Event.builder(event), flowConstruct);
  }

  @Override
  public DefaultTypedValue evaluateTyped(String expression, Event event, Event.Builder eventBuilder,
                                         FlowConstruct flowConstruct) {
    expression = removeExpressionMarker(expression);

    final Object value = evaluate(expression, event, eventBuilder, flowConstruct);
    final Serializable compiledExpression = expressionExecutor.getCompiledExpression(expression);
    final DataType dataType = dataTypeResolver.resolve(value, event, compiledExpression);

    return new DefaultTypedValue(value, dataType);
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
    if (!muleContext.getConfiguration().isValidateExpressions()) {
      if (logger.isDebugEnabled()) {
        logger.debug("Validate expressions is turned off, no checking done for: " + expression);
      }
      return;
    }

    final StringBuilder message = new StringBuilder();
    try {
      parser.validate(expression);
      final AtomicBoolean valid = new AtomicBoolean(true);

      if (expression.contains(DEFAULT_EXPRESSION_PREFIX)) {
        parser.parse(token -> {
          if (valid.get()) {
            try {
              expressionExecutor.validate(token);
            } catch (InvalidExpressionException e) {
              valid.compareAndSet(true, false);
              message.append(token).append(" is invalid\n");
              message.append(e.getMessage());
            }
          }
          return null;
        }, expression);
      } else {
        expressionExecutor.validate(expression);
      }
    } catch (Exception e) {
      throw new InvalidExpressionException(expression, e.getMessage());
    }

    if (message.length() > 0) {
      throw new InvalidExpressionException(expression, message.toString());
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

  @Override
  public String parse(String expression, final Event event, FlowConstruct flowConstruct) throws ExpressionRuntimeException {
    return parse(expression, event, Event.builder(event), flowConstruct);
  }

  @Override
  public String parse(String expression, final Event event, Event.Builder eventBuilder, FlowConstruct flowConstruct)
      throws ExpressionRuntimeException {
    return parser.parse((TemplateParser.TemplateCallback) token -> {
      Object result = evaluate(token, event, eventBuilder, flowConstruct);
      if (result instanceof InternalMessage) {
        return ((InternalMessage) result).getPayload();
      } else {
        return result;
      }
    }, expression);
  }

  public static String removeExpressionMarker(String expression) {
    if (expression == null) {
      throw new IllegalArgumentException(CoreMessages.objectIsNull("expression").getMessage());
    }
    if (expression.startsWith(DEFAULT_EXPRESSION_PREFIX)) {
      expression = expression.substring(2, expression.length() - 1);
    }
    return expression;
  }

  @Override
  public boolean evaluateBoolean(String expression, Event event, FlowConstruct flowConstruct)
      throws ExpressionRuntimeException {
    return evaluateBoolean(expression, event, flowConstruct, false, false);
  }

  @Override
  public boolean evaluateBoolean(String expression, Event event, FlowConstruct flowConstruct, boolean nullReturnsTrue,
                                 boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException {
    return resolveBoolean(evaluate(expression, event, flowConstruct), nullReturnsTrue, nonBooleanReturnsTrue, expression);
  }

  protected boolean resolveBoolean(Object result, boolean nullReturnsTrue, boolean nonBooleanReturnsTrue, String expression) {
    if (result == null) {
      return nullReturnsTrue;
    } else if (result instanceof Boolean) {
      return (Boolean) result;
    } else if (result instanceof String) {
      if (result.toString().toLowerCase().equalsIgnoreCase("false")) {
        return false;
      } else if (result.toString().toLowerCase().equalsIgnoreCase("true")) {
        return true;
      } else {
        return nonBooleanReturnsTrue;
      }
    } else {
      logger.warn("Expression: " + expression + ", returned an non-boolean result. Returning: " + nonBooleanReturnsTrue);
      return nonBooleanReturnsTrue;
    }
  }

  @Override
  public boolean isExpression(String expression) {
    return (expression.contains(DEFAULT_EXPRESSION_PREFIX));
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
