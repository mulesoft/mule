package org.mule.jbi.engines.agila;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.agila.bpel.deployer.exception.DeploymentException;
import org.apache.agila.bpel.deployer.priv.ActivityDeployer;
import org.apache.agila.bpel.deployer.priv.ActivityDeployerFactory;
import org.apache.agila.bpel.deployer.priv.context.DeployerContext;
import org.apache.agila.bpel.deployer.priv.validate.bpel.BPELValidator;
import org.apache.agila.bpel.deployer.priv.validate.wsdl.WSDLValidator;
import org.apache.agila.bpel.engine.common.persistence.CreationException;
import org.apache.agila.bpel.engine.common.persistence.DBSessionException;
import org.apache.agila.bpel.engine.common.persistence.XMLDataAccess;
import org.apache.agila.bpel.engine.common.transaction.TransactionException;
import org.apache.agila.bpel.engine.common.transaction.TransactionManager;
import org.apache.agila.bpel.engine.priv.core.definition.AgilaProcess;
import org.apache.agila.bpel.engine.priv.core.definition.ProcessFactory;
import org.apache.agila.bpel.engine.priv.core.definition.Property;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.XPath;
import org.dom4j.io.SAXReader;
import org.xmldb.api.base.Collection;

/**
 * 
 */

public class AgilaDeployer {
		
    public static final String NS_SEPARATOR = ":";
    public static final String SPACE = " ";
    public static final String XMLNS = "xmlns";
    public static final String DEFINITIONS = "definitions";

    private Logger log = Logger.getLogger(AgilaDeployer.class);
    private static final String BPEL_NS = "http://schemas.xmlsoap.org/ws/2003/03/business-process/";

    private WSDLValidator wsdlValidator = null;
    private BPELValidator bpelValidator = null;

    public void deploy(Document processDoc, Document definitionDoc) throws DeploymentException {
        try {
            log.debug("begin transaction");
            TransactionManager.beginTransaction();

            Element processElement = processDoc.getRootElement();
            log.debug("<process>");
            AgilaProcess tp = deployProcessElement(processElement);
            Document defDoc = definitionDoc;
            deployDefinitions(defDoc, tp);
            deployVariables(processElement.element("variables"), tp);
            deployCorrelationSets(processElement.element("correlationSets"), tp);

            // todo implements 'partnerLinks' elements
            // todo implements 'partners' elements
            // todo implements 'faultHandlers' elements
            // todo implements 'compensationHandlers' elements
            // todo implements 'eventHandlers' elements

            deployActivity(processElement, tp);
            log.debug("</process> ");

            saveProcess(processDoc, definitionDoc, tp.getNamespace()+tp.getName());

            log.debug("commit transaction");
            TransactionManager.commitTransaction();

        } catch (Throwable e) {
            try {
                TransactionManager.rollbackTransaction();
            } catch (TransactionException e1) {
                throw new DeploymentException("Could not rollback transaction.", e);
            }
            throw new DeploymentException(e);
        }
    }

    private void saveProcess(Document processDoc, Document defDoc, String processName) throws DeploymentException {
        try {
            Collection processColl = XMLDataAccess.getCollection("/process");
            Collection processDefColl = XMLDataAccess.getCollection("/process/def");
            if (processColl == null) {
                Collection rootColl = XMLDataAccess.getRootCollection();
                processColl = XMLDataAccess.createCollection(rootColl, "process");
            }
            if (processDefColl == null) {
                processDefColl = XMLDataAccess.createCollection(processColl, "def");
            }
            XMLDataAccess.insertDocument(processColl, "" + processName.hashCode(), processDoc);
            XMLDataAccess.insertDocument(processDefColl, "" + processName.hashCode(), defDoc);
        } catch (Exception e) {
            throw new DeploymentException("Could not save the process description and web services " +
                    "definitions in DB.", e);
        }
    }

