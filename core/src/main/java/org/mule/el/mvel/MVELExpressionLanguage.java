/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.el.ExpressionLanguage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.expression.InvalidExpressionException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.config.i18n.CoreMessages;
import org.mule.expression.DefaultExpressionManager;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.MimeType;

import org.mvel2.CompileException;
import org.mvel2.ParserContext;
import org.mvel2.ast.Function;
import org.mvel2.compiler.ExpressionCompiler;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.CachedMapVariableResolverFactory;
import org.mvel2.util.CompilerTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expression language that uses MVEL (http://mvel.codehaus.org/).
 */
public class MVELExpressionLanguage implements ExpressionLanguage, Initialisable
{
    private static Logger log = LoggerFactory.getLogger(MVELExpressionLanguage.class);

    protected ParserContext parserContext;
    protected MuleContext muleContext;
    protected MVELExpressionExecutor expressionExecutor;

    protected MVELExpressionLanguageContext staticContext;

    // Configuration
    protected String globalFunctionsString;
    protected String globalFunctionsFile;
    protected Map<String, Function> globalFunctions = new HashMap<String, Function>();
    protected Map<String, String> aliases = new HashMap<String, String>();
    protected Map<String, Class<?>> imports = new HashMap<String, Class<?>>();
    protected boolean autoResolveVariables = true;

    public MVELExpressionLanguage(MuleContext muleContext)
    {
        this.muleContext = muleContext;
        System.setProperty("mvel2.compiler.allow_override_all_prophandling", "true");
        if (muleContext.getExpressionManager() instanceof DefaultExpressionManager)
        {
            ((DefaultExpressionManager) muleContext.getExpressionManager()).setExpressionLanguage(this);
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        parserContext = createParserContext();
        expressionExecutor = new MVELExpressionExecutor(parserContext);

        loadGlobalFunctions();
        createStaticContext();
    }

    protected void createStaticContext()
    {
        staticContext = new StaticVariableResolverFactory(parserContext, muleContext);
    }

    protected void loadGlobalFunctions() throws InitialisationException
    {
        // Global functions defined in external file
        if (globalFunctionsFile != null)
        {
            try
            {
                globalFunctions.putAll(CompilerTools.extractAllDeclaredFunctions(new ExpressionCompiler(
                    IOUtils.getResourceAsString(globalFunctionsFile, getClass())).compile()));
            }
            catch (IOException e)
            {
                throw new InitialisationException(CoreMessages.failedToLoad(globalFunctionsFile), e, this);
            }
        }

        // Global functions defined in configuration file (take precedence over functions in file)
        globalFunctions.putAll(CompilerTools.extractAllDeclaredFunctions(new ExpressionCompiler(
            globalFunctionsString).compile()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression)
    {
        MVELExpressionLanguageContext factory = createExpressionLanguageContext();
        factory.appendFactory(createGlobalVariableResolverFactory(factory));
        return (T) evaluateInternal(expression, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression, Map<String, Object> vars)
    {
        MVELExpressionLanguageContext factory = createExpressionLanguageContext();
        if (vars != null)
        {
            factory.appendFactory(new CachedMapVariableResolverFactory(vars));
        }
        factory.appendFactory(createGlobalVariableResolverFactory(factory));
        return (T) evaluateInternal(expression, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression, MuleEvent event)
    {
        return (T) evaluate(expression, event, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T evaluate(String expression, MuleEvent event, Map<String, Object> vars)
    {
        MVELExpressionLanguageContext factory = createExpressionLanguageContext();
        if (vars != null)
        {
            factory.appendFactory(new CachedMapVariableResolverFactory(vars));
        }
        factory.appendFactory(createEventVariableResolverFactory(event));

        factory.appendFactory(createGlobalVariableResolverFactory(factory));
        if (autoResolveVariables)
        {
            factory.localFactory.appendFactory(createVariableVariableResolverFactory(event));
        }
        factory.addPrivateVariable(MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE,
            event.getMessage());
        return (T) evaluateInternal(expression, factory);
    }

    @Override
    @Deprecated
    public <T> T evaluate(String expression, MuleMessage message)
    {
        MVELExpressionLanguageContext factory = createExpressionLanguageContext();
        factory.appendFactory(createMessageVariableResolverFactory(message));

        factory.appendFactory(createGlobalVariableResolverFactory(factory));
        if (autoResolveVariables)
        {
            factory.localFactory.appendFactory(createVariableVariableResolverFactory(message));
        }
        factory.addPrivateVariable(MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE, message);
        return (T) evaluateInternal(expression, factory);
    }

    @Override
    @Deprecated
    public <T> T evaluate(String expression, MuleMessage message, Map<String, Object> vars)
    {
        MVELExpressionLanguageContext factory = createExpressionLanguageContext();
        if (vars != null)
        {
            factory.appendFactory(new CachedMapVariableResolverFactory(vars));
        }
        factory.appendFactory(createMessageVariableResolverFactory(message));

        factory.appendFactory(createGlobalVariableResolverFactory(factory));
        if (autoResolveVariables)
        {
            factory.localFactory.appendFactory(createVariableVariableResolverFactory(message));
        }
        factory.addPrivateVariable(MVELExpressionLanguageContext.MULE_MESSAGE_INTERNAL_VARIABLE, message);
        return (T) evaluateInternal(expression, factory);
    }

    @SuppressWarnings("unchecked")
    protected <T> T evaluateInternal(String expression, MVELExpressionLanguageContext variableResolverFactory)
    {
        validate(expression);

        if (expression.startsWith(ExpressionManager.DEFAULT_EXPRESSION_PREFIX))
        {
            expression = expression.substring(2, expression.length() - 1);
        }

        try
        {
            return (T) expressionExecutor.execute(expression, variableResolverFactory);
        }
        catch (Exception e)
        {
            throw new ExpressionRuntimeException(CoreMessages.expressionEvaluationFailed(expression), e);
        }
    }

    @Override
    public boolean isValid(String expression)
    {
        try
        {
            validate(expression);
            return true;
        }
        catch (InvalidExpressionException e)
        {
            return false;
        }
    }

    @Override
    public void validate(String expression) throws InvalidExpressionException
    {
        if (expression.startsWith(ExpressionManager.DEFAULT_EXPRESSION_PREFIX))
        {
            if (!expression.endsWith(ExpressionManager.DEFAULT_EXPRESSION_POSTFIX))
            {
                throw new InvalidExpressionException(expression, "Expression string is not an expression");
            }
            expression = expression.substring(2, expression.length() - 1);
        }

        try
        {
            expressionExecutor.validate(expression);
        }
        catch (CompileException e)
        {
            throw new InvalidExpressionException(expression, e.getMessage());
        }
    }

    protected MVELExpressionLanguageContext createExpressionLanguageContext()
    {
        MVELExpressionLanguageContext factory = new MVELExpressionLanguageContext(
            parserContext.createSubcontext(), muleContext);
        factory.appendFactory(new MVELExpressionLanguageContext(staticContext));
        factory.addPrivateVariable(MVELExpressionLanguageContext.MULE_CONTEXT_INTERNAL_VARIABLE, muleContext);
        return factory;
    }

    protected ParserContext createParserContext()
    {
        ParserContext parserContext = new ParserContext();
        configureParserContext(parserContext);
        return parserContext;
    }

    protected void configureParserContext(ParserContext parserContext)
    {
        // Nullify ParserConfiguration classloader to ensure context classloader is used instead
        parserContext.getParserConfiguration().setClassLoader(null);

        // defaults imports

        // JRE
        parserContext.addPackageImport("java.io");
        parserContext.addPackageImport("java.lang");
        parserContext.addPackageImport("java.net");
        parserContext.addPackageImport("java.util");

        parserContext.addImport(BigDecimal.class);
        parserContext.addImport(BigInteger.class);
        parserContext.addImport(DataHandler.class);
        parserContext.addImport(MimeType.class);
        parserContext.addImport(Pattern.class);

        // Mule
        parserContext.addImport(DataType.class);
        parserContext.addImport(DataTypeFactory.class);
        parserContext.addImport(NullPayload.class);
    }

    public void setGlobalFunctionsString(String globalFunctionsString)
    {
        this.globalFunctionsString = globalFunctionsString;
    }

    public void setAliases(Map<String, String> aliases)
    {
        this.aliases = aliases;
    }

    public void setImports(Map<String, Class<?>> imports)
    {
        this.imports = imports;
    }

    public void setAutoResolveVariables(boolean autoResolveVariables)
    {
        this.autoResolveVariables = autoResolveVariables;
    }

    public void addGlobalFunction(String name, Function function)
    {
        this.globalFunctions.put(name, function);
    }

    public void addImport(String name, Class<?> clazz)
    {
        this.imports.put(name, clazz);
    }

    public void addAlias(String name, String expression)
    {
        this.aliases.put(name, expression);
    }

    public void setGlobalFunctionsFile(String globalFunctionsFile)
    {
        this.globalFunctionsFile = globalFunctionsFile;
    }

    protected VariableResolverFactory createGlobalVariableResolverFactory(MVELExpressionLanguageContext context)
    {
        return new GlobalVariableResolverFactory(this, context, parserContext, muleContext);
    }

    protected VariableResolverFactory createEventVariableResolverFactory(MuleEvent event)
    {
        return new EventVariableResolverFactory(parserContext, muleContext, event);
    }

    @Deprecated
    protected VariableResolverFactory createMessageVariableResolverFactory(MuleMessage message)
    {
        return new MessageVariableResolverFactory(parserContext, muleContext, message);
    }

    protected VariableResolverFactory createVariableVariableResolverFactory(MuleEvent event)
    {
        return new VariableVariableResolverFactory(parserContext, muleContext, event);
    }

    @Deprecated
    protected VariableResolverFactory createVariableVariableResolverFactory(MuleMessage message)
    {
        return new VariableVariableResolverFactory(parserContext, muleContext, message);
    }

}
