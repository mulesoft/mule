package org.codehaus.xfire.aegis.type;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.aegis.type.basic.Base64Type;
import org.codehaus.xfire.aegis.type.basic.BigDecimalType;
import org.codehaus.xfire.aegis.type.basic.BigIntegerType;
import org.codehaus.xfire.aegis.type.basic.BooleanType;
import org.codehaus.xfire.aegis.type.basic.CalendarType;
import org.codehaus.xfire.aegis.type.basic.CharacterType;
import org.codehaus.xfire.aegis.type.basic.DateTimeType;
import org.codehaus.xfire.aegis.type.basic.DoubleType;
import org.codehaus.xfire.aegis.type.basic.FloatType;
import org.codehaus.xfire.aegis.type.basic.IntType;
import org.codehaus.xfire.aegis.type.basic.LongType;
import org.codehaus.xfire.aegis.type.basic.ObjectType;
import org.codehaus.xfire.aegis.type.basic.ShortType;
import org.codehaus.xfire.aegis.type.basic.SqlDateType;
import org.codehaus.xfire.aegis.type.basic.StringType;
import org.codehaus.xfire.aegis.type.basic.TimeType;
import org.codehaus.xfire.aegis.type.basic.TimestampType;
import org.codehaus.xfire.aegis.type.basic.URIType;
import org.codehaus.xfire.aegis.type.mtom.DataHandlerType;
import org.codehaus.xfire.aegis.type.mtom.DataSourceType;
import org.codehaus.xfire.aegis.type.xml.DocumentType;
import org.codehaus.xfire.aegis.type.xml.JDOMDocumentType;
import org.codehaus.xfire.aegis.type.xml.JDOMElementType;
import org.codehaus.xfire.aegis.type.xml.SourceType;
import org.codehaus.xfire.aegis.type.xml.XMLStreamReaderType;
import org.codehaus.xfire.soap.Soap11;
import org.codehaus.xfire.soap.SoapConstants;
import org.codehaus.xfire.util.ClassLoaderUtils;
import org.jdom.Element;
import org.w3c.dom.Document;

/**
 * TODO REMOVE THIS CLASS ONCE XFIRE 1.1.1 IS RELEASED. THIS IS A PATCHED
 * FILE, DETAILS AVAILABLE AT http://jira.codehaus.org/browse/XFIRE-409 .
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Feb 22, 2004
 */
