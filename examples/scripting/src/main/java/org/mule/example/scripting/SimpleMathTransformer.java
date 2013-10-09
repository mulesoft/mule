/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.scripting;


import org.mule.api.transformer.DataType;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.NumberUtils;

/**
 * A simple transformer which adds/subtracts/multiplies/divides a constant factor to numeric messages.
 */
public class SimpleMathTransformer extends AbstractTransformer
{
    /** Operation to perform: "add", "subtract", "multiply", "divide" */
    private String operation = "add";
    
    /** Factor to be applied */
    private double factor;
    
    public SimpleMathTransformer()
    {
        DataType<Number> numberDataType = DataTypeFactory.create(Number.class);
        registerSourceType(numberDataType);
        setReturnDataType(numberDataType);
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        double data = NumberUtils.toDouble(src);
        if (data == NumberUtils.DOUBLE_ERROR)
        {
            throw new TransformerException(MessageFactory.createStaticMessage("Unable to convert message to double: " + src));
        }
        
        double result;
        if (operation.equalsIgnoreCase("add"))
        {
            result = data + factor;
        }
        else if (operation.equalsIgnoreCase("subtract"))
        {
            result = data - factor;
        }
        else if (operation.equalsIgnoreCase("multiply"))
        {
            result = data * factor;
        }
        else if (operation.equalsIgnoreCase("divide"))
        {
            result = data / factor;
        }
        else
        {
            throw new TransformerException(MessageFactory.createStaticMessage("Operation " + operation + " not recognized"));
        }
        
        // no auto-boxing
        return new Double(result);
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }

    public double getFactor()
    {
        return factor;
    }

    public void setFactor(double factor)
    {
        this.factor = factor;
    }
}
