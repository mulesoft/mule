/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.el;

import static org.mule.util.ClassUtils.isConsumable;
import static org.mule.util.Preconditions.checkArgument;
import static org.mule.util.Preconditions.checkState;

import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.OutputHandler;
import org.mule.el.context.MessageContext;
import org.mule.el.mvel.MVELExpressionLanguageContext;
import org.mule.module.xml.transformer.DelayedResult;
import org.mule.module.xml.util.NamespaceManager;
import org.mule.module.xml.util.XMLUtils;
import org.mule.module.xml.xpath.SaxonXpathEvaluator;
import org.mule.module.xml.xpath.XPathEvaluator;
import org.mule.module.xml.xpath.XPathReturnType;
import org.mule.transformer.TransformerUtils;
import org.mule.transformer.types.DataTypeFactory;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;

import java.io.InputStream;

import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * A MEL function capable of evaluation XPath expressions by delegating into an instance
 * of {@link XPathEvaluator}
 * <p/>
 * The function will now accept the following arguments (in order):
 * <p/>
 * <table>
 * <tr>
 * <td><b>expression</b> (required String)</td>
 * <td>The Xpath expression to be evaluated. Cannot be null or blank.</td>
 * </tr>
 * <tr>
 * <td><b>input</b> (optional Object, defaults to the message payload)</td>
 * <td>The input data on which the expression is going to be evaluated. This is an optional argument, it defaults to the message payload if not provided.</td>
 * </tr>
 * <tr>
 * <td>Output type (optional String, defaults to ?STRING?)</td>
 * <td>When executing an XPath expression, a developer might have very different intents. Sometimes you want to retrieve actual data,
 * sometimes you just want to verify if a node exists. Also, the JAXP API (JSR-206) defines the standard way for a Java application to handle XML, and therefore,
 * how to execute XPath expressions. This API accounts for the different intents a developer might have and allows choosing from a list of possible output types.
 * We consider this to be a really useful features in JAXP, and we also consider that many Java developers that are familiar with this API would appreciate
 * that Mule accounts for this while hiding the rest of the API's complexity. That is why there's a third parameter (optional, String),
 * which will allow specifying one of the following:
 * <ul>
 * <li>BOOLEAN: returns the effective boolean value of the expression, as a java.lang.Boolean. This is the same as wrapping the expression in a call of the XPath boolean() function.</li>
 * <li>STRING: returns the result of the expression converted to a string, as a java.lang.String. This is the same as wrapping the expression in a call of the XPath string() function.</li>
 * <li>NUMBER: returns the result of the expression converted to a double as a java.lang.Double. This is the same as wrapping the expression in a call of the XPath number() function.</li>
 * <li>NODE: returns the result the result as a node object.</li>
 * <li>NODESET: returns a {@link NodeList}.</li>
 * </td>
 * </ul>
 * </tr>
 * </table>
 * <p/>
 * Input types
 * <p/>
 * This function supports the following input types:
 * <p/>
 * <li>
 * <ul>{@link Document}</ul>
 * <ul>{@link Node}</ul>
 * <ul>{@link InputSource}</ul>
 * <ul>{@link OutputHandler}</ul>
 * <ul>{@code byte[]}</ul>
 * <ul>{@link InputStream}</ul>
 * <ul>{@link XMLStreamReader}</ul>
 * <ul>{@link DelayedResult}</ul>
 * </li>
 * <p/>
 * <p/>
 * If the input if not of any of these types, then we'll attempt to use a registered transformer to transform the input into a DOM document or Node.
 * If no such transformer can be found, then an {@link IllegalArgumentException} will be thrown.
 * <p/>
 * This function will verify if the input is a consumable type (streams, readers, etc). Because evaluating the expression over a consumable input
 * will cause that source to be exhausted, in the cases in which the input value was the actual message payload
 * (no matter if it was given explicitly or by default), it will update the output message payload with the result obtained from consuming the input.
 *
 * @since 3.6.0
 */
public class XPath3Function implements ExpressionLanguageFunction
{

    private static final Logger LOGGER = LoggerFactory.getLogger(XPath3Function.class);

    private static final XPathReturnType DEFAULT_RETURN_TYPE = XPathReturnType.STRING;
    private static final DataType<?>[] SUPPORTED_TYPES = new DataType<?>[] {DataTypeFactory.create(Document.class), DataTypeFactory.create(Node.class)};
    private static final String SUPPORTED_TYPES_AS_STRING = Joiner.on(',').join(SUPPORTED_TYPES);

