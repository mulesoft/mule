package org.mule.test.usecases.axis.clientbridge;

/**
 * A server-side component to receive and process ComplexData.
 */
public class DoSomeWork implements WorkInterface
{
    public ComplexData executeComplexity(ComplexData input)
    {
        System.err.println("DoSomeWork.executeComplexity(" + input + ")");
        return input;
    }
}
