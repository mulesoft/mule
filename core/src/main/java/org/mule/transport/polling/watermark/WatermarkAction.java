package org.mule.transport.polling.watermark;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.store.ObjectStore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract definition of a Watermark Action (Retrieve/Store). It has common attributes between watermark functionality
 * as well as common methods
 */
public abstract class WatermarkAction
{
    public static final String WATERMARK_RETRIEVED_ACTION_NAME = "Watermark Retrieved";
    public static final String WATERMARK_VARIABLE_ATTRIBUTE = "watermark variable";
    public static final String WATERMARK_VALUE_ATTRIBUTE = "watermark value";
    public static final String WATERMARK_STORED_ATTRIBUTE_NAME = "Watermark stored";


    /**
     * The mule context
     *
     * @see org.mule.transport.polling.watermark.builder.DefaultWatermarkConfiguration#muleContext
     */
    protected MuleContext muleContext;

    /**
     * The object store where the watermark is stored
     *
     * @see org.mule.transport.polling.watermark.builder.DefaultWatermarkConfiguration#objectStore
     */
    protected ObjectStore objectStore;

    /**
     * The watermark variable.
     *
     * @see org.mule.transport.polling.watermark.builder.DefaultWatermarkConfiguration#variable
     */
    protected String variable;

    protected WatermarkAction(MuleContext muleContext, ObjectStore objectStore, String variable)
    {
        this.muleContext = muleContext;
        this.objectStore = objectStore;
        this.variable = variable;
    }


    /**
     * Evaluates a mel expression. If the value is not an expression or if it is not valid then returns the same value.
     *
     * @param value The expression the user wrote in the xml. Can be an expression or not
     * @param event The mule event in which we need to evaluate the expression
     * @return The evaluated value
     */
    protected String evaluate(String value, MuleEvent event)
    {
        ExpressionManager expressionManager = getExpressionManager();
        if (expressionManager.isExpression(value) && expressionManager.isValidExpression(value))
        {
            return (String) expressionManager.evaluate(value, event);
        }
        return value;
    }

    protected ExpressionManager getExpressionManager()
    {
        return muleContext.getExpressionManager();
    }

    protected Map<String, String> createMetadata(String evaluatedVariable, Serializable watermarkValue)
    {
        Map<String, String> metaData = new HashMap<String, String>();
        metaData.put(WATERMARK_VARIABLE_ATTRIBUTE, evaluatedVariable);
        metaData.put(WATERMARK_VALUE_ATTRIBUTE, watermarkValue.toString());
        return metaData;
    }


}