public class DefaultTypeMappingRegistry
    implements TypeMappingRegistry
{
    private static final Log logger = LogFactory.getLog(DefaultTypeMappingRegistry.class);

    protected static final QName XSD_STRING = new QName(SoapConstants.XSD, "string", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_LONG = new QName(SoapConstants.XSD, "long", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_FLOAT = new QName(SoapConstants.XSD, "float", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_DOUBLE = new QName(SoapConstants.XSD, "double", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_CHAR = new QName(SoapConstants.XSD, "char", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_INT = new QName(SoapConstants.XSD, "int", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_SHORT = new QName(SoapConstants.XSD, "short", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_BOOLEAN = new QName(SoapConstants.XSD, "boolean", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_DATETIME = new QName(SoapConstants.XSD, "dateTime", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_TIME = new QName(SoapConstants.XSD, "dateTime", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_BASE64 = new QName(SoapConstants.XSD, "base64Binary", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_DECIMAL = new QName(SoapConstants.XSD, "decimal", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_INTEGER = new QName(SoapConstants.XSD, "integer", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_URI = new QName(SoapConstants.XSD, "anyURI", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_ANY = new QName(SoapConstants.XSD, "anyType", SoapConstants.XSD_PREFIX);
    
    protected static final QName XSD_DATE = new QName(SoapConstants.XSD, "date", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_DURATION = new QName(SoapConstants.XSD, "duration", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_G_YEAR_MONTH = new QName(SoapConstants.XSD, "gYearMonth", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_G_MONTH_DAY = new QName(SoapConstants.XSD, "gMonthDay", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_G_YEAR = new QName(SoapConstants.XSD, "gYear", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_G_MONTH = new QName(SoapConstants.XSD, "gMonth", SoapConstants.XSD_PREFIX);
    protected static final QName XSD_G_DAY = new QName(SoapConstants.XSD, "gDay", SoapConstants.XSD_PREFIX);

    protected static final String ENCODED_NS = Soap11.getInstance().getSoapEncodingStyle();
    protected static final QName ENCODED_STRING = new QName(ENCODED_NS, "string");
    protected static final QName ENCODED_LONG = new QName(ENCODED_NS, "long");
    protected static final QName ENCODED_FLOAT = new QName(ENCODED_NS, "float");
    protected static final QName ENCODED_CHAR = new QName(ENCODED_NS, "char");
    protected static final QName ENCODED_DOUBLE = new QName(ENCODED_NS, "double");
    protected static final QName ENCODED_INT = new QName(ENCODED_NS, "int");
    protected static final QName ENCODED_SHORT = new QName(ENCODED_NS, "short");
    protected static final QName ENCODED_BOOLEAN = new QName(ENCODED_NS, "boolean");
    protected static final QName ENCODED_DATETIME = new QName(ENCODED_NS, "dateTime");
    protected static final QName ENCODED_BASE64 = new QName(ENCODED_NS, "base64Binary");
    protected static final QName ENCODED_DECIMAL = new QName(ENCODED_NS, "decimal");
    protected static final QName ENCODED_INTEGER = new QName(ENCODED_NS, "integer");

    private Hashtable registry;
    
    private TypeMapping defaultTM;

    private TypeCreator typeCreator;

    private Configuration typeConfiguration;
    
    public DefaultTypeMappingRegistry()
    {
        this(false);
    }

    public DefaultTypeMappingRegistry(boolean createDefault)
    {
        this(null, createDefault);
    }

    public DefaultTypeMappingRegistry(TypeCreator typeCreator, boolean createDefault)
    {
        registry = new Hashtable();

        this.typeCreator = typeCreator;
        this.typeConfiguration = new Configuration();
        
        if (createDefault)
        {
            createDefaultMappings();
        }
    }

    public TypeMapping register(String encodingStyleURI, TypeMapping mapping)
    {
        TypeMapping previous = (TypeMapping) registry.get(encodingStyleURI);

        mapping.setEncodingStyleURI(encodingStyleURI);

        registry.put(encodingStyleURI, mapping);

        return previous;
    }

    public void registerDefault(TypeMapping mapping)
    {
        defaultTM = mapping;
    }

    /**
     * @see org.codehaus.xfire.aegis.type.TypeMappingRegistry#getDefaultTypeMapping()
     */
    public TypeMapping getDefaultTypeMapping()
    {
        return defaultTM;
    }

    /**
     * @see org.codehaus.xfire.aegis.type.TypeMappingRegistry#getRegisteredEncodingStyleURIs()
     */
    public String[] getRegisteredEncodingStyleURIs()
    {
        return (String[]) registry.keySet().toArray(new String[registry.size()]);
    }

    /**
     * @see org.codehaus.xfire.aegis.type.TypeMappingRegistry#getTypeMapping(java.lang.String)
     */
    public TypeMapping getTypeMapping(String encodingStyleURI)
    {
        return (TypeMapping) registry.get(encodingStyleURI);
    }

    /**
     * @see org.codehaus.xfire.aegis.type.TypeMappingRegistry#createTypeMapping(boolean)
     */
    public TypeMapping createTypeMapping(boolean autoTypes)
    {
        return createTypeMapping(getDefaultTypeMapping(), autoTypes);
    }

    /**
     * @see org.codehaus.xfire.aegis.type.TypeMappingRegistry#createTypeMapping(String,
     *      boolean)
     */
    public TypeMapping createTypeMapping(String parentNamespace, boolean autoTypes)
    {
        return createTypeMapping(getTypeMapping(parentNamespace), autoTypes);
    }

    protected TypeMapping createTypeMapping(TypeMapping parent, boolean autoTypes)
    {
        CustomTypeMapping tm = new CustomTypeMapping(parent);

        if (autoTypes)
            tm.setTypeCreator(getTypeCreator());

        return tm;
    }

    public TypeCreator getTypeCreator()
    {
        if (typeCreator == null)
        {
            typeCreator = createTypeCreator();
        }

        return typeCreator;
    }

    public void setTypeCreator(TypeCreator typeCreator)
    {
        this.typeCreator = typeCreator;
    }

    protected TypeCreator createTypeCreator()
    {
        AbstractTypeCreator xmlCreator = createRootTypeCreator();
        xmlCreator.setNextCreator(createDefaultTypeCreator());
        
        if (isJDK5andAbove())
        {
            try
            {
                String j5TC = "org.codehaus.xfire.aegis.type.java5.Java5TypeCreator";

                Class clazz = ClassLoaderUtils.loadClass(j5TC, getClass());

                AbstractTypeCreator j5Creator = (AbstractTypeCreator) clazz.newInstance();
                j5Creator.setNextCreator(createDefaultTypeCreator());
                j5Creator.setConfiguration(getConfiguration());
                xmlCreator.setNextCreator(j5Creator);
            }
            catch (Throwable t)
            {
                logger.info("Couldn't find Java 5 module on classpath. Annotation mappings will not be supported.");

                if (!(t instanceof ClassNotFoundException))
                    logger.debug("Error loading Java 5 module", t);
            }
        }
        
        return xmlCreator;
    }

    boolean isJDK5andAbove()
    {
        String v = System.getProperty("java.class.version", "44.0");
        return ("49.0".compareTo(v) <= 0);
    }

    protected AbstractTypeCreator createRootTypeCreator()
    {
        AbstractTypeCreator creator = new XMLTypeCreator();
        creator.setConfiguration(getConfiguration());
        return creator;
    }

    protected AbstractTypeCreator createDefaultTypeCreator()
    {
        AbstractTypeCreator creator = new DefaultTypeCreator();
        creator.setConfiguration(getConfiguration());
        return creator;
    }

    /**
     * @see org.codehaus.xfire.aegis.type.TypeMappingRegistry#unregisterTypeMapping(java.lang.String)
     */
    public TypeMapping unregisterTypeMapping(String encodingStyleURI)
    {
        TypeMapping tm = (TypeMapping) registry.get(encodingStyleURI);
        registry.remove(encodingStyleURI);
        return tm;
    }

    public boolean removeTypeMapping(TypeMapping mapping)
    {
        int n = 0;

        for (Iterator itr = registry.values().iterator(); itr.hasNext();)
        {
            if (itr.next().equals(mapping))
            {
                itr.remove();
                n++;
            }
        }

        return (n > 0);
    }

    /**
     * @see org.codehaus.xfire.aegis.type.TypeMappingRegistry#clear()
     */
    public void clear()
    {
        registry.clear();
    }

    public TypeMapping createDefaultMappings()
    {
        TypeMapping tm = createTypeMapping(false);

        register(tm, boolean.class, XSD_BOOLEAN, new BooleanType());
        register(tm, int.class, XSD_INT, new IntType());
        register(tm, short.class, XSD_SHORT, new ShortType());
        register(tm, double.class, XSD_DOUBLE, new DoubleType());
        register(tm, float.class, XSD_FLOAT, new FloatType());
        register(tm, long.class, XSD_LONG, new LongType());
        register(tm, char.class,XSD_CHAR, new CharacterType());
        register(tm, Character.class,XSD_CHAR, new CharacterType());
        register(tm, String.class, XSD_STRING, new StringType());
        register(tm, Boolean.class, XSD_BOOLEAN, new BooleanType());
        register(tm, Integer.class, XSD_INT, new IntType());
        register(tm, Short.class, XSD_SHORT, new ShortType());
        register(tm, Double.class, XSD_DOUBLE, new DoubleType());
        register(tm, Float.class, XSD_FLOAT, new FloatType());
        register(tm, Long.class, XSD_LONG, new LongType());
        register(tm, Date.class, XSD_DATETIME, new DateTimeType());
        register(tm, java.sql.Date.class, XSD_DATETIME, new SqlDateType());
        register(tm, Time.class, XSD_TIME, new TimeType());
        register(tm, Timestamp.class, XSD_DATETIME, new TimestampType());
        register(tm, Calendar.class, XSD_DATETIME, new CalendarType());
        register(tm, byte[].class, XSD_BASE64, new Base64Type());
        register(tm, BigDecimal.class, XSD_DECIMAL, new BigDecimalType());
        register(tm, BigInteger.class, XSD_INTEGER, new BigIntegerType());
        register(tm, URI.class, XSD_URI, new URIType());
        register(tm, Document.class, XSD_ANY, new DocumentType());
        register(tm, Source.class, XSD_ANY, new SourceType());
        register(tm, XMLStreamReader.class, XSD_ANY, new XMLStreamReaderType());
        register(tm, Element.class, XSD_ANY, new JDOMElementType());
        register(tm, org.jdom.Document.class, XSD_ANY, new JDOMDocumentType());
        register(tm, Object.class, XSD_ANY, new ObjectType());
        register(tm, DataSource.class, XSD_BASE64, new DataSourceType());
        register(tm, DataHandler.class, XSD_BASE64, new DataHandlerType());

        if ( isJDK5andAbove())
        {
            registerIfAvailable(tm, "javax.xml.datatype.Duration", XSD_DURATION, "org.codehaus.xfire.aegis.type.java5.DurationType");
            registerIfAvailable(tm, "javax.xml.datatype.XMLGregorianCalendar", XSD_DATE, "org.codehaus.xfire.aegis.type.java5.XMLGregorianCalendarType");
            registerIfAvailable(tm, "javax.xml.datatype.XMLGregorianCalendar", XSD_TIME, "org.codehaus.xfire.aegis.type.java5.XMLGregorianCalendarType");
            registerIfAvailable(tm, "javax.xml.datatype.XMLGregorianCalendar", XSD_G_DAY, "org.codehaus.xfire.aegis.type.java5.XMLGregorianCalendarType");
            registerIfAvailable(tm, "javax.xml.datatype.XMLGregorianCalendar", XSD_G_MONTH, "org.codehaus.xfire.aegis.type.java5.XMLGregorianCalendarType");
            registerIfAvailable(tm, "javax.xml.datatype.XMLGregorianCalendar", XSD_G_MONTH_DAY, "org.codehaus.xfire.aegis.type.java5.XMLGregorianCalendarType");
            registerIfAvailable(tm, "javax.xml.datatype.XMLGregorianCalendar", XSD_G_YEAR, "org.codehaus.xfire.aegis.type.java5.XMLGregorianCalendarType");
            registerIfAvailable(tm, "javax.xml.datatype.XMLGregorianCalendar", XSD_G_YEAR_MONTH, "org.codehaus.xfire.aegis.type.java5.XMLGregorianCalendarType");
            registerIfAvailable(tm, "javax.xml.datatype.XMLGregorianCalendar", XSD_DATETIME, "org.codehaus.xfire.aegis.type.java5.XMLGregorianCalendarType");
        }
        
        register(SoapConstants.XSD, tm);
        registerDefault(tm);

        // Create a Type Mapping for SOAP 1.1 Encoding
        TypeMapping soapTM = createTypeMapping(tm, false);

        register(soapTM, boolean.class, ENCODED_BOOLEAN, new BooleanType());
        register(soapTM, int.class, ENCODED_INT, new IntType());
        register(soapTM, short.class, ENCODED_SHORT, new ShortType());
        register(soapTM, double.class, ENCODED_DOUBLE, new DoubleType());
        register(soapTM, float.class, ENCODED_FLOAT, new FloatType());
        register(soapTM, long.class, ENCODED_LONG, new LongType());
        register(soapTM, char.class,ENCODED_CHAR,new CharacterType());
        register(soapTM, Character.class,ENCODED_CHAR,new CharacterType());
        register(soapTM, String.class, ENCODED_STRING, new StringType());
        register(soapTM, Boolean.class, ENCODED_BOOLEAN, new BooleanType());
        register(soapTM, Integer.class, ENCODED_INT, new IntType());
        register(soapTM, Short.class, ENCODED_SHORT, new ShortType());
        register(soapTM, Double.class, ENCODED_DOUBLE, new DoubleType());
        register(soapTM, Float.class, ENCODED_FLOAT, new FloatType());
        register(soapTM, Long.class, ENCODED_LONG, new LongType());
        register(soapTM, Date.class, ENCODED_DATETIME, new DateTimeType());
        register(soapTM, java.sql.Date.class, ENCODED_DATETIME, new SqlDateType());
        register(soapTM, Calendar.class, ENCODED_DATETIME, new CalendarType());
        register(soapTM, byte[].class, ENCODED_BASE64, new Base64Type());
        register(soapTM, BigDecimal.class, ENCODED_DECIMAL, new BigDecimalType());
        register(soapTM, BigInteger.class, ENCODED_INTEGER, new BigIntegerType());
        
        register(soapTM, boolean.class, XSD_BOOLEAN, new BooleanType());
        register(soapTM, int.class, XSD_INT, new IntType());
        register(soapTM, short.class, XSD_SHORT, new ShortType());
        register(soapTM, double.class, XSD_DOUBLE, new DoubleType());
        register(soapTM, float.class, XSD_FLOAT, new FloatType());
        register(soapTM, long.class, XSD_LONG, new LongType());
        register(soapTM, String.class, XSD_STRING, new StringType());
        register(soapTM, Boolean.class, XSD_BOOLEAN, new BooleanType());
        register(soapTM, Integer.class, XSD_INT, new IntType());
        register(soapTM, Short.class, XSD_SHORT, new ShortType());
        register(soapTM, Double.class, XSD_DOUBLE, new DoubleType());
        register(soapTM, Float.class, XSD_FLOAT, new FloatType());
        register(soapTM, Long.class, XSD_LONG, new LongType());
        register(soapTM, Date.class, XSD_DATETIME, new DateTimeType());
        register(soapTM, java.sql.Date.class, XSD_DATETIME, new SqlDateType());
        register(soapTM, Time.class, XSD_TIME, new TimeType());
        register(soapTM, Timestamp.class, XSD_DATETIME, new TimestampType());
        register(soapTM, Calendar.class, XSD_DATETIME, new CalendarType());
        register(soapTM, byte[].class, XSD_BASE64, new Base64Type());
        register(soapTM, BigDecimal.class, XSD_DECIMAL, new BigDecimalType());
        register(soapTM, URI.class, XSD_URI, new URIType());
        register(soapTM, Document.class, XSD_ANY, new DocumentType());
        register(soapTM, Source.class, XSD_ANY, new SourceType());
        register(soapTM, XMLStreamReader.class, XSD_ANY, new XMLStreamReaderType());
        register(soapTM, Element.class, XSD_ANY, new JDOMElementType());
        register(soapTM, org.jdom.Document.class, XSD_ANY, new JDOMDocumentType());
        register(soapTM, Object.class, XSD_ANY, new ObjectType());
        register(soapTM, DataSource.class, XSD_BASE64, new DataSourceType());
        register(soapTM, DataHandler.class, XSD_BASE64, new DataHandlerType());
        register(soapTM, BigInteger.class, XSD_INTEGER, new BigIntegerType());
        
        register(ENCODED_NS, soapTM);

        return tm;
    }

    protected void registerIfAvailable(TypeMapping tm, String className, QName typeName, String typeClassName)
    {
        try
        {
            Class cls = ClassLoaderUtils.loadClass(className, getClass());
            Class typeCls = ClassLoaderUtils.loadClass(typeClassName, getClass());
            try
            {
                Type type = (Type) typeCls.newInstance();
                
                register(tm, cls, typeName, type);
            }
            catch (InstantiationException e)
            {
                throw new XFireRuntimeException("Couldn't instantiate Type ", e);
            }
            catch (IllegalAccessException e)
            {
                throw new XFireRuntimeException("Couldn't instantiate Type ", e);
            }
        }
        catch (ClassNotFoundException e)
        {
            logger.debug("Could not find optional Type " + className +". Skipping.");
        }
        
    }

    protected void register(TypeMapping tm, Class class1, QName name, Type type)
    {
        if (!getConfiguration().isDefaultNillable())
        {
            type.setNillable(false);
        }
        
        tm.register(class1, name, type);
    }

    public Configuration getConfiguration()
    {
        return typeConfiguration;
    }

    public void setConfiguration(Configuration typeConfiguration)
    {
        this.typeConfiguration = typeConfiguration;
    }

}
