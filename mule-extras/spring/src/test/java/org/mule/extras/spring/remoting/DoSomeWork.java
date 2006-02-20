package org.mule.extras.spring.remoting;

/**
 * A server-side component to receive and process ComplexData.
 */
public class DoSomeWork implements WorkInterface
{
    public String executeByteArray(byte[] input)
    {
        return executeString(new String(input));
    }

    public String executeString(String input)
    {
        String rval = "You said " + input;
        System.err.println("\n\n" + rval + "\n");
        return rval;
    }

    public ComplexData executeComplexity(ComplexData input)
    {
        System.err.println("DoSomeWork.executeComplexity(" + input + ")");
        input.setSomeString(input.getSomeString() + " Received");
        input.setSomeInteger(new Integer(input.getSomeInteger().intValue() + 1));
        return input;
    }
}
