package org.mule.test.construct;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.List;

/**
 * transformer for tests involving transformation from
 * lists to string concatenated 
 *
 */
public class TestStringAppendTransformer extends StringAppendTransformer
{
    public TestStringAppendTransformer(String append)
    {
        super(append);
    }
    
    /**
     * Tranforms a list of string to its concatenation
     * 
     * @param args arguments to concatenate
     * @return arguments transformed as concatenation of string
     * @throws TransformerException
     */
    public Object transformArray(List<String> args) throws TransformerException
    {
        StringBuffer buffer = new StringBuffer();

        for (String arg : args)
        {
            buffer.append(arg);
        }

        return transform(buffer.toString());
    }

}
