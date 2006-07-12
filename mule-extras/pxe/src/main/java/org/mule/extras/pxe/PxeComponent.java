/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.pxe;

import com.fs.pxe.bpel.provider.BpelServiceProvider;
import com.fs.pxe.kernel.modbpellog.ModBpelEventLogger;
import com.fs.pxe.kernel.modhibernatedao.ModHibernateDAO;
import com.fs.pxe.kernel.modjdbc.ModJdbcDS;
import com.fs.pxe.kernel.modsfwk.ModSfwk;
import com.fs.pxe.kernel.modsfwk.ModSvcProvider;
import com.fs.pxe.sfwk.deployment.SystemDeploymentBundle;
import com.fs.pxe.sfwk.deployment.som.SystemDescriptor;
import com.fs.pxe.sfwk.mngmt.DomainAdminMBean;
import com.fs.pxe.sfwk.mngmt.SystemAdminMBean;
import com.fs.pxe.sfwk.spi.Message;
import com.fs.pxe.sfwk.spi.MessageExchangeEvent;
import com.fs.pxe.sfwk.spi.MessageExchangeException;
import com.fs.pxe.sfwk.spi.PxeException;
import com.fs.pxe.sfwk.spi.ServiceContext;
import com.fs.pxe.sfwk.spi.ServicePort;
import com.fs.pxe.sfwk.spi.ServiceProviderException;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.i18n.Messages;
import org.mule.extras.pxe.transformers.DirectoryToSystemDeploymentBundle;
import org.mule.extras.pxe.transformers.JarToSystemDeploymentBundle;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleMessage;
import org.mule.impl.UMODescriptorAware;
import org.mule.impl.message.ExceptionPayload;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.concurrent.Latch;
import org.w3c.dom.Document;