    private void deployDefinitions(Document doc, AgilaProcess tp) throws DeploymentException {
        Element rootElement = doc.getRootElement();
        Map addedProperty = new HashMap();
        try {
        	Iterator properties = rootElement.elementIterator("property");
        	while (properties.hasNext()) {
        		Element e = (Element) properties.next();
        		String name = e.valueOf("@name");
        		if (addedProperty.containsKey(name) == false) {
        			Property prop = ProcessFactory.addProperty(tp, name, e.valueOf("@type"));
                    addedProperty.put(name, prop);
        		}
        	}
        	Iterator propertyAlias = rootElement.elementIterator("propertyAlias");
            while (propertyAlias.hasNext()) {
                Element e = (Element) propertyAlias.next();
                String propertyName = e.valueOf("@propertyName");

                XPath xpathSelector = DocumentHelper.createXPath("//*/defaultNS:property[@name=\"" + propertyName + "\"]");
                HashMap nsMap = new HashMap(1);
                nsMap.put("defaultNS", BPEL_NS);
                xpathSelector.setNamespaceURIs(nsMap);
                Node propNode = xpathSelector.selectSingleNode(doc);

                Property prop = (Property) addedProperty.get(propertyName);
                if (propNode != null && prop == null) {
                    prop = ProcessFactory.addProperty(tp,
                            propertyName, propNode.valueOf("@type"));
                    addedProperty.put(propertyName, prop);
                }
                if (prop != null) {
                    ProcessFactory.addPropertyAlias(prop, e.valueOf("@messageType"),
                            e.valueOf("@part"), e.valueOf("@query"));
                } else {
                    log.error("A propertyAlias is defined without property : " + propertyName);
                    throw new DeploymentException("a propertyAlias is defined without property : " + propertyName);
                }
            }
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
    }

    private void deployActivity(Element processElement, AgilaProcess tp) throws DeploymentException, TransactionException {
        String methodName = "deployActivity";
        Element activityElement = ActivityDeployer.getActivityElement(processElement);
        if (activityElement != null) {
            log.debug("<" + activityElement.getName() + ">");
            ActivityDeployer ad = ActivityDeployerFactory.getActivityDeployer(activityElement.getName());
            try {
                ad.deploy(activityElement, tp, new DeployerContext());
                log.debug("</" + activityElement.getName() + ">");
            } catch (DeploymentException e) {
                TransactionManager.rollbackTransaction();
                log.error("Transation Rolled Back due to " + e.getMessage());
                throw new DeploymentException(e);
            }
        }
    }

    /**
     * Deploy the proces element.
     * 
     * @param processElement the process DOM element
     * @return the AgilaProcess corresponding the given process
     * @throws TransactionException 
     * @throws DeploymentException  
     */
    private AgilaProcess deployProcessElement(Element processElement) throws TransactionException, DeploymentException {
        String methodName = "deployProcessElement";
        String name = processElement.valueOf("@name");
        String targetNamespace = processElement.valueOf("@targetNamespace");
//	        String queryLanguage = processElement.valueOf("@queryLanguage");
//	        String expressionLanguage = processElement.valueOf("@expressionLanguage");
//	        String suppressJoinFailure = processElement.valueOf("@suppressJoinFailure");
//	        String enableInstanceCompensation = processElement.valueOf("@enableInstanceCompensation");
//	        String abstractProcess = processElement.valueOf("@abstractProcess");
//	        String xmlns = processElement.valueOf("@xmlns");
        AgilaProcess tp = null;
        try {
            tp = ProcessFactory.createProcess(name, targetNamespace);
        } catch (DBSessionException e) {
            TransactionManager.rollbackTransaction();
            throw new DeploymentException(e);
        } catch (CreationException e) {
            TransactionManager.rollbackTransaction();
            throw new DeploymentException(e);
        }
        return tp;
    }

    /**
     * Deployment of the Variables elements
     * <p/>
     * <variables>?
     * <variable name="ncname" messageType="qname"?
     * type="qname"? element="qname"?/>+
     * </variables>
     * 
     * @param element the Variables DOM element.
     * @param tp      the parent process
     */
    private void deployVariables(Element element, AgilaProcess tp) {
        if (element != null) {
            for (Iterator it = element.elementIterator("variable"); it.hasNext();) {
                log.debug("<variable>");
                Element variable = (Element) it.next();
                String name = variable.valueOf("@name");
                String messageType = variable.valueOf("@messageType");
                String type = variable.valueOf("@type");
                String elmt = variable.valueOf("@element");
                log.debug("name = " + name);
                log.debug("messageType = " + messageType);
                log.debug("type = " + type);
                log.debug("element = " + elmt);
                log.debug("</variable>");
            }
        }
    }

    /**
     * Deployment of the CorrelationSets element
     * <p/>
     * <correlationSets>?
     * <correlationSet name="ncname" properties="qname-list"/>+
     * </correlationSets>
     * 
     * @param element the CorrelationSets DOM element.
     * @param tp      the parent process
     * @throws DeploymentException 
     */
    private void deployCorrelationSets(Element element, AgilaProcess tp) throws DeploymentException {
        if (element != null) {
            for (Iterator it = element.elementIterator("correlationSet"); it.hasNext();) {
                log.debug("<correlationSet>");
                Element variable = (Element) it.next();
                String name = variable.valueOf("@name");
                String properties = variable.valueOf("@properties");
                log.debug("name = " + name);
                log.debug("messageType = " + properties);
                log.debug("</correlationSet>");
                try {
                    ProcessFactory.addCorrelation(tp, name, truncNamespace(properties));
                } catch (Exception e) {
                    throw new DeploymentException(e);
                }
            }
        }
    }

    private String truncNamespace(String nsProps) {
        StringBuffer properties = new StringBuffer();
        for (StringTokenizer nsTokenizer = new StringTokenizer(nsProps); nsTokenizer.hasMoreTokens();) {
            String nsProp = nsTokenizer.nextToken();
            int index = nsProp.lastIndexOf(NS_SEPARATOR);
            properties.append(nsProp.substring(index + 1));
            if (nsTokenizer.hasMoreTokens()) {
                properties.append(SPACE);
            }
        }
        return properties.toString();
    }

    private Document getDocument(String xmlProcessDescription) throws DocumentException {
        if (xmlProcessDescription == null) {
            return null;
        }
        return DocumentHelper.parseText(xmlProcessDescription);
    }

    private Document getDocument(URL xmlProcessDescription) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(xmlProcessDescription);

        return document;
    }

    private WSDLValidator getWSDLValidator() {
        if (wsdlValidator == null) {
            wsdlValidator = new WSDLValidator();
        }
        return wsdlValidator;
    }

    private BPELValidator getBPELValidator() {
        if (wsdlValidator == null) {
            bpelValidator = new BPELValidator();
        }
        return bpelValidator;
    }
}