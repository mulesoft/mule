/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.scripting;


import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.NumberUtils;

/**
 * Component which accumulates successive numbers, by adding/subtracting/multiplying/dividing.  
 */
public class AccumulatorComponent implements Callable
{
    /** Operation to perform: "add", "subtract", "multiply", "divide" */
    private String operation = "add";
    
    private double accumulatedValue = 0;
    
    public Object onCall(MuleEventContext context) throws Exception
    {
        Object msg = context.getMessage().getPayload();

        double data = NumberUtils.toDouble(msg);
        if (data == NumberUtils.DOUBLE_ERROR)
        {
            throw new TransformerException(MessageFactory.createStaticMessage("Unable to convert message to double: " + msg));
        }
        
        if (operation.equalsIgnoreCase("add"))
        {
            accumulatedValue += data;
        }
        else if (operation.equalsIgnoreCase("subtract"))
        {
            accumulatedValue -= data;
        }
        else if (operation.equalsIgnoreCase("multiply"))
        {
            accumulatedValue *= data;
        }
        else if (operation.equalsIgnoreCase("divide"))
        {
            accumulatedValue /= data;
        }
        else 
        {
            throw new TransformerException(MessageFactory.createStaticMessage("Operation " + operation + " not recognized"));
        }

        // no auto-boxing
        return new Double(accumulatedValue);
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }
}