    private final MuleContext muleContext;
    private XPathEvaluator xpathEvaluator;
    private Supplier<XPathEvaluator> xpathEvaluatorSupplier = new LookupEvaluatorSupplier();

    XPath3Function(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public Object call(Object[] params, ExpressionLanguageContext context)
    {
        validateParams(params);

        final MessageContext ctx = context.getVariable("message");
        final String xpathExpression = getXpathExpression(params);
        final XPathReturnType returnType = getReturnType(params);
        final MuleEvent event = getMuleEvent(context);
        final Object input = getInput(params, event);

        try
        {
            Node node = toDOMNode(input, event);
            Object result = xpathEvaluatorSupplier.get().evaluate(xpathExpression, node, returnType, event);

            MuleMessage message = event.getMessage();
            if (input == message.getPayload() && isConsumable(message.getPayload().getClass()))
            {
                ctx.setPayload(node);
            }

            return result;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    private MuleEvent getMuleEvent(ExpressionLanguageContext context)
    {
        MuleEvent event = context.getVariable(MVELExpressionLanguageContext.MULE_EVENT_INTERNAL_VARIABLE);
        if (event == null)
        {
            event = RequestContext.getEvent();
        }

        checkState(event != null, "Could not obtain MuleEvent");
        return event;
    }

    private Node toDOMNode(Object input, MuleEvent event) throws Exception
    {
        if (input == null)
        {
            throw new IllegalArgumentException("Can't evaluate an XPath expression over a null input");
        }

        Node node = XMLUtils.toDOMNode(input, event);

        if (node == null)
        {
            node = (Node) TransformerUtils.transformToAny(input, muleContext, SUPPORTED_TYPES);
        }

        if (node == null)
        {
            throw new IllegalArgumentException(
                    String.format("Could not transform input of type '%s' to a supported one. Supported types are '%s'", input.getClass().getName(), SUPPORTED_TYPES_AS_STRING));
        }

        return node;
    }

    private Object getInput(Object[] params, MuleEvent event)
    {
        if (params.length >= 2)
        {
            return params[1];
        }

        return event.getMessage().getPayload();
    }

    private void validateParams(Object[] params)
    {
        checkArgument(params.length > 0 && params.length <= 3, String.format("xpath3() function accepts up to 3 arguments, but %s were provided instead", params.length));
    }

    private String getXpathExpression(Object[] params)
    {
        checkArgument(params[0] != null, "XPath expression cannot be null");
        checkArgument(params[0] instanceof String, "XPath expression must be a String");

        String expression = (String) params[0];
        checkArgument(!StringUtils.isBlank(expression), "XPath expression cannot be blank");

        return expression;
    }

    private XPathReturnType getReturnType(Object[] params)
    {
        if (params.length >= 3)
        {
            Object returnType = params[2];

            if (returnType == null)
            {
                return DEFAULT_RETURN_TYPE;
            }

            checkArgument(returnType instanceof String, "returnType argument must be of type String");
            try
            {
                return XPathReturnType.valueOf((String) returnType);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException(returnType + " is not a valid XPath return type", e);
            }
        }

        return DEFAULT_RETURN_TYPE;
    }

    private class LookupEvaluatorSupplier implements Supplier<XPathEvaluator>
    {

        @Override
        public synchronized XPathEvaluator get()
        {
            if (xpathEvaluator == null)
            {
                xpathEvaluator = new SaxonXpathEvaluator();
                try
                {
                    NamespaceManager namespaceManager = muleContext.getRegistry().lookupObject(NamespaceManager.class);
                    if (namespaceManager != null)
                    {
                        xpathEvaluator.registerNamespaces(namespaceManager);
                    }
                }
                catch (RegistrationException e)
                {
                    LOGGER.debug("No namespace manager found. Will not register any namespaces");
                }

                xpathEvaluatorSupplier = new FastEvaluatorSupplier();
            }

            return xpathEvaluator;
        }
    }

    private class FastEvaluatorSupplier implements Supplier<XPathEvaluator>
    {

        @Override
        public XPathEvaluator get()
        {
            return xpathEvaluator;
        }
    }
}
