/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.util.StringUtils;

import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.beans.factory.parsing.Problem;
import org.w3c.dom.Element;

/**
 * A very simple extension to {@link org.springframework.beans.factory.parsing.FailFastProblemReporter}
 * that intercepts errors related to missing definition parsers to give a more helpful message.
 * In the future we may want to replace this by something more sophisticated that allows
 * different problems to be resolved by different "pluggable" components...
 */
public class MissingParserProblemReporter extends FailFastProblemReporter
{

    public static final String NO_PARSER_PREFIX = "Cannot locate BeanDefinitionParser";
    public static final String MULESOURCE = "mulesource.org";
    public static final String CORE = "/core/";
    public static final String XML = "xml";

    // @Override
    public void fatal(Problem problem)
    {
        if (isMissingParser(problem))
        {
            problem = extendProblemDetails(problem);
        }
        super.fatal(problem);
    }

    protected boolean isMissingParser(Problem problem)
    {
        // Spring doesn't give us much useful data here - parseState and rootCause are null
        String message = problem.getMessage();
        return (null != message && message.startsWith(NO_PARSER_PREFIX));
    }

    protected Problem extendProblemDetails(Problem problem)
    {
        try
        {
            String element = ((Element) problem.getLocation().getSource()).getLocalName();
            String message = "The element '" + element + "' does not have an associated Bean Definition Parser.";
            String namespace = ((Element) problem.getLocation().getSource()).getNamespaceURI();
            if (StringUtils.contains(namespace, MULESOURCE) && StringUtils.contains(namespace, CORE) &&
                    StringUtils.contains(element, XML))

            {
                // we have a problem with XML
                message += "  You appear to be using XML functionality from the Mule core."
                        + "  Is the XML module present on the classpath?";
            }
            else
            {
                message += "  Is the module or transport associated with " + namespace + " present on the classpath?";
            }
            return new Problem(message, problem.getLocation(), problem.getParseState(), problem.getRootCause());
        }
        catch (Exception e)
        {
            // fall back to previous message
            return problem;
        }
    }

}
