package org.mule.transport.polling.watermark.builder;


import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.context.notification.NotificationException;
import org.mule.transport.polling.watermark.Watermark;
import org.mule.transport.polling.watermark.WatermarkPipelineListener;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * The default configuration of the watermark. If the user defines a watermark configuration in the xml, then this class
 * is instantiated
 */
public class DefaultWatermarkFactory implements WatermarkFactory, MuleContextAware
{

    public static final String WATERMARK_OBJECT_STORE_NAME = "watermarkObjectStore";
    /**
     * Logger to notify errors.
     */
    private static Log logger = LogFactory.getLog(DefaultWatermarkFactory.class);

    /**
     * The watermark variable that will end up being the object store key. This variable is also the name of the flow
     * variable in the flow construct.
     */
    private String variable;

    /**
     * The default expression to update the flow variable in case it is not in the object store or it fails to retrieve
     * it.
     */
    private String defaultExpression;

    /**
     * The update expression to update the watermark value in the object store.
     * It is optional so it can be null.
     */
    private String updateExpression;

    /**
     * The object store instance.
     * The default value is the persistent user object store.
     */
    private ObjectStore objectStore;

    /**
     * The mule context instance
     */
    private MuleContext muleContext;

    /**
     * The configuration annotations
     */
    private Map<QName, Object> annotations = new HashMap<QName, Object>();


    public Watermark buildFor(FlowConstruct flowConstruct)
    {
        checkDefaultObjectStore();

        Watermark watermark = Watermark.create(muleContext, objectStore, variable, defaultExpression,
                                               updateExpression, annotations);

        try
        {
            muleContext.registerListener(new WatermarkPipelineListener(watermark, flowConstruct));
        }
        catch (NotificationException e)
        {
            logger.error("The watermark processor could not be registered, the watermark will not be updated at the end" +
                         "of the flow.");
        }

        return watermark;
    }

    private void checkDefaultObjectStore()
    {
        if ( objectStore == null ){
            ObjectStoreManager objectStoreManager = (ObjectStoreManager) muleContext.getRegistry().get(
                    MuleProperties.OBJECT_STORE_MANAGER);
            objectStore = objectStoreManager.getObjectStore(WATERMARK_OBJECT_STORE_NAME);
        }
    }


    public void setVariable(String variable)
    {
        this.variable = variable;
    }

    public void setDefaultExpression(String defaultExpression)
    {
        this.defaultExpression = defaultExpression;
    }

    public void setUpdateExpression(String updateExpression)
    {
        this.updateExpression = updateExpression;
    }

    public void setObjectStore(ObjectStore objectStore)
    {
        this.objectStore = objectStore;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
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
        this.annotations = annotations;
    }
}
