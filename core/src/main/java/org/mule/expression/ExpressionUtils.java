package org.mule.expression;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;

/**
 *
 */
public final class ExpressionUtils
{
    private ExpressionUtils()
    {
        // don't instantiate
    }

    /**
     * Handlers scope-aware expressions like "#[header:INBOUND:foo]
     */
    public static Object getPropertyWithScope(String expression, MuleMessage msg)
    {
        // see if scope has been specified explicitly
        final String[] tokens = expression.split(":", 2); // note we split only once, not on every separator
        // default
        PropertyScope scope = PropertyScope.OUTBOUND;
        if (tokens.length == 2)
        {
            final String candidate = tokens[0];
            scope = PropertyScope.get(candidate.toLowerCase());
            if (scope == null)
            {
                throw new IllegalArgumentException(String.format("'%s' is not a valid property scope.", candidate));
            }

            // cut-off leading scope and separator
            expression = expression.substring(candidate.length() + 1);
        }

        return msg.getProperty(expression, scope);
    }
}
