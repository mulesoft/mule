/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring;

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

    @Override
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
            String namespace = ((Element) problem.getLocation().getSource()).getNamespaceURI();
            String message = "The element '" + element + "' does not have an associated Bean Definition Parser."
                    +"  Is the module or transport associated with " + namespace + " present on the classpath?";
            return new Problem(message, problem.getLocation(), problem.getParseState(), problem.getRootCause());
        }
        catch (Exception e)
        {
            // fall back to previous message
            return problem;
        }
    }

}
