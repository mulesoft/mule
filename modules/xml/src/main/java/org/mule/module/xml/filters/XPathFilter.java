
package org.mule.module.xml.filters;

import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.config.i18n.MessageFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;

/**
 * @author Ryan Heaton
 */
public class XPathFilter extends AbstractJaxpFilter implements Filter, Initialisable
{

    protected transient Log logger = LogFactory.getLog(getClass());

    private String pattern;
    private String expectedValue;
    private XPath xpath;
    private Map<String, String> prefixToNamespaceMap = null;

    public XPathFilter()
    {
        super();
    }

    public XPathFilter(String pattern)
    {
        this.pattern = pattern;
    }

    public XPathFilter(String pattern, String expectedValue)
    {
        this.pattern = pattern;
        this.expectedValue = expectedValue;
    }

    public void initialise() throws InitialisationException
    {
        super.initialise();
        
        if (getXpath() == null)
        {
            setXpath(XPathFactory.newInstance().newXPath());
        }


        if (pattern == null)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("A pattern must be supplied to the StandardXPathFilter."),
                this);
        }

        final Map<String, String> prefixToNamespaceMap = this.prefixToNamespaceMap;
        if (prefixToNamespaceMap != null)
        {
            getXpath().setNamespaceContext(new NamespaceContext()
            {
                public String getNamespaceURI(String prefix)
                {
                    return prefixToNamespaceMap.get(prefix);
                }

                public String getPrefix(String namespaceURI)
                {

                    for (Map.Entry<String, String> entry : prefixToNamespaceMap.entrySet())
                    {
                        if (namespaceURI.equals(entry.getValue()))
                        {
                            return entry.getKey();
                        }
                    }

                    return null;
                }

                public Iterator getPrefixes(String namespaceURI)
                {
                    String prefix = getPrefix(namespaceURI);
                    if (prefix == null)
                    {
                        return Collections.emptyList().iterator();
                    }
                    else
                    {
                        return Arrays.asList(prefix).iterator();
                    }
                }
            });
        }

        if (logger.isInfoEnabled())
        {
            logger.info("XPath implementation: " + getXpath());
            logger.info("DocumentBuilderFactory implementation: " + getDocumentBuilderFactory());
        }
    }

    public boolean accept(MuleMessage message)
    {
        Object payload = message.getPayload();
        if (payload == null)
        {
            logger.warn("Applying StandardXPathFilter to null object.");
            return false;
        }
        if (pattern == null)
        {
            logger.warn("Expression for StandardXPathFilter is not set.");
            return false;
        }
        if (expectedValue == null)
        {
            // Handle the special case where the expected value really is null.
            if (pattern.endsWith("= null") || pattern.endsWith("=null"))
            {
                expectedValue = "null";
                pattern = pattern.substring(0, pattern.lastIndexOf("="));
            }
            else
            {
                if (logger.isInfoEnabled())
                {
                    logger.info("Expected value for StandardXPathFilter is not set, using 'true' by default");
                }
                expectedValue = Boolean.TRUE.toString();
            }
        }

        Node node;
        try
        {
            node = toDOMNode(payload);
        }
        catch (Exception e)
        {
            logger.warn("StandardXPathFilter filter rejected message because of an error while parsing XML: "
                        + e.getMessage(), e);
            return false;
        }

        message.setPayload(node);

        return accept(node);
    }

    protected boolean accept(Node node)
    {
        Object xpathResult;
        boolean accept = false;

        try
        {
            xpathResult = getXpath().evaluate(pattern, node, XPathConstants.STRING);
        }
        catch (Exception e)
        {
            logger.warn(
                "StandardXPathFilter filter rejected message because of an error while evaluating the expression: "
                                + e.getMessage(), e);
            return false;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("StandardXPathFilter Expression result = '" + xpathResult
                         + "' -  Expected value = '" + expectedValue + "'");
        }

        // Compare the XPath result with the expected result.
        if (xpathResult != null && !"".equals(xpathResult))
        {
            accept = xpathResult.toString().equals(expectedValue);
        }
        else
        {
            // A null result was actually expected.
            if (expectedValue.equals("null"))
            {
                accept = true;
            }
            // A null result was not expected, something probably went wrong.
            else
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("StandardXPathFilter expression evaluates to null: " + pattern);
                }
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("StandardXPathFilter accept object  : " + accept);
        }

        return accept;
    }

    /**
     * @return XPath expression
     */
    public String getPattern()
    {
        return pattern;
    }

    /**
     * @param pattern The XPath expression
     */
    public void setPattern(String pattern)
    {
        this.pattern = pattern;
    }

    /**
     * @return The expected result value of the XPath expression
     */
    public String getExpectedValue()
    {
        return expectedValue;
    }

    /**
     * Sets the expected result value of the XPath expression
     * 
     * @param expectedValue The expected value.
     */
    public void setExpectedValue(String expectedValue)
    {
        this.expectedValue = expectedValue;
    }

    /**
     * The xpath object to use to evaluate the expression.
     * 
     * @return The xpath object to use to evaluate the expression.
     */
    public XPath getXpath()
    {
        return xpath;
    }

    /**
     * The xpath object to use to evaluate the expression.
     * 
     * @param xpath The xpath object to use to evaluate the expression.
     */
    public void setXpath(XPath xpath)
    {
        this.xpath = xpath;
    }


    /**
     * The prefix-to-namespace map for the namespace context to be applied to the
     * XPath evaluation.
     * 
     * @return The prefix-to-namespace map for the namespace context to be applied to
     *         the XPath evaluation.
     */
    public Map<String, String> getNamespaces()
    {
        return prefixToNamespaceMap;
    }

    /**
     * The prefix-to-namespace map for the namespace context to be applied to the
     * XPath evaluation.
     * 
     * @param prefixToNamespaceMap The prefix-to-namespace map for the namespace
     *            context to be applied to the XPath evaluation.
     */
    public void setNamespaces(Map<String, String> prefixToNamespaceMap)
    {
        this.prefixToNamespaceMap = prefixToNamespaceMap;
    }
}
