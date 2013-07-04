package org.mule.transport.polling.watermark;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.store.ObjectStore;
import org.mule.context.notification.CustomMetadataNotification;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Abstract definition of a Watermark Action (Retrieve/Store). It has common attributes between watermark functionality
 * as well as common methods
 */
public class Watermark implements AnnotatedObject
{

    public static final String WATERMARK_RETRIEVED_ACTION_NAME = "Watermark Retrieved";
    public static final String WATERMARK_VARIABLE_ATTRIBUTE = "watermark variable";
    public static final String WATERMARK_VALUE_ATTRIBUTE = "watermark value";
    public static final String WATERMARK_STORED_ATTRIBUTE_NAME = "Watermark stored";


    /**
     * Factory to create a WatermarkRetrieveMessageProcessor
     */
    public static Watermark create(MuleContext muleContext,
                                         ObjectStore objectStore,
                                         String variable,
                                         String defaultExpression,
                                         Map<QName, Object> annotations)
    {
        Watermark watermark =
                new Watermark(muleContext, objectStore, variable, defaultExpression);
        watermark.setAnnotations(annotations);

        return watermark;
    }


    /**
     * The default expression to update the watermark variable
     *
     * @see org.mule.transport.polling.watermark.builder.DefaultWatermarkConfiguration#defaultExpression
     */
    private String defaultExpression;

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

    /**
     * The watermark annotations
     */
    protected Map<QName, Object> annotations = new HashMap<QName, Object>();


    protected Watermark(MuleContext muleContext, ObjectStore objectStore, String variable, String defaultExpression)
    {
        this.muleContext = muleContext;
        this.objectStore = objectStore;
        this.variable = variable;
        this.defaultExpression = defaultExpression;
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

    @Override
    public Object getAnnotation(QName name)
    {
        return annotations.get(name);
    }

    @Override
    public Map<QName, Object> getAnnotations()
    {
        return annotations;
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations)
    {
        if (annotations != null)
        {
            this.annotations = annotations;
        }
    }

    /**
     * If the object store contains a value associated with the configured variable then it stores that value as a flow
     * variable in the {@link MuleEvent}, if there is no value associated then evaluates the defaultExpression and stores
     * that value as flow variable.
     *
     * @param event MuleEvent to be processed
     * @return The processed {@link MuleEvent}
     * @throws org.mule.api.MuleException Does not throw any exception
     */
    @Override
    public MuleEvent retrieve(MuleEvent event) throws MuleException
    {
        String evaluatedVariable = evaluate(variable, event);
        Serializable watermarkValue;
        if (objectStore.contains(evaluatedVariable))
        {
            watermarkValue = objectStore.retrieve(evaluatedVariable);
        }
        else
        {
            watermarkValue = evaluate(defaultExpression, event);
        }
        event.getMessage().setInvocationProperty(evaluatedVariable, watermarkValue);

        muleContext.fireNotification(new CustomMetadataNotification(event, this, WATERMARK_RETRIEVED_ACTION_NAME, createMetadata(evaluatedVariable, watermarkValue)));

        return event;
    }


}
