package org.mule.transport.polling.watermark.builder;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.transport.polling.watermark.Watermark;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;


/**
 * Null Pattern. If no watermark configuration is defined in the xml, then this class is instantiated.
 *
 * @since 3.5.0
 */
public class NullWatermarkFactory implements WatermarkFactory
{
    @Override
    public Object getAnnotation(QName name)
    {
        return null;
    }

    @Override
    public Map<QName, Object> getAnnotations()
    {
        return new HashMap<QName, Object>();
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations)
    {
        // does nothing
    }

    @Override
    public Watermark createFor(FlowConstruct flowConstruct)
    {
        return new Watermark(null,null,null,null,null){
            @Override
            public MuleEvent retrieve(MuleEvent event) throws MuleException
            {
                return event;
            }

            @Override
            public void store(MuleEvent event) throws MuleException
            {
                // DOES NOTHING...
            }
        };
    }
}