/**
 * <code>PxeComponent</code> embeds the PXE runtime engineas a a Mule component so that
 * BPEL processes can take part in Mule applications
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PxeComponent implements Callable, Initialisable, Lifecycle, UMODescriptorAware {
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private MBeanServer server;
    private List names = new ArrayList();
    private Map services = new HashMap();
    private Map dbAttributes = new HashMap();
    private Map daoAttributes = new HashMap();
    private Map bpelAttributes = new HashMap();
    private Latch lock;
    private MuleMessage result;
    private String configuration;
    private String pxeEndpoint;
    private PxeEndpoint endpoint;
    private String defaultOperation;
    private QName operation;
    private MuleDescriptor descriptor;

    /**
     * Singleton instance
     */
    private static PxeComponent instance;

    public static PxeComponent getInstance() {
        return PxeComponent.instance;
    }

    public PxeComponent() {
        PxeComponent.instance = this;
        dbAttributes.put("DataSourceName", "pxe-ds");
        dbAttributes.put("TransactionManagerName", "PxeTransactionManager");
        dbAttributes.put("Driver", "org.hsqldb.jdbcDriver");
        dbAttributes.put("Username", "sa");
        dbAttributes.put("Password", "");
        dbAttributes.put("Url", "jdbc:hsqldb:" + new File(MuleManager.getConfiguration().getWorkingDirectory()) + "/hsqldb/pxeDb");

        daoAttributes.put("BpelStateStoreConnectionFactory", "bpel_sscf");
        daoAttributes.put("StateStoreConnectionFactory", "sscf");
        daoAttributes.put("TransactionManager", "PxeTransactionManager");
        daoAttributes.put("DataSource", dbAttributes.get("DataSourceName"));

        bpelAttributes.put("JndiName", "BpelSP");
        bpelAttributes.put("TransactionManagerName", "PxeTransactionManager");
        bpelAttributes.put("ProviderClass", BpelServiceProvider.class.getName());
        bpelAttributes.put("ProviderURI", "uri:bpelProvider");
        bpelAttributes.put("ProviderProperties", "stateStoreConnectionFactory=bpel_sscf");

        if (logger.isDebugEnabled()) {
            logConfig();
        }
    }

    public void initialise() throws InitialisationException, RecoverableException {

        if (server == null) {
            List l = MBeanServerFactory.findMBeanServer(null);
            if (l != null && l.size() > 0) {
                server = (MBeanServer) l.get(0);
            } else {
                logger.info("No Mbean server found.  Creating new one with domain: Mule_" + descriptor.getName());
                server = MBeanServerFactory.createMBeanServer("Mule_" + descriptor.getName());
            }
        }

        try {
            endpoint = new PxeEndpoint(pxeEndpoint);
            operation = parseQName(defaultOperation);

            if (configuration == null) {
                throw new InitialisationException(new org.mule.config.i18n.Message(Messages.X_IS_NULL, "configuration param"), this);
            }

            if (logger.isDebugEnabled()) {
                logConfig();
            }
            ObjectName modBpelEventLogger = new ObjectName("fivesight.pxe:mod=BpelLogger");
            this.server.createMBean(ModBpelEventLogger.class.getName(), modBpelEventLogger);

            ObjectName modJdbcDS = new ObjectName("fivesight.pxe:mod=PxeDB");
            this.server.createMBean(ModJdbcDS.class.getName(), modJdbcDS);

            this.server.setAttribute(modJdbcDS, new Attribute("DataSourceName", dbAttributes.get("DataSourceName")));
            this.server.setAttribute(modJdbcDS, new Attribute("TransactionManagerName", dbAttributes.get("TransactionManagerName")));
            this.server.setAttribute(modJdbcDS, new Attribute("Driver", dbAttributes.get("Driver")));
            this.server.setAttribute(modJdbcDS, new Attribute("Username", dbAttributes.get("Username")));
            this.server.setAttribute(modJdbcDS, new Attribute("Password", dbAttributes.get("Password")));
            this.server.setAttribute(modJdbcDS, new Attribute("Url", dbAttributes.get("Url")));

            this.server.setAttribute(modJdbcDS, new Attribute("PoolMax", new Integer(descriptor.getPoolingProfile().getMaxActive())));
            this.server.setAttribute(modJdbcDS, new Attribute("PoolMin", new Integer(descriptor.getPoolingProfile().getMaxIdle())));

            ObjectName modDAO = new ObjectName("fivesight.pxe:mod=HibernateDAO");
            this.server.createMBean(ModHibernateDAO.class.getName(), modDAO);
            this.server.setAttribute(modDAO, new Attribute("BpelStateStoreConnectionFactory", daoAttributes.get("BpelStateStoreConnectionFactory")));
            this.server.setAttribute(modDAO, new Attribute("StateStoreConnectionFactory", daoAttributes.get("StateStoreConnectionFactory")));
            this.server.setAttribute(modDAO, new Attribute("TransactionManager", daoAttributes.get("TransactionManager")));
            this.server.setAttribute(modDAO, new Attribute("DataSource", daoAttributes.get("DataSource")));

            URL hibernateProps = ClassUtils.getResource("hibernate.properties", getClass());
            String temp = "Not Found";
            if (hibernateProps != null) {
                temp = hibernateProps.toString();
            }
            this.server.setAttribute(modDAO, new Attribute("HibernateProperties", temp));

            ObjectName modSPBpel = new ObjectName("fivesight.pxe:mod=ServiceProvider,name=Bpel");
            this.server.createMBean(ModSvcProvider.class.getName(), modSPBpel);
            this.server.setAttribute(modSPBpel, new Attribute("JndiName", bpelAttributes.get("JndiName")));
            this.server.setAttribute(modSPBpel, new Attribute("TransactionManagerName", bpelAttributes.get("TransactionManagerName")));
            this.server.setAttribute(modSPBpel, new Attribute("ProviderClass", bpelAttributes.get("ProviderClass")));
            this.server.setAttribute(modSPBpel, new Attribute("ProviderURI", bpelAttributes.get("ProviderURI")));
            this.server.setAttribute(modSPBpel, new Attribute("ProviderProperties", bpelAttributes.get("ProviderProperties")));

            ObjectName modSPMule = new ObjectName("fivesight.pxe:mod=ServiceProvider,name=Mule");
            this.server.createMBean(ModSvcProvider.class.getName(), modSPMule);
            this.server.setAttribute(modSPMule, new Attribute("JndiName", "MuleSP"));
            this.server.setAttribute(modSPMule, new Attribute("TransactionManagerName", "PxeTransactionManager"));
            this.server.setAttribute(modSPMule, new Attribute("ProviderClass", MuleProtocolAdapter.class.getName()));
            this.server.setAttribute(modSPMule, new Attribute("ProviderURI", "uri:protocoladapter.mule"));

            ObjectName modSfwk = new ObjectName("fivesight.pxe:mod=PXE");
            this.server.createMBean(ModSfwk.class.getName(), modSfwk);
            this.server.setAttribute(modSfwk, new Attribute("DomainId", "mule"));
            this.server.setAttribute(modSfwk, new Attribute("TransactionManager", "PxeTransactionManager"));
            this.server.setAttribute(modSfwk, new Attribute("DAOConnectionFactory", "sscf"));
            this.server.setAttribute(modSfwk, new Attribute("SvcProviderJndiBindings", "BpelSP,MuleSP"));

            names.add(modBpelEventLogger);
            names.add(modJdbcDS);
            names.add(modDAO);
            names.add(modSPBpel);
            names.add(modSPMule);
            names.add(modSfwk);

            try {
                InitialContext ctx = new InitialContext();
                TransactionManager txManager = MuleManager.getInstance().getTransactionManager();
                if (txManager == null) {
                    throw new InitialisationException(new org.mule.config.i18n.Message(Messages.TX_MANAGER_NOT_SET), this);
                }
                ctx.rebind("PxeTransactionManager", txManager);
                ctx.close();
            } catch (NamingException e) {
                throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X, descriptor.getName()), e, this);
            }

        } catch (Exception e) {
            throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X, descriptor.getName()), e, this);
        }
    }

    private QName parseQName(String name) {
        if(name==null) {
            return null;
        }
        int i = name.indexOf(":");
        QName qname = null;
        if (i > -1) {
            qname = new QName("uri:" + defaultOperation.substring(0, i),
                    defaultOperation.substring(i + 1));
        } else {
            qname = new QName(defaultOperation);
        }
        return qname;
    }

    public void start() throws UMOException {
        for (Iterator iterator = names.iterator(); iterator.hasNext();) {
            ObjectName name = (ObjectName) iterator.next();
            try {
                this.server.invoke(name, "start", new Object[0], new String[0]);
            } catch (Exception e) {
                throw new LifecycleException(e, this);
            }
        }

        UMOTransformer trans = null;
        if (configuration.endsWith(".jar")) {
            trans = new JarToSystemDeploymentBundle();
        } else {
            trans = new DirectoryToSystemDeploymentBundle();
        }
        SystemDeploymentBundle sdb = (SystemDeploymentBundle) trans.transform(configuration);
        try {
            handleDeploymentBundle(sdb);
        } catch (Exception e) {
            throw new LifecycleException(new org.mule.config.i18n.Message(Messages.FAILED_TO_START_X, descriptor.getName()), e, this);
        }
    }

    public void stop() throws UMOException {
        for (Iterator iterator = names.iterator(); iterator.hasNext();) {
            ObjectName name = (ObjectName) iterator.next();
            try {
                this.server.invoke(name, "stop", new Object[0], new String[0]);
            } catch (Exception e) {
                logger.error(e);
            }
        }
    }

    public void dispose() {
        for (Iterator iterator = names.iterator(); iterator.hasNext();) {
            ObjectName name = (ObjectName) iterator.next();
            try {
                this.server.unregisterMBean(name);
            } catch (Exception e) {
                logger.error(e);
            }
        }
        names.clear();
    }

    public void setDescriptor(UMODescriptor descriptor) {
        this.descriptor = (MuleDescriptor) descriptor;
    }

    public MBeanServer getServer() {
        return server;
    }

    public void setServer(MBeanServer server) {
        this.server = server;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public String getPxeEndpoint() {
        return pxeEndpoint;
    }

    public void setPxeEndpoint(String pxeEndpoint) {
        this.pxeEndpoint = pxeEndpoint;
    }

    public String getDefaultOperation() {
        return defaultOperation;
    }

    public void setDefaultOperation(String defaultOperation) {
        this.defaultOperation = defaultOperation;
    }

    public Map getDbAttributes() {
        return dbAttributes;
    }

    public void setDbAttributes(Map dbAttributes) {
        this.dbAttributes.putAll(dbAttributes);
    }

    public Map getDaoAttributes() {
        return daoAttributes;
    }

    public void setDaoAttributes(Map daoAttributes) {
        this.daoAttributes.putAll(daoAttributes);
    }

    public Map getBpelAttributes() {
        return bpelAttributes;
    }

    public void setBpelAttributes(Map bpelAttributes) {
        this.bpelAttributes.putAll(bpelAttributes);
    }

    private Object resolveJmx(ObjectName oname, Class mbeanClass) {
        return MBeanServerInvocationHandler.newProxyInstance(this.server,
                oname, mbeanClass, true);
    }

    void activateService(ServiceContext service) throws ServiceProviderException {
        try {
                ServicePort[] ports = service.getImports();
                if (ports != null && ports.length > 0) {
                    Definition def = service.getSystemWSDL();
                    QName serviceName = new QName(def.getTargetNamespace(), service.getServiceName());
                    logger.info("Activating service: " + serviceName);
                    this.services.put(service.getServiceUUID(), service);
                }
            } catch (PxeException e) {
                throw new ServiceProviderException(e);
            }

    }

    void deactivateService(ServiceContext service) {
        String id = service.getServiceUUID();
        this.services.remove(id);
    }

    public Object onCall(UMOEventContext eventContext) throws Exception {
        TransactionManager mgr = MuleManager.getInstance().getTransactionManager();
        Transaction tx = mgr.getTransaction();
        com.fs.pxe.sfwk.spi.MessageExchange mePxe = null;
        try {
            if (tx == null) {
                mgr.begin();
            }
            DOMResult r = new DOMResult();
            Source source = (Source) eventContext.getTransformedMessage(Source.class);
            TransformerFactory.newInstance().newTransformer().transform(source, r);

            String svcName = endpoint.getServiceName().getLocalPart();
            String port = endpoint.getPortName();
            QName operation = null;
            String op = eventContext.getMessage().getStringProperty("bpel.operation", null);
            if (op != null) {
                operation = parseQName(op);
            }
            else {
                operation = this.operation;
            }
            if (operation == null) {
                throw new NullPointerException("No operation has been set for this event");
            }
            for (Iterator it = this.services.values().iterator(); it.hasNext();) {
                ServiceContext service = (ServiceContext) it.next();
                if (svcName.equals(service.getServiceName())) {
                    ServicePort svcPort = service.getImport(port);
                    if (svcPort != null) {
                        mePxe = service.createMessageExchange(svcPort, null, operation.getLocalPart());
                        com.fs.pxe.sfwk.spi.Message im = mePxe.createInputMessage();
                        im.setMessage(((Document) r.getNode()).getDocumentElement());
                        mePxe.input(im);
                        break;
                    }
                }
            }
        } finally {
            if (tx == null) {
                mgr.commit();
            }
            if(eventContext.isSynchronous()) {
                lock = new Latch();
                if(eventContext.getTimeout() == UMOEvent.TIMEOUT_WAIT_FOREVER) {
                    lock.await();
                } else {
                    lock.await(eventContext.getTimeout(), TimeUnit.MILLISECONDS);
                    if(result == null) {
                        logger.info("Synchronization either timed out or no result was returned");
                    }
                }
            }
            return result;
        }
    }


    /**
     * Process message exchange from pxe
     */

    public void onMessageExchange(MessageExchangeEvent event) throws MessageExchangeException {
        try {
            if (event.getEventType() == MessageExchangeEvent.IN_RCVD_EVENT) {
                com.fs.pxe.sfwk.spi.MessageExchange me = event.getMessageExchange();
                Message input = me.lastInput();
                result = new MuleMessage(input.getMessage());

            } else if (event.getEventType() == MessageExchangeEvent.OUT_RCVD_EVENT) {
                com.fs.pxe.sfwk.spi.MessageExchange me = event.getMessageExchange();
                Message output = me.lastOutput();
                result = new MuleMessage(output.getMessage());
            } else if (event.getEventType() == MessageExchangeEvent.IN_FAULT_EVENT ||
                    event.getEventType() == MessageExchangeEvent.OUT_FAULT_EVENT) {
                com.fs.pxe.sfwk.spi.MessageExchange me = event.getMessageExchange();
                //todo what needs to be passed in here??
                Message fault = me.lastFault(null);
                result = new MuleMessage(fault.getMessage());
                result.setExceptionPayload(new ExceptionPayload(new Exception("Failed to process PXE Bpel event. See Message payload for details: " + fault.getDescription())));

            }
            lock.countDown();

        } catch (Exception e) {
            throw new MessageExchangeException(e);
        }
    }

    protected void handleDeploymentBundle(SystemDeploymentBundle sdb) throws Exception {
        ObjectName adminName = (ObjectName) this.server.getAttribute(new ObjectName("fivesight.pxe:mod=PXE"), "DomainAdminMBean");

        DomainAdminMBean _domainAdminMBean = (DomainAdminMBean) resolveJmx(adminName, DomainAdminMBean.class);
        SystemDescriptor desc = sdb.getDescriptor();
        ObjectName systemName = _domainAdminMBean.getSystem(desc.getName());
        SystemAdminMBean system = (SystemAdminMBean) resolveJmx(systemName, SystemAdminMBean.class);
        if (systemName != null && system != null) {
            logger.info("System '" + desc.getName() + "' already exist.  Undeploying...");
            system.undeploy();
        }

        systemName = _domainAdminMBean.deploySystemBundle(sdb);
        system = (SystemAdminMBean) resolveJmx(systemName, SystemAdminMBean.class);
        system.enable();

        logger.info("System " + sdb.getDescriptor().getName() + " successfully deployed.");
    }

    protected void logConfig() {
        logger.debug("Default Pxe Database attributes are:");
        logger.debug(PropertiesUtils.propertiesToString(dbAttributes, true));

        logger.debug("Default Pxe DAO attributes are:");
        logger.debug(PropertiesUtils.propertiesToString(daoAttributes, true));

        logger.debug("Default Pxe Bpel attributes are:");
        logger.debug(PropertiesUtils.propertiesToString(bpelAttributes, true));
    }
}
