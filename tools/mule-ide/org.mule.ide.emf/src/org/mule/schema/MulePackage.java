/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see org.mule.schema.MuleFactory
 * @model kind="package"
 * @generated
 */
public interface MulePackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "schema";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "platform:/resource/org.mule.ide.emf/xml/mule.xsd";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "Mule";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	MulePackage eINSTANCE = org.mule.schema.impl.MulePackageImpl.init();

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.AgentsTypeImpl <em>Agents Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.AgentsTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getAgentsType()
	 * @generated
	 */
	int AGENTS_TYPE = 0;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENTS_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Agent</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENTS_TYPE__AGENT = 1;

	/**
	 * The number of structural features of the the '<em>Agents Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENTS_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.AgentTypeImpl <em>Agent Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.AgentTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getAgentType()
	 * @generated
	 */
	int AGENT_TYPE = 1;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_TYPE__CLASS_NAME = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_TYPE__NAME = 3;

	/**
	 * The feature id for the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_TYPE__REF = 4;

	/**
	 * The number of structural features of the the '<em>Agent Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_TYPE_FEATURE_COUNT = 5;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.CatchAllStrategyTypeImpl <em>Catch All Strategy Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.CatchAllStrategyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getCatchAllStrategyType()
	 * @generated
	 */
	int CATCH_ALL_STRATEGY_TYPE = 2;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CATCH_ALL_STRATEGY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Endpoint</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CATCH_ALL_STRATEGY_TYPE__ENDPOINT = 1;

	/**
	 * The feature id for the '<em><b>Global Endpoint</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CATCH_ALL_STRATEGY_TYPE__GLOBAL_ENDPOINT = 2;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CATCH_ALL_STRATEGY_TYPE__PROPERTIES = 3;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CATCH_ALL_STRATEGY_TYPE__CLASS_NAME = 4;

	/**
	 * The number of structural features of the the '<em>Catch All Strategy Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CATCH_ALL_STRATEGY_TYPE_FEATURE_COUNT = 5;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ComponentFactoryTypeImpl <em>Component Factory Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ComponentFactoryTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getComponentFactoryType()
	 * @generated
	 */
	int COMPONENT_FACTORY_TYPE = 3;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_FACTORY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_FACTORY_TYPE__CLASS_NAME = 1;

	/**
	 * The number of structural features of the the '<em>Component Factory Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_FACTORY_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ComponentLifecycleAdapterFactoryTypeImpl <em>Component Lifecycle Adapter Factory Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ComponentLifecycleAdapterFactoryTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getComponentLifecycleAdapterFactoryType()
	 * @generated
	 */
	int COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE = 4;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE__CLASS_NAME = 1;

	/**
	 * The number of structural features of the the '<em>Component Lifecycle Adapter Factory Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ComponentPoolFactoryTypeImpl <em>Component Pool Factory Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ComponentPoolFactoryTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getComponentPoolFactoryType()
	 * @generated
	 */
	int COMPONENT_POOL_FACTORY_TYPE = 5;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_POOL_FACTORY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_POOL_FACTORY_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_POOL_FACTORY_TYPE__CLASS_NAME = 2;

	/**
	 * The number of structural features of the the '<em>Component Pool Factory Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPONENT_POOL_FACTORY_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ConnectionStrategyTypeImpl <em>Connection Strategy Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ConnectionStrategyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getConnectionStrategyType()
	 * @generated
	 */
	int CONNECTION_STRATEGY_TYPE = 6;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTION_STRATEGY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTION_STRATEGY_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTION_STRATEGY_TYPE__CLASS_NAME = 2;

	/**
	 * The number of structural features of the the '<em>Connection Strategy Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTION_STRATEGY_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ConnectorTypeImpl <em>Connector Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ConnectorTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getConnectorType()
	 * @generated
	 */
	int CONNECTOR_TYPE = 7;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTOR_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTOR_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Threading Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTOR_TYPE__THREADING_PROFILE = 2;

	/**
	 * The feature id for the '<em><b>Exception Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTOR_TYPE__EXCEPTION_STRATEGY = 3;

	/**
	 * The feature id for the '<em><b>Connection Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTOR_TYPE__CONNECTION_STRATEGY = 4;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTOR_TYPE__CLASS_NAME = 5;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTOR_TYPE__NAME = 6;

	/**
	 * The feature id for the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTOR_TYPE__REF = 7;

	/**
	 * The number of structural features of the the '<em>Connector Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONNECTOR_TYPE_FEATURE_COUNT = 8;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ConstraintTypeImpl <em>Constraint Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ConstraintTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getConstraintType()
	 * @generated
	 */
	int CONSTRAINT_TYPE = 8;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Left Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__LEFT_FILTER = 1;

	/**
	 * The feature id for the '<em><b>Right Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__RIGHT_FILTER = 2;

	/**
	 * The feature id for the '<em><b>Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__FILTER = 3;

	/**
	 * The feature id for the '<em><b>Batch Size</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__BATCH_SIZE = 4;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__CLASS_NAME = 5;

	/**
	 * The feature id for the '<em><b>Expected Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__EXPECTED_TYPE = 6;

	/**
	 * The feature id for the '<em><b>Expression</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__EXPRESSION = 7;

	/**
	 * The feature id for the '<em><b>Frequency</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__FREQUENCY = 8;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__PATH = 9;

	/**
	 * The feature id for the '<em><b>Pattern</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE__PATTERN = 10;

	/**
	 * The number of structural features of the the '<em>Constraint Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONSTRAINT_TYPE_FEATURE_COUNT = 11;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ContainerContextTypeImpl <em>Container Context Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ContainerContextTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getContainerContextType()
	 * @generated
	 */
	int CONTAINER_CONTEXT_TYPE = 9;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_CONTEXT_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_CONTEXT_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_CONTEXT_TYPE__CLASS_NAME = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_CONTEXT_TYPE__NAME = 3;

	/**
	 * The number of structural features of the the '<em>Container Context Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_CONTEXT_TYPE_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ContainerEntryTypeImpl <em>Container Entry Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ContainerEntryTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getContainerEntryType()
	 * @generated
	 */
	int CONTAINER_ENTRY_TYPE = 10;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_ENTRY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Reference</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_ENTRY_TYPE__REFERENCE = 1;

	/**
	 * The feature id for the '<em><b>Required</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_ENTRY_TYPE__REQUIRED = 2;

	/**
	 * The number of structural features of the the '<em>Container Entry Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_ENTRY_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ContainerPropertyTypeImpl <em>Container Property Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ContainerPropertyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getContainerPropertyType()
	 * @generated
	 */
	int CONTAINER_PROPERTY_TYPE = 11;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_PROPERTY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Container</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_PROPERTY_TYPE__CONTAINER = 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_PROPERTY_TYPE__NAME = 2;

	/**
	 * The feature id for the '<em><b>Reference</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_PROPERTY_TYPE__REFERENCE = 3;

	/**
	 * The feature id for the '<em><b>Required</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_PROPERTY_TYPE__REQUIRED = 4;

	/**
	 * The number of structural features of the the '<em>Container Property Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int CONTAINER_PROPERTY_TYPE_FEATURE_COUNT = 5;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.DocumentRootImpl <em>Document Root</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.DocumentRootImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getDocumentRoot()
	 * @generated
	 */
	int DOCUMENT_ROOT = 12;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__MIXED = 0;

	/**
	 * The feature id for the '<em><b>XMLNS Prefix Map</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__XMLNS_PREFIX_MAP = 1;

	/**
	 * The feature id for the '<em><b>XSI Schema Location</b></em>' map.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__XSI_SCHEMA_LOCATION = 2;

	/**
	 * The feature id for the '<em><b>Mule Configuration</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT__MULE_CONFIGURATION = 3;

	/**
	 * The number of structural features of the the '<em>Document Root</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DOCUMENT_ROOT_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.EncryptionStrategyTypeImpl <em>Encryption Strategy Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.EncryptionStrategyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getEncryptionStrategyType()
	 * @generated
	 */
	int ENCRYPTION_STRATEGY_TYPE = 13;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENCRYPTION_STRATEGY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENCRYPTION_STRATEGY_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENCRYPTION_STRATEGY_TYPE__CLASS_NAME = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENCRYPTION_STRATEGY_TYPE__NAME = 3;

	/**
	 * The number of structural features of the the '<em>Encryption Strategy Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENCRYPTION_STRATEGY_TYPE_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.EndpointIdentifiersTypeImpl <em>Endpoint Identifiers Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.EndpointIdentifiersTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getEndpointIdentifiersType()
	 * @generated
	 */
	int ENDPOINT_IDENTIFIERS_TYPE = 14;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_IDENTIFIERS_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Endpoint Identifier</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_IDENTIFIERS_TYPE__ENDPOINT_IDENTIFIER = 1;

	/**
	 * The number of structural features of the the '<em>Endpoint Identifiers Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_IDENTIFIERS_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.EndpointIdentifierTypeImpl <em>Endpoint Identifier Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.EndpointIdentifierTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getEndpointIdentifierType()
	 * @generated
	 */
	int ENDPOINT_IDENTIFIER_TYPE = 15;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_IDENTIFIER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_IDENTIFIER_TYPE__NAME = 1;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_IDENTIFIER_TYPE__VALUE = 2;

	/**
	 * The number of structural features of the the '<em>Endpoint Identifier Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_IDENTIFIER_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.EndpointTypeImpl <em>Endpoint Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.EndpointTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getEndpointType()
	 * @generated
	 */
	int ENDPOINT_TYPE = 16;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Transaction</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__TRANSACTION = 1;

	/**
	 * The feature id for the '<em><b>Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__FILTER = 2;

	/**
	 * The feature id for the '<em><b>Security Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__SECURITY_FILTER = 3;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__PROPERTIES = 4;

	/**
	 * The feature id for the '<em><b>Address</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__ADDRESS = 5;

	/**
	 * The feature id for the '<em><b>Connector</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__CONNECTOR = 6;

	/**
	 * The feature id for the '<em><b>Create Connector</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__CREATE_CONNECTOR = 7;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__NAME = 8;

	/**
	 * The feature id for the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__REF = 9;

	/**
	 * The feature id for the '<em><b>Remote Sync</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__REMOTE_SYNC = 10;

	/**
	 * The feature id for the '<em><b>Remote Sync Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT = 11;

	/**
	 * The feature id for the '<em><b>Response Transformers</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__RESPONSE_TRANSFORMERS = 12;

	/**
	 * The feature id for the '<em><b>Synchronous</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__SYNCHRONOUS = 13;

	/**
	 * The feature id for the '<em><b>Transformers</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__TRANSFORMERS = 14;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE__TYPE = 15;

	/**
	 * The number of structural features of the the '<em>Endpoint Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENDPOINT_TYPE_FEATURE_COUNT = 16;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.EntryPointResolverTypeImpl <em>Entry Point Resolver Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.EntryPointResolverTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getEntryPointResolverType()
	 * @generated
	 */
	int ENTRY_POINT_RESOLVER_TYPE = 17;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENTRY_POINT_RESOLVER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENTRY_POINT_RESOLVER_TYPE__CLASS_NAME = 1;

	/**
	 * The number of structural features of the the '<em>Entry Point Resolver Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENTRY_POINT_RESOLVER_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.EntryTypeImpl <em>Entry Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.EntryTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getEntryType()
	 * @generated
	 */
	int ENTRY_TYPE = 18;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENTRY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENTRY_TYPE__VALUE = 1;

	/**
	 * The number of structural features of the the '<em>Entry Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENTRY_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.EnvironmentPropertiesTypeImpl <em>Environment Properties Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.EnvironmentPropertiesTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getEnvironmentPropertiesType()
	 * @generated
	 */
	int ENVIRONMENT_PROPERTIES_TYPE = 19;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENVIRONMENT_PROPERTIES_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Group</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENVIRONMENT_PROPERTIES_TYPE__GROUP = 1;

	/**
	 * The feature id for the '<em><b>Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENVIRONMENT_PROPERTIES_TYPE__PROPERTY = 2;

	/**
	 * The feature id for the '<em><b>Factory Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENVIRONMENT_PROPERTIES_TYPE__FACTORY_PROPERTY = 3;

	/**
	 * The feature id for the '<em><b>System Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENVIRONMENT_PROPERTIES_TYPE__SYSTEM_PROPERTY = 4;

	/**
	 * The feature id for the '<em><b>Map</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENVIRONMENT_PROPERTIES_TYPE__MAP = 5;

	/**
	 * The feature id for the '<em><b>List</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENVIRONMENT_PROPERTIES_TYPE__LIST = 6;

	/**
	 * The feature id for the '<em><b>File Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENVIRONMENT_PROPERTIES_TYPE__FILE_PROPERTIES = 7;

	/**
	 * The number of structural features of the the '<em>Environment Properties Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ENVIRONMENT_PROPERTIES_TYPE_FEATURE_COUNT = 8;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ExceptionStrategyTypeImpl <em>Exception Strategy Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ExceptionStrategyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getExceptionStrategyType()
	 * @generated
	 */
	int EXCEPTION_STRATEGY_TYPE = 20;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXCEPTION_STRATEGY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Endpoint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXCEPTION_STRATEGY_TYPE__ENDPOINT = 1;

	/**
	 * The feature id for the '<em><b>Global Endpoint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXCEPTION_STRATEGY_TYPE__GLOBAL_ENDPOINT = 2;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXCEPTION_STRATEGY_TYPE__PROPERTIES = 3;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXCEPTION_STRATEGY_TYPE__CLASS_NAME = 4;

	/**
	 * The number of structural features of the the '<em>Exception Strategy Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EXCEPTION_STRATEGY_TYPE_FEATURE_COUNT = 5;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.FactoryEntryTypeImpl <em>Factory Entry Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.FactoryEntryTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getFactoryEntryType()
	 * @generated
	 */
	int FACTORY_ENTRY_TYPE = 21;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FACTORY_ENTRY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Factory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FACTORY_ENTRY_TYPE__FACTORY = 1;

	/**
	 * The number of structural features of the the '<em>Factory Entry Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FACTORY_ENTRY_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.FactoryPropertyTypeImpl <em>Factory Property Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.FactoryPropertyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getFactoryPropertyType()
	 * @generated
	 */
	int FACTORY_PROPERTY_TYPE = 22;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FACTORY_PROPERTY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Factory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FACTORY_PROPERTY_TYPE__FACTORY = 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FACTORY_PROPERTY_TYPE__NAME = 2;

	/**
	 * The number of structural features of the the '<em>Factory Property Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FACTORY_PROPERTY_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.FilePropertiesTypeImpl <em>File Properties Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.FilePropertiesTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getFilePropertiesType()
	 * @generated
	 */
	int FILE_PROPERTIES_TYPE = 23;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILE_PROPERTIES_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Location</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILE_PROPERTIES_TYPE__LOCATION = 1;

	/**
	 * The feature id for the '<em><b>Override</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILE_PROPERTIES_TYPE__OVERRIDE = 2;

	/**
	 * The number of structural features of the the '<em>File Properties Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILE_PROPERTIES_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.FilterTypeImpl <em>Filter Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.FilterTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getFilterType()
	 * @generated
	 */
	int FILTER_TYPE = 24;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__FILTER = 2;

	/**
	 * The feature id for the '<em><b>Left Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__LEFT_FILTER = 3;

	/**
	 * The feature id for the '<em><b>Right Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__RIGHT_FILTER = 4;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__CLASS_NAME = 5;

	/**
	 * The feature id for the '<em><b>Config File</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__CONFIG_FILE = 6;

	/**
	 * The feature id for the '<em><b>Expected Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__EXPECTED_TYPE = 7;

	/**
	 * The feature id for the '<em><b>Expression</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__EXPRESSION = 8;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__PATH = 9;

	/**
	 * The feature id for the '<em><b>Pattern</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE__PATTERN = 10;

	/**
	 * The number of structural features of the the '<em>Filter Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILTER_TYPE_FEATURE_COUNT = 11;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.GlobalEndpointsTypeImpl <em>Global Endpoints Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.GlobalEndpointsTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getGlobalEndpointsType()
	 * @generated
	 */
	int GLOBAL_ENDPOINTS_TYPE = 25;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINTS_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Endpoint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINTS_TYPE__ENDPOINT = 1;

	/**
	 * The number of structural features of the the '<em>Global Endpoints Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINTS_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.GlobalEndpointTypeImpl <em>Global Endpoint Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.GlobalEndpointTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getGlobalEndpointType()
	 * @generated
	 */
	int GLOBAL_ENDPOINT_TYPE = 26;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Transaction</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__TRANSACTION = 1;

	/**
	 * The feature id for the '<em><b>Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__FILTER = 2;

	/**
	 * The feature id for the '<em><b>Security Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__SECURITY_FILTER = 3;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__PROPERTIES = 4;

	/**
	 * The feature id for the '<em><b>Address</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__ADDRESS = 5;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__NAME = 6;

	/**
	 * The feature id for the '<em><b>Remote Sync</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC = 7;

	/**
	 * The feature id for the '<em><b>Remote Sync Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT = 8;

	/**
	 * The feature id for the '<em><b>Response Transformers</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__RESPONSE_TRANSFORMERS = 9;

	/**
	 * The feature id for the '<em><b>Synchronous</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__SYNCHRONOUS = 10;

	/**
	 * The feature id for the '<em><b>Transformers</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE__TRANSFORMERS = 11;

	/**
	 * The number of structural features of the the '<em>Global Endpoint Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GLOBAL_ENDPOINT_TYPE_FEATURE_COUNT = 12;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.InboundRouterTypeImpl <em>Inbound Router Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.InboundRouterTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getInboundRouterType()
	 * @generated
	 */
	int INBOUND_ROUTER_TYPE = 27;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INBOUND_ROUTER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Catch All Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INBOUND_ROUTER_TYPE__CATCH_ALL_STRATEGY = 1;

	/**
	 * The feature id for the '<em><b>Endpoint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INBOUND_ROUTER_TYPE__ENDPOINT = 2;

	/**
	 * The feature id for the '<em><b>Global Endpoint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INBOUND_ROUTER_TYPE__GLOBAL_ENDPOINT = 3;

	/**
	 * The feature id for the '<em><b>Router</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INBOUND_ROUTER_TYPE__ROUTER = 4;

	/**
	 * The feature id for the '<em><b>Match All</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INBOUND_ROUTER_TYPE__MATCH_ALL = 5;

	/**
	 * The number of structural features of the the '<em>Inbound Router Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INBOUND_ROUTER_TYPE_FEATURE_COUNT = 6;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.InterceptorStackTypeImpl <em>Interceptor Stack Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.InterceptorStackTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getInterceptorStackType()
	 * @generated
	 */
	int INTERCEPTOR_STACK_TYPE = 28;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERCEPTOR_STACK_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Interceptor</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERCEPTOR_STACK_TYPE__INTERCEPTOR = 1;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERCEPTOR_STACK_TYPE__NAME = 2;

	/**
	 * The number of structural features of the the '<em>Interceptor Stack Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERCEPTOR_STACK_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.InterceptorTypeImpl <em>Interceptor Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.InterceptorTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getInterceptorType()
	 * @generated
	 */
	int INTERCEPTOR_TYPE = 29;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERCEPTOR_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERCEPTOR_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERCEPTOR_TYPE__CLASS_NAME = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERCEPTOR_TYPE__NAME = 3;

	/**
	 * The number of structural features of the the '<em>Interceptor Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int INTERCEPTOR_TYPE_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.LeftFilterTypeImpl <em>Left Filter Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.LeftFilterTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getLeftFilterType()
	 * @generated
	 */
	int LEFT_FILTER_TYPE = 30;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__FILTER = 2;

	/**
	 * The feature id for the '<em><b>Left Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__LEFT_FILTER = 3;

	/**
	 * The feature id for the '<em><b>Right Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__RIGHT_FILTER = 4;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__CLASS_NAME = 5;

	/**
	 * The feature id for the '<em><b>Config File</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__CONFIG_FILE = 6;

	/**
	 * The feature id for the '<em><b>Expected Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__EXPECTED_TYPE = 7;

	/**
	 * The feature id for the '<em><b>Expression</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__EXPRESSION = 8;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__PATH = 9;

	/**
	 * The feature id for the '<em><b>Pattern</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE__PATTERN = 10;

	/**
	 * The number of structural features of the the '<em>Left Filter Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LEFT_FILTER_TYPE_FEATURE_COUNT = 11;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ListTypeImpl <em>List Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ListTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getListType()
	 * @generated
	 */
	int LIST_TYPE = 31;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Group</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_TYPE__GROUP = 1;

	/**
	 * The feature id for the '<em><b>Entry</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_TYPE__ENTRY = 2;

	/**
	 * The feature id for the '<em><b>Factory Entry</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_TYPE__FACTORY_ENTRY = 3;

	/**
	 * The feature id for the '<em><b>System Entry</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_TYPE__SYSTEM_ENTRY = 4;

	/**
	 * The feature id for the '<em><b>Container Entry</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_TYPE__CONTAINER_ENTRY = 5;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_TYPE__NAME = 6;

	/**
	 * The number of structural features of the the '<em>List Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LIST_TYPE_FEATURE_COUNT = 7;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.MapTypeImpl <em>Map Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.MapTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getMapType()
	 * @generated
	 */
	int MAP_TYPE = 32;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Group</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__GROUP = 1;

	/**
	 * The feature id for the '<em><b>Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__PROPERTY = 2;

	/**
	 * The feature id for the '<em><b>Factory Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__FACTORY_PROPERTY = 3;

	/**
	 * The feature id for the '<em><b>Container Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__CONTAINER_PROPERTY = 4;

	/**
	 * The feature id for the '<em><b>System Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__SYSTEM_PROPERTY = 5;

	/**
	 * The feature id for the '<em><b>Map</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__MAP = 6;

	/**
	 * The feature id for the '<em><b>List</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__LIST = 7;

	/**
	 * The feature id for the '<em><b>File Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__FILE_PROPERTIES = 8;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE__NAME = 9;

	/**
	 * The number of structural features of the the '<em>Map Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAP_TYPE_FEATURE_COUNT = 10;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ModelTypeImpl <em>Model Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ModelTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getModelType()
	 * @generated
	 */
	int MODEL_TYPE = 33;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__DESCRIPTION = 1;

	/**
	 * The feature id for the '<em><b>Entry Point Resolver</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__ENTRY_POINT_RESOLVER = 2;

	/**
	 * The feature id for the '<em><b>Component Factory</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__COMPONENT_FACTORY = 3;

	/**
	 * The feature id for the '<em><b>Component Lifecycle Adapter Factory</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__COMPONENT_LIFECYCLE_ADAPTER_FACTORY = 4;

	/**
	 * The feature id for the '<em><b>Component Pool Factory</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__COMPONENT_POOL_FACTORY = 5;

	/**
	 * The feature id for the '<em><b>Exception Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__EXCEPTION_STRATEGY = 6;

	/**
	 * The feature id for the '<em><b>Mule Descriptor</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__MULE_DESCRIPTOR = 7;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__CLASS_NAME = 8;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__NAME = 9;

	/**
	 * The feature id for the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__REF = 10;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE__TYPE = 11;

	/**
	 * The number of structural features of the the '<em>Model Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MODEL_TYPE_FEATURE_COUNT = 12;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.MuleConfigurationTypeImpl <em>Configuration Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.MuleConfigurationTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getMuleConfigurationType()
	 * @generated
	 */
	int MULE_CONFIGURATION_TYPE = 34;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__DESCRIPTION = 1;

	/**
	 * The feature id for the '<em><b>Environment Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__ENVIRONMENT_PROPERTIES = 2;

	/**
	 * The feature id for the '<em><b>Mule Environment Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__MULE_ENVIRONMENT_PROPERTIES = 3;

	/**
	 * The feature id for the '<em><b>Container Context</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__CONTAINER_CONTEXT = 4;

	/**
	 * The feature id for the '<em><b>Security Manager</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__SECURITY_MANAGER = 5;

	/**
	 * The feature id for the '<em><b>Transaction Manager</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__TRANSACTION_MANAGER = 6;

	/**
	 * The feature id for the '<em><b>Agents</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__AGENTS = 7;

	/**
	 * The feature id for the '<em><b>Connector</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__CONNECTOR = 8;

	/**
	 * The feature id for the '<em><b>Endpoint Identifiers</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__ENDPOINT_IDENTIFIERS = 9;

	/**
	 * The feature id for the '<em><b>Transformers</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__TRANSFORMERS = 10;

	/**
	 * The feature id for the '<em><b>Global Endpoints</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__GLOBAL_ENDPOINTS = 11;

	/**
	 * The feature id for the '<em><b>Interceptor Stack</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__INTERCEPTOR_STACK = 12;

	/**
	 * The feature id for the '<em><b>Model</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__MODEL = 13;

	/**
	 * The feature id for the '<em><b>Mule Descriptor</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__MULE_DESCRIPTOR = 14;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__ID = 15;

	/**
	 * The feature id for the '<em><b>Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE__VERSION = 16;

	/**
	 * The number of structural features of the the '<em>Configuration Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_CONFIGURATION_TYPE_FEATURE_COUNT = 17;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.MuleDescriptorTypeImpl <em>Descriptor Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.MuleDescriptorTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getMuleDescriptorType()
	 * @generated
	 */
	int MULE_DESCRIPTOR_TYPE = 35;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Inbound Router</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__INBOUND_ROUTER = 1;

	/**
	 * The feature id for the '<em><b>Outbound Router</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__OUTBOUND_ROUTER = 2;

	/**
	 * The feature id for the '<em><b>Response Router</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__RESPONSE_ROUTER = 3;

	/**
	 * The feature id for the '<em><b>Interceptor</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__INTERCEPTOR = 4;

	/**
	 * The feature id for the '<em><b>Threading Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__THREADING_PROFILE = 5;

	/**
	 * The feature id for the '<em><b>Pooling Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__POOLING_PROFILE = 6;

	/**
	 * The feature id for the '<em><b>Queue Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__QUEUE_PROFILE = 7;

	/**
	 * The feature id for the '<em><b>Exception Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__EXCEPTION_STRATEGY = 8;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__PROPERTIES = 9;

	/**
	 * The feature id for the '<em><b>Container Managed</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__CONTAINER_MANAGED = 10;

	/**
	 * The feature id for the '<em><b>Implementation</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__IMPLEMENTATION = 11;

	/**
	 * The feature id for the '<em><b>Inbound Endpoint</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__INBOUND_ENDPOINT = 12;

	/**
	 * The feature id for the '<em><b>Inbound Transformer</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__INBOUND_TRANSFORMER = 13;

	/**
	 * The feature id for the '<em><b>Initial State</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__INITIAL_STATE = 14;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__NAME = 15;

	/**
	 * The feature id for the '<em><b>Outbound Endpoint</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__OUTBOUND_ENDPOINT = 16;

	/**
	 * The feature id for the '<em><b>Outbound Transformer</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__OUTBOUND_TRANSFORMER = 17;

	/**
	 * The feature id for the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__REF = 18;

	/**
	 * The feature id for the '<em><b>Response Transformer</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__RESPONSE_TRANSFORMER = 19;

	/**
	 * The feature id for the '<em><b>Singleton</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__SINGLETON = 20;

	/**
	 * The feature id for the '<em><b>Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE__VERSION = 21;

	/**
	 * The number of structural features of the the '<em>Descriptor Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_DESCRIPTOR_TYPE_FEATURE_COUNT = 22;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl <em>Environment Properties Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.MuleEnvironmentPropertiesTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE = 36;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Threading Profile</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE = 1;

	/**
	 * The feature id for the '<em><b>Pooling Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE = 2;

	/**
	 * The feature id for the '<em><b>Queue Profile</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE = 3;

	/**
	 * The feature id for the '<em><b>Persistence Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY = 4;

	/**
	 * The feature id for the '<em><b>Connection Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY = 5;

	/**
	 * The feature id for the '<em><b>Client Mode</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE = 6;

	/**
	 * The feature id for the '<em><b>Embedded</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__EMBEDDED = 7;

	/**
	 * The feature id for the '<em><b>Enable Message Events</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__ENABLE_MESSAGE_EVENTS = 8;

	/**
	 * The feature id for the '<em><b>Encoding</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__ENCODING = 9;

	/**
	 * The feature id for the '<em><b>Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL = 10;

	/**
	 * The feature id for the '<em><b>Recoverable Mode</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE = 11;

	/**
	 * The feature id for the '<em><b>Remote Sync</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__REMOTE_SYNC = 12;

	/**
	 * The feature id for the '<em><b>Server Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL = 13;

	/**
	 * The feature id for the '<em><b>Synchronous</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS = 14;

	/**
	 * The feature id for the '<em><b>Synchronous Event Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT = 15;

	/**
	 * The feature id for the '<em><b>Transaction Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT = 16;

	/**
	 * The feature id for the '<em><b>Working Directory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY = 17;

	/**
	 * The number of structural features of the the '<em>Environment Properties Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MULE_ENVIRONMENT_PROPERTIES_TYPE_FEATURE_COUNT = 18;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.OutboundRouterTypeImpl <em>Outbound Router Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.OutboundRouterTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getOutboundRouterType()
	 * @generated
	 */
	int OUTBOUND_ROUTER_TYPE = 37;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OUTBOUND_ROUTER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Catch All Strategy</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OUTBOUND_ROUTER_TYPE__CATCH_ALL_STRATEGY = 1;

	/**
	 * The feature id for the '<em><b>Router</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OUTBOUND_ROUTER_TYPE__ROUTER = 2;

	/**
	 * The feature id for the '<em><b>Match All</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OUTBOUND_ROUTER_TYPE__MATCH_ALL = 3;

	/**
	 * The number of structural features of the the '<em>Outbound Router Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OUTBOUND_ROUTER_TYPE_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.PersistenceStrategyTypeImpl <em>Persistence Strategy Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.PersistenceStrategyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getPersistenceStrategyType()
	 * @generated
	 */
	int PERSISTENCE_STRATEGY_TYPE = 38;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERSISTENCE_STRATEGY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERSISTENCE_STRATEGY_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERSISTENCE_STRATEGY_TYPE__CLASS_NAME = 2;

	/**
	 * The number of structural features of the the '<em>Persistence Strategy Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PERSISTENCE_STRATEGY_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.PoolingProfileTypeImpl <em>Pooling Profile Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.PoolingProfileTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getPoolingProfileType()
	 * @generated
	 */
	int POOLING_PROFILE_TYPE = 39;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POOLING_PROFILE_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Exhausted Action</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POOLING_PROFILE_TYPE__EXHAUSTED_ACTION = 1;

	/**
	 * The feature id for the '<em><b>Factory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POOLING_PROFILE_TYPE__FACTORY = 2;

	/**
	 * The feature id for the '<em><b>Initialisation Policy</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POOLING_PROFILE_TYPE__INITIALISATION_POLICY = 3;

	/**
	 * The feature id for the '<em><b>Max Active</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POOLING_PROFILE_TYPE__MAX_ACTIVE = 4;

	/**
	 * The feature id for the '<em><b>Max Idle</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POOLING_PROFILE_TYPE__MAX_IDLE = 5;

	/**
	 * The feature id for the '<em><b>Max Wait</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POOLING_PROFILE_TYPE__MAX_WAIT = 6;

	/**
	 * The number of structural features of the the '<em>Pooling Profile Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int POOLING_PROFILE_TYPE_FEATURE_COUNT = 7;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.PropertiesTypeImpl <em>Properties Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.PropertiesTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getPropertiesType()
	 * @generated
	 */
	int PROPERTIES_TYPE = 40;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Group</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__GROUP = 1;

	/**
	 * The feature id for the '<em><b>Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__PROPERTY = 2;

	/**
	 * The feature id for the '<em><b>Factory Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__FACTORY_PROPERTY = 3;

	/**
	 * The feature id for the '<em><b>Container Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__CONTAINER_PROPERTY = 4;

	/**
	 * The feature id for the '<em><b>System Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__SYSTEM_PROPERTY = 5;

	/**
	 * The feature id for the '<em><b>Map</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__MAP = 6;

	/**
	 * The feature id for the '<em><b>List</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__LIST = 7;

	/**
	 * The feature id for the '<em><b>File Properties</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__FILE_PROPERTIES = 8;

	/**
	 * The feature id for the '<em><b>Text Property</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE__TEXT_PROPERTY = 9;

	/**
	 * The number of structural features of the the '<em>Properties Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTIES_TYPE_FEATURE_COUNT = 10;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.PropertyTypeImpl <em>Property Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.PropertyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getPropertyType()
	 * @generated
	 */
	int PROPERTY_TYPE = 41;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY_TYPE__NAME = 1;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY_TYPE__VALUE = 2;

	/**
	 * The number of structural features of the the '<em>Property Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int PROPERTY_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.QueueProfileTypeImpl <em>Queue Profile Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.QueueProfileTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getQueueProfileType()
	 * @generated
	 */
	int QUEUE_PROFILE_TYPE = 42;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int QUEUE_PROFILE_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int QUEUE_PROFILE_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Max Outstanding Messages</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int QUEUE_PROFILE_TYPE__MAX_OUTSTANDING_MESSAGES = 2;

	/**
	 * The feature id for the '<em><b>Persistent</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int QUEUE_PROFILE_TYPE__PERSISTENT = 3;

	/**
	 * The number of structural features of the the '<em>Queue Profile Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int QUEUE_PROFILE_TYPE_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ReplyToTypeImpl <em>Reply To Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ReplyToTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getReplyToType()
	 * @generated
	 */
	int REPLY_TO_TYPE = 43;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REPLY_TO_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Address</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REPLY_TO_TYPE__ADDRESS = 1;

	/**
	 * The number of structural features of the the '<em>Reply To Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int REPLY_TO_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ResponseRouterTypeImpl <em>Response Router Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ResponseRouterTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getResponseRouterType()
	 * @generated
	 */
	int RESPONSE_ROUTER_TYPE = 44;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPONSE_ROUTER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Endpoint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPONSE_ROUTER_TYPE__ENDPOINT = 1;

	/**
	 * The feature id for the '<em><b>Global Endpoint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPONSE_ROUTER_TYPE__GLOBAL_ENDPOINT = 2;

	/**
	 * The feature id for the '<em><b>Router</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPONSE_ROUTER_TYPE__ROUTER = 3;

	/**
	 * The feature id for the '<em><b>Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPONSE_ROUTER_TYPE__TIMEOUT = 4;

	/**
	 * The number of structural features of the the '<em>Response Router Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RESPONSE_ROUTER_TYPE_FEATURE_COUNT = 5;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.RightFilterTypeImpl <em>Right Filter Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.RightFilterTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getRightFilterType()
	 * @generated
	 */
	int RIGHT_FILTER_TYPE = 45;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__FILTER = 2;

	/**
	 * The feature id for the '<em><b>Left Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__LEFT_FILTER = 3;

	/**
	 * The feature id for the '<em><b>Right Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__RIGHT_FILTER = 4;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__CLASS_NAME = 5;

	/**
	 * The feature id for the '<em><b>Config File</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__CONFIG_FILE = 6;

	/**
	 * The feature id for the '<em><b>Expected Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__EXPECTED_TYPE = 7;

	/**
	 * The feature id for the '<em><b>Expression</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__EXPRESSION = 8;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__PATH = 9;

	/**
	 * The feature id for the '<em><b>Pattern</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE__PATTERN = 10;

	/**
	 * The number of structural features of the the '<em>Right Filter Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RIGHT_FILTER_TYPE_FEATURE_COUNT = 11;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.RouterTypeImpl <em>Router Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.RouterTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getRouterType()
	 * @generated
	 */
	int ROUTER_TYPE = 46;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Endpoint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__ENDPOINT = 1;

	/**
	 * The feature id for the '<em><b>Global Endpoint</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__GLOBAL_ENDPOINT = 2;

	/**
	 * The feature id for the '<em><b>Reply To</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__REPLY_TO = 3;

	/**
	 * The feature id for the '<em><b>Transaction</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__TRANSACTION = 4;

	/**
	 * The feature id for the '<em><b>Filter</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__FILTER = 5;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__PROPERTIES = 6;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__CLASS_NAME = 7;

	/**
	 * The feature id for the '<em><b>Enable Correlation</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__ENABLE_CORRELATION = 8;

	/**
	 * The feature id for the '<em><b>Property Extractor</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE__PROPERTY_EXTRACTOR = 9;

	/**
	 * The number of structural features of the the '<em>Router Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ROUTER_TYPE_FEATURE_COUNT = 10;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.SecurityFilterTypeImpl <em>Security Filter Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.SecurityFilterTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getSecurityFilterType()
	 * @generated
	 */
	int SECURITY_FILTER_TYPE = 47;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_FILTER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_FILTER_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_FILTER_TYPE__CLASS_NAME = 2;

	/**
	 * The feature id for the '<em><b>Use Providers</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_FILTER_TYPE__USE_PROVIDERS = 3;

	/**
	 * The number of structural features of the the '<em>Security Filter Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_FILTER_TYPE_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.SecurityManagerTypeImpl <em>Security Manager Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.SecurityManagerTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getSecurityManagerType()
	 * @generated
	 */
	int SECURITY_MANAGER_TYPE = 48;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_MANAGER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Security Provider</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_MANAGER_TYPE__SECURITY_PROVIDER = 1;

	/**
	 * The feature id for the '<em><b>Encryption Strategy</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_MANAGER_TYPE__ENCRYPTION_STRATEGY = 2;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_MANAGER_TYPE__CLASS_NAME = 3;

	/**
	 * The feature id for the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_MANAGER_TYPE__REF = 4;

	/**
	 * The number of structural features of the the '<em>Security Manager Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_MANAGER_TYPE_FEATURE_COUNT = 5;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.SecurityProviderTypeImpl <em>Security Provider Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.SecurityProviderTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getSecurityProviderType()
	 * @generated
	 */
	int SECURITY_PROVIDER_TYPE = 49;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_PROVIDER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_PROVIDER_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_PROVIDER_TYPE__CLASS_NAME = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_PROVIDER_TYPE__NAME = 3;

	/**
	 * The feature id for the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_PROVIDER_TYPE__REF = 4;

	/**
	 * The number of structural features of the the '<em>Security Provider Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECURITY_PROVIDER_TYPE_FEATURE_COUNT = 5;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.SystemEntryTypeImpl <em>System Entry Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.SystemEntryTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getSystemEntryType()
	 * @generated
	 */
	int SYSTEM_ENTRY_TYPE = 50;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYSTEM_ENTRY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Default Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYSTEM_ENTRY_TYPE__DEFAULT_VALUE = 1;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYSTEM_ENTRY_TYPE__KEY = 2;

	/**
	 * The number of structural features of the the '<em>System Entry Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYSTEM_ENTRY_TYPE_FEATURE_COUNT = 3;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.SystemPropertyTypeImpl <em>System Property Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.SystemPropertyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getSystemPropertyType()
	 * @generated
	 */
	int SYSTEM_PROPERTY_TYPE = 51;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYSTEM_PROPERTY_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Default Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYSTEM_PROPERTY_TYPE__DEFAULT_VALUE = 1;

	/**
	 * The feature id for the '<em><b>Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYSTEM_PROPERTY_TYPE__KEY = 2;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYSTEM_PROPERTY_TYPE__NAME = 3;

	/**
	 * The number of structural features of the the '<em>System Property Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SYSTEM_PROPERTY_TYPE_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.TextPropertyTypeImpl <em>Text Property Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.TextPropertyTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getTextPropertyType()
	 * @generated
	 */
	int TEXT_PROPERTY_TYPE = 52;

	/**
	 * The feature id for the '<em><b>Value</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEXT_PROPERTY_TYPE__VALUE = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEXT_PROPERTY_TYPE__NAME = 1;

	/**
	 * The number of structural features of the the '<em>Text Property Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEXT_PROPERTY_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.ThreadingProfileTypeImpl <em>Threading Profile Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.ThreadingProfileTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getThreadingProfileType()
	 * @generated
	 */
	int THREADING_PROFILE_TYPE = 53;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Do Threading</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE__DO_THREADING = 1;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE__ID = 2;

	/**
	 * The feature id for the '<em><b>Max Buffer Size</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE__MAX_BUFFER_SIZE = 3;

	/**
	 * The feature id for the '<em><b>Max Threads Active</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE__MAX_THREADS_ACTIVE = 4;

	/**
	 * The feature id for the '<em><b>Max Threads Idle</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE__MAX_THREADS_IDLE = 5;

	/**
	 * The feature id for the '<em><b>Pool Exhausted Action</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE__POOL_EXHAUSTED_ACTION = 6;

	/**
	 * The feature id for the '<em><b>Thread TTL</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE__THREAD_TTL = 7;

	/**
	 * The feature id for the '<em><b>Thread Wait Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE__THREAD_WAIT_TIMEOUT = 8;

	/**
	 * The number of structural features of the the '<em>Threading Profile Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int THREADING_PROFILE_TYPE_FEATURE_COUNT = 9;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.TransactionManagerTypeImpl <em>Transaction Manager Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.TransactionManagerTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getTransactionManagerType()
	 * @generated
	 */
	int TRANSACTION_MANAGER_TYPE = 54;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_MANAGER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_MANAGER_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Factory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_MANAGER_TYPE__FACTORY = 2;

	/**
	 * The feature id for the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_MANAGER_TYPE__REF = 3;

	/**
	 * The number of structural features of the the '<em>Transaction Manager Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_MANAGER_TYPE_FEATURE_COUNT = 4;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.TransactionTypeImpl <em>Transaction Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.TransactionTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getTransactionType()
	 * @generated
	 */
	int TRANSACTION_TYPE = 55;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Constraint</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_TYPE__CONSTRAINT = 1;

	/**
	 * The feature id for the '<em><b>Action</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_TYPE__ACTION = 2;

	/**
	 * The feature id for the '<em><b>Factory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_TYPE__FACTORY = 3;

	/**
	 * The feature id for the '<em><b>Timeout</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_TYPE__TIMEOUT = 4;

	/**
	 * The number of structural features of the the '<em>Transaction Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSACTION_TYPE_FEATURE_COUNT = 5;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.TransformersTypeImpl <em>Transformers Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.TransformersTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getTransformersType()
	 * @generated
	 */
	int TRANSFORMERS_TYPE = 56;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMERS_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Transformer</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMERS_TYPE__TRANSFORMER = 1;

	/**
	 * The number of structural features of the the '<em>Transformers Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMERS_TYPE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link org.mule.schema.impl.TransformerTypeImpl <em>Transformer Type</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.impl.TransformerTypeImpl
	 * @see org.mule.schema.impl.MulePackageImpl#getTransformerType()
	 * @generated
	 */
	int TRANSFORMER_TYPE = 57;

	/**
	 * The feature id for the '<em><b>Mixed</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMER_TYPE__MIXED = 0;

	/**
	 * The feature id for the '<em><b>Properties</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMER_TYPE__PROPERTIES = 1;

	/**
	 * The feature id for the '<em><b>Class Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMER_TYPE__CLASS_NAME = 2;

	/**
	 * The feature id for the '<em><b>Ignore Bad Input</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMER_TYPE__IGNORE_BAD_INPUT = 3;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMER_TYPE__NAME = 4;

	/**
	 * The feature id for the '<em><b>Ref</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMER_TYPE__REF = 5;

	/**
	 * The feature id for the '<em><b>Return Class</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMER_TYPE__RETURN_CLASS = 6;

	/**
	 * The number of structural features of the the '<em>Transformer Type</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TRANSFORMER_TYPE_FEATURE_COUNT = 7;

	/**
	 * The meta object id for the '{@link org.mule.schema.ActionType <em>Action Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.ActionType
	 * @see org.mule.schema.impl.MulePackageImpl#getActionType()
	 * @generated
	 */
	int ACTION_TYPE = 58;

	/**
	 * The meta object id for the '{@link org.mule.schema.CreateConnectorType <em>Create Connector Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.CreateConnectorType
	 * @see org.mule.schema.impl.MulePackageImpl#getCreateConnectorType()
	 * @generated
	 */
	int CREATE_CONNECTOR_TYPE = 59;

	/**
	 * The meta object id for the '{@link org.mule.schema.EnableCorrelationType <em>Enable Correlation Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.EnableCorrelationType
	 * @see org.mule.schema.impl.MulePackageImpl#getEnableCorrelationType()
	 * @generated
	 */
	int ENABLE_CORRELATION_TYPE = 60;

	/**
	 * The meta object id for the '{@link org.mule.schema.ExhaustedActionType <em>Exhausted Action Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.ExhaustedActionType
	 * @see org.mule.schema.impl.MulePackageImpl#getExhaustedActionType()
	 * @generated
	 */
	int EXHAUSTED_ACTION_TYPE = 61;

	/**
	 * The meta object id for the '{@link org.mule.schema.IdType <em>Id Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.IdType
	 * @see org.mule.schema.impl.MulePackageImpl#getIdType()
	 * @generated
	 */
	int ID_TYPE = 62;

	/**
	 * The meta object id for the '{@link org.mule.schema.InitialisationPolicyType <em>Initialisation Policy Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.InitialisationPolicyType
	 * @see org.mule.schema.impl.MulePackageImpl#getInitialisationPolicyType()
	 * @generated
	 */
	int INITIALISATION_POLICY_TYPE = 63;

	/**
	 * The meta object id for the '{@link org.mule.schema.InitialStateType <em>Initial State Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.InitialStateType
	 * @see org.mule.schema.impl.MulePackageImpl#getInitialStateType()
	 * @generated
	 */
	int INITIAL_STATE_TYPE = 64;

	/**
	 * The meta object id for the '{@link org.mule.schema.PoolExhaustedActionType <em>Pool Exhausted Action Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.PoolExhaustedActionType
	 * @see org.mule.schema.impl.MulePackageImpl#getPoolExhaustedActionType()
	 * @generated
	 */
	int POOL_EXHAUSTED_ACTION_TYPE = 65;

	/**
	 * The meta object id for the '{@link org.mule.schema.TypeType1 <em>Type Type1</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.TypeType1
	 * @see org.mule.schema.impl.MulePackageImpl#getTypeType1()
	 * @generated
	 */
	int TYPE_TYPE1 = 66;

	/**
	 * The meta object id for the '<em>Action Type Object</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.ActionType
	 * @see org.mule.schema.impl.MulePackageImpl#getActionTypeObject()
	 * @generated
	 */
	int ACTION_TYPE_OBJECT = 67;

	/**
	 * The meta object id for the '<em>Create Connector Type Object</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.CreateConnectorType
	 * @see org.mule.schema.impl.MulePackageImpl#getCreateConnectorTypeObject()
	 * @generated
	 */
	int CREATE_CONNECTOR_TYPE_OBJECT = 68;

	/**
	 * The meta object id for the '<em>Enable Correlation Type Object</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.EnableCorrelationType
	 * @see org.mule.schema.impl.MulePackageImpl#getEnableCorrelationTypeObject()
	 * @generated
	 */
	int ENABLE_CORRELATION_TYPE_OBJECT = 69;

	/**
	 * The meta object id for the '<em>Exhausted Action Type Object</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.ExhaustedActionType
	 * @see org.mule.schema.impl.MulePackageImpl#getExhaustedActionTypeObject()
	 * @generated
	 */
	int EXHAUSTED_ACTION_TYPE_OBJECT = 70;

	/**
	 * The meta object id for the '<em>Id Type Object</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.IdType
	 * @see org.mule.schema.impl.MulePackageImpl#getIdTypeObject()
	 * @generated
	 */
	int ID_TYPE_OBJECT = 71;

	/**
	 * The meta object id for the '<em>Initialisation Policy Type Object</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.InitialisationPolicyType
	 * @see org.mule.schema.impl.MulePackageImpl#getInitialisationPolicyTypeObject()
	 * @generated
	 */
	int INITIALISATION_POLICY_TYPE_OBJECT = 72;

	/**
	 * The meta object id for the '<em>Initial State Type Object</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.InitialStateType
	 * @see org.mule.schema.impl.MulePackageImpl#getInitialStateTypeObject()
	 * @generated
	 */
	int INITIAL_STATE_TYPE_OBJECT = 73;

	/**
	 * The meta object id for the '<em>Pool Exhausted Action Type Object</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.PoolExhaustedActionType
	 * @see org.mule.schema.impl.MulePackageImpl#getPoolExhaustedActionTypeObject()
	 * @generated
	 */
	int POOL_EXHAUSTED_ACTION_TYPE_OBJECT = 74;

	/**
	 * The meta object id for the '<em>Type Type</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see java.lang.String
	 * @see org.mule.schema.impl.MulePackageImpl#getTypeType()
	 * @generated
	 */
	int TYPE_TYPE = 75;

	/**
	 * The meta object id for the '<em>Type Type Object</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.mule.schema.TypeType1
	 * @see org.mule.schema.impl.MulePackageImpl#getTypeTypeObject()
	 * @generated
	 */
	int TYPE_TYPE_OBJECT = 76;

	/**
	 * The meta object id for the '<em>Version Type</em>' data type.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see java.lang.String
	 * @see org.mule.schema.impl.MulePackageImpl#getVersionType()
	 * @generated
	 */
	int VERSION_TYPE = 77;


	/**
	 * Returns the meta object for class '{@link org.mule.schema.AgentsType <em>Agents Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Agents Type</em>'.
	 * @see org.mule.schema.AgentsType
	 * @generated
	 */
	EClass getAgentsType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.AgentsType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.AgentsType#getMixed()
	 * @see #getAgentsType()
	 * @generated
	 */
	EAttribute getAgentsType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.AgentsType#getAgent <em>Agent</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Agent</em>'.
	 * @see org.mule.schema.AgentsType#getAgent()
	 * @see #getAgentsType()
	 * @generated
	 */
	EReference getAgentsType_Agent();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.AgentType <em>Agent Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Agent Type</em>'.
	 * @see org.mule.schema.AgentType
	 * @generated
	 */
	EClass getAgentType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.AgentType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.AgentType#getMixed()
	 * @see #getAgentType()
	 * @generated
	 */
	EAttribute getAgentType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.AgentType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.AgentType#getProperties()
	 * @see #getAgentType()
	 * @generated
	 */
	EReference getAgentType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.AgentType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.AgentType#getClassName()
	 * @see #getAgentType()
	 * @generated
	 */
	EAttribute getAgentType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.AgentType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.AgentType#getName()
	 * @see #getAgentType()
	 * @generated
	 */
	EAttribute getAgentType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.AgentType#getRef <em>Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ref</em>'.
	 * @see org.mule.schema.AgentType#getRef()
	 * @see #getAgentType()
	 * @generated
	 */
	EAttribute getAgentType_Ref();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.CatchAllStrategyType <em>Catch All Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Catch All Strategy Type</em>'.
	 * @see org.mule.schema.CatchAllStrategyType
	 * @generated
	 */
	EClass getCatchAllStrategyType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.CatchAllStrategyType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.CatchAllStrategyType#getMixed()
	 * @see #getCatchAllStrategyType()
	 * @generated
	 */
	EAttribute getCatchAllStrategyType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.CatchAllStrategyType#getEndpoint <em>Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Endpoint</em>'.
	 * @see org.mule.schema.CatchAllStrategyType#getEndpoint()
	 * @see #getCatchAllStrategyType()
	 * @generated
	 */
	EReference getCatchAllStrategyType_Endpoint();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.CatchAllStrategyType#getGlobalEndpoint <em>Global Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Global Endpoint</em>'.
	 * @see org.mule.schema.CatchAllStrategyType#getGlobalEndpoint()
	 * @see #getCatchAllStrategyType()
	 * @generated
	 */
	EReference getCatchAllStrategyType_GlobalEndpoint();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.CatchAllStrategyType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.CatchAllStrategyType#getProperties()
	 * @see #getCatchAllStrategyType()
	 * @generated
	 */
	EReference getCatchAllStrategyType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.CatchAllStrategyType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.CatchAllStrategyType#getClassName()
	 * @see #getCatchAllStrategyType()
	 * @generated
	 */
	EAttribute getCatchAllStrategyType_ClassName();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ComponentFactoryType <em>Component Factory Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Component Factory Type</em>'.
	 * @see org.mule.schema.ComponentFactoryType
	 * @generated
	 */
	EClass getComponentFactoryType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ComponentFactoryType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ComponentFactoryType#getMixed()
	 * @see #getComponentFactoryType()
	 * @generated
	 */
	EAttribute getComponentFactoryType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ComponentFactoryType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.ComponentFactoryType#getClassName()
	 * @see #getComponentFactoryType()
	 * @generated
	 */
	EAttribute getComponentFactoryType_ClassName();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ComponentLifecycleAdapterFactoryType <em>Component Lifecycle Adapter Factory Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Component Lifecycle Adapter Factory Type</em>'.
	 * @see org.mule.schema.ComponentLifecycleAdapterFactoryType
	 * @generated
	 */
	EClass getComponentLifecycleAdapterFactoryType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ComponentLifecycleAdapterFactoryType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ComponentLifecycleAdapterFactoryType#getMixed()
	 * @see #getComponentLifecycleAdapterFactoryType()
	 * @generated
	 */
	EAttribute getComponentLifecycleAdapterFactoryType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ComponentLifecycleAdapterFactoryType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.ComponentLifecycleAdapterFactoryType#getClassName()
	 * @see #getComponentLifecycleAdapterFactoryType()
	 * @generated
	 */
	EAttribute getComponentLifecycleAdapterFactoryType_ClassName();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ComponentPoolFactoryType <em>Component Pool Factory Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Component Pool Factory Type</em>'.
	 * @see org.mule.schema.ComponentPoolFactoryType
	 * @generated
	 */
	EClass getComponentPoolFactoryType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ComponentPoolFactoryType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ComponentPoolFactoryType#getMixed()
	 * @see #getComponentPoolFactoryType()
	 * @generated
	 */
	EAttribute getComponentPoolFactoryType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ComponentPoolFactoryType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.ComponentPoolFactoryType#getProperties()
	 * @see #getComponentPoolFactoryType()
	 * @generated
	 */
	EReference getComponentPoolFactoryType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ComponentPoolFactoryType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.ComponentPoolFactoryType#getClassName()
	 * @see #getComponentPoolFactoryType()
	 * @generated
	 */
	EAttribute getComponentPoolFactoryType_ClassName();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ConnectionStrategyType <em>Connection Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Connection Strategy Type</em>'.
	 * @see org.mule.schema.ConnectionStrategyType
	 * @generated
	 */
	EClass getConnectionStrategyType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ConnectionStrategyType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ConnectionStrategyType#getMixed()
	 * @see #getConnectionStrategyType()
	 * @generated
	 */
	EAttribute getConnectionStrategyType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ConnectionStrategyType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.ConnectionStrategyType#getProperties()
	 * @see #getConnectionStrategyType()
	 * @generated
	 */
	EReference getConnectionStrategyType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConnectionStrategyType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.ConnectionStrategyType#getClassName()
	 * @see #getConnectionStrategyType()
	 * @generated
	 */
	EAttribute getConnectionStrategyType_ClassName();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ConnectorType <em>Connector Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Connector Type</em>'.
	 * @see org.mule.schema.ConnectorType
	 * @generated
	 */
	EClass getConnectorType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ConnectorType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ConnectorType#getMixed()
	 * @see #getConnectorType()
	 * @generated
	 */
	EAttribute getConnectorType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ConnectorType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.ConnectorType#getProperties()
	 * @see #getConnectorType()
	 * @generated
	 */
	EReference getConnectorType_Properties();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ConnectorType#getThreadingProfile <em>Threading Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Threading Profile</em>'.
	 * @see org.mule.schema.ConnectorType#getThreadingProfile()
	 * @see #getConnectorType()
	 * @generated
	 */
	EReference getConnectorType_ThreadingProfile();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ConnectorType#getExceptionStrategy <em>Exception Strategy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Exception Strategy</em>'.
	 * @see org.mule.schema.ConnectorType#getExceptionStrategy()
	 * @see #getConnectorType()
	 * @generated
	 */
	EReference getConnectorType_ExceptionStrategy();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ConnectorType#getConnectionStrategy <em>Connection Strategy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Connection Strategy</em>'.
	 * @see org.mule.schema.ConnectorType#getConnectionStrategy()
	 * @see #getConnectorType()
	 * @generated
	 */
	EReference getConnectorType_ConnectionStrategy();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConnectorType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.ConnectorType#getClassName()
	 * @see #getConnectorType()
	 * @generated
	 */
	EAttribute getConnectorType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConnectorType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.ConnectorType#getName()
	 * @see #getConnectorType()
	 * @generated
	 */
	EAttribute getConnectorType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConnectorType#getRef <em>Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ref</em>'.
	 * @see org.mule.schema.ConnectorType#getRef()
	 * @see #getConnectorType()
	 * @generated
	 */
	EAttribute getConnectorType_Ref();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ConstraintType <em>Constraint Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Constraint Type</em>'.
	 * @see org.mule.schema.ConstraintType
	 * @generated
	 */
	EClass getConstraintType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ConstraintType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ConstraintType#getMixed()
	 * @see #getConstraintType()
	 * @generated
	 */
	EAttribute getConstraintType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ConstraintType#getLeftFilter <em>Left Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Left Filter</em>'.
	 * @see org.mule.schema.ConstraintType#getLeftFilter()
	 * @see #getConstraintType()
	 * @generated
	 */
	EReference getConstraintType_LeftFilter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ConstraintType#getRightFilter <em>Right Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Right Filter</em>'.
	 * @see org.mule.schema.ConstraintType#getRightFilter()
	 * @see #getConstraintType()
	 * @generated
	 */
	EReference getConstraintType_RightFilter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ConstraintType#getFilter <em>Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Filter</em>'.
	 * @see org.mule.schema.ConstraintType#getFilter()
	 * @see #getConstraintType()
	 * @generated
	 */
	EReference getConstraintType_Filter();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConstraintType#getBatchSize <em>Batch Size</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Batch Size</em>'.
	 * @see org.mule.schema.ConstraintType#getBatchSize()
	 * @see #getConstraintType()
	 * @generated
	 */
	EAttribute getConstraintType_BatchSize();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConstraintType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.ConstraintType#getClassName()
	 * @see #getConstraintType()
	 * @generated
	 */
	EAttribute getConstraintType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConstraintType#getExpectedType <em>Expected Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expected Type</em>'.
	 * @see org.mule.schema.ConstraintType#getExpectedType()
	 * @see #getConstraintType()
	 * @generated
	 */
	EAttribute getConstraintType_ExpectedType();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConstraintType#getExpression <em>Expression</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expression</em>'.
	 * @see org.mule.schema.ConstraintType#getExpression()
	 * @see #getConstraintType()
	 * @generated
	 */
	EAttribute getConstraintType_Expression();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConstraintType#getFrequency <em>Frequency</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Frequency</em>'.
	 * @see org.mule.schema.ConstraintType#getFrequency()
	 * @see #getConstraintType()
	 * @generated
	 */
	EAttribute getConstraintType_Frequency();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConstraintType#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see org.mule.schema.ConstraintType#getPath()
	 * @see #getConstraintType()
	 * @generated
	 */
	EAttribute getConstraintType_Path();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ConstraintType#getPattern <em>Pattern</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Pattern</em>'.
	 * @see org.mule.schema.ConstraintType#getPattern()
	 * @see #getConstraintType()
	 * @generated
	 */
	EAttribute getConstraintType_Pattern();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ContainerContextType <em>Container Context Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Container Context Type</em>'.
	 * @see org.mule.schema.ContainerContextType
	 * @generated
	 */
	EClass getContainerContextType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ContainerContextType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ContainerContextType#getMixed()
	 * @see #getContainerContextType()
	 * @generated
	 */
	EAttribute getContainerContextType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ContainerContextType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.ContainerContextType#getProperties()
	 * @see #getContainerContextType()
	 * @generated
	 */
	EReference getContainerContextType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ContainerContextType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.ContainerContextType#getClassName()
	 * @see #getContainerContextType()
	 * @generated
	 */
	EAttribute getContainerContextType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ContainerContextType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.ContainerContextType#getName()
	 * @see #getContainerContextType()
	 * @generated
	 */
	EAttribute getContainerContextType_Name();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ContainerEntryType <em>Container Entry Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Container Entry Type</em>'.
	 * @see org.mule.schema.ContainerEntryType
	 * @generated
	 */
	EClass getContainerEntryType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ContainerEntryType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ContainerEntryType#getMixed()
	 * @see #getContainerEntryType()
	 * @generated
	 */
	EAttribute getContainerEntryType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ContainerEntryType#getReference <em>Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Reference</em>'.
	 * @see org.mule.schema.ContainerEntryType#getReference()
	 * @see #getContainerEntryType()
	 * @generated
	 */
	EAttribute getContainerEntryType_Reference();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ContainerEntryType#isRequired <em>Required</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Required</em>'.
	 * @see org.mule.schema.ContainerEntryType#isRequired()
	 * @see #getContainerEntryType()
	 * @generated
	 */
	EAttribute getContainerEntryType_Required();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ContainerPropertyType <em>Container Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Container Property Type</em>'.
	 * @see org.mule.schema.ContainerPropertyType
	 * @generated
	 */
	EClass getContainerPropertyType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ContainerPropertyType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ContainerPropertyType#getMixed()
	 * @see #getContainerPropertyType()
	 * @generated
	 */
	EAttribute getContainerPropertyType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ContainerPropertyType#getContainer <em>Container</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Container</em>'.
	 * @see org.mule.schema.ContainerPropertyType#getContainer()
	 * @see #getContainerPropertyType()
	 * @generated
	 */
	EAttribute getContainerPropertyType_Container();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ContainerPropertyType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.ContainerPropertyType#getName()
	 * @see #getContainerPropertyType()
	 * @generated
	 */
	EAttribute getContainerPropertyType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ContainerPropertyType#getReference <em>Reference</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Reference</em>'.
	 * @see org.mule.schema.ContainerPropertyType#getReference()
	 * @see #getContainerPropertyType()
	 * @generated
	 */
	EAttribute getContainerPropertyType_Reference();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ContainerPropertyType#isRequired <em>Required</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Required</em>'.
	 * @see org.mule.schema.ContainerPropertyType#isRequired()
	 * @see #getContainerPropertyType()
	 * @generated
	 */
	EAttribute getContainerPropertyType_Required();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.DocumentRoot <em>Document Root</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Document Root</em>'.
	 * @see org.mule.schema.DocumentRoot
	 * @generated
	 */
	EClass getDocumentRoot();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.DocumentRoot#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.DocumentRoot#getMixed()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EAttribute getDocumentRoot_Mixed();

	/**
	 * Returns the meta object for the map '{@link org.mule.schema.DocumentRoot#getXMLNSPrefixMap <em>XMLNS Prefix Map</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>XMLNS Prefix Map</em>'.
	 * @see org.mule.schema.DocumentRoot#getXMLNSPrefixMap()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EReference getDocumentRoot_XMLNSPrefixMap();

	/**
	 * Returns the meta object for the map '{@link org.mule.schema.DocumentRoot#getXSISchemaLocation <em>XSI Schema Location</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the map '<em>XSI Schema Location</em>'.
	 * @see org.mule.schema.DocumentRoot#getXSISchemaLocation()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EReference getDocumentRoot_XSISchemaLocation();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.DocumentRoot#getMuleConfiguration <em>Mule Configuration</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Mule Configuration</em>'.
	 * @see org.mule.schema.DocumentRoot#getMuleConfiguration()
	 * @see #getDocumentRoot()
	 * @generated
	 */
	EReference getDocumentRoot_MuleConfiguration();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.EncryptionStrategyType <em>Encryption Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Encryption Strategy Type</em>'.
	 * @see org.mule.schema.EncryptionStrategyType
	 * @generated
	 */
	EClass getEncryptionStrategyType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.EncryptionStrategyType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.EncryptionStrategyType#getMixed()
	 * @see #getEncryptionStrategyType()
	 * @generated
	 */
	EAttribute getEncryptionStrategyType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.EncryptionStrategyType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.EncryptionStrategyType#getProperties()
	 * @see #getEncryptionStrategyType()
	 * @generated
	 */
	EReference getEncryptionStrategyType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EncryptionStrategyType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.EncryptionStrategyType#getClassName()
	 * @see #getEncryptionStrategyType()
	 * @generated
	 */
	EAttribute getEncryptionStrategyType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EncryptionStrategyType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.EncryptionStrategyType#getName()
	 * @see #getEncryptionStrategyType()
	 * @generated
	 */
	EAttribute getEncryptionStrategyType_Name();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.EndpointIdentifiersType <em>Endpoint Identifiers Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Endpoint Identifiers Type</em>'.
	 * @see org.mule.schema.EndpointIdentifiersType
	 * @generated
	 */
	EClass getEndpointIdentifiersType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.EndpointIdentifiersType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.EndpointIdentifiersType#getMixed()
	 * @see #getEndpointIdentifiersType()
	 * @generated
	 */
	EAttribute getEndpointIdentifiersType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.EndpointIdentifiersType#getEndpointIdentifier <em>Endpoint Identifier</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Endpoint Identifier</em>'.
	 * @see org.mule.schema.EndpointIdentifiersType#getEndpointIdentifier()
	 * @see #getEndpointIdentifiersType()
	 * @generated
	 */
	EReference getEndpointIdentifiersType_EndpointIdentifier();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.EndpointIdentifierType <em>Endpoint Identifier Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Endpoint Identifier Type</em>'.
	 * @see org.mule.schema.EndpointIdentifierType
	 * @generated
	 */
	EClass getEndpointIdentifierType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.EndpointIdentifierType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.EndpointIdentifierType#getMixed()
	 * @see #getEndpointIdentifierType()
	 * @generated
	 */
	EAttribute getEndpointIdentifierType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointIdentifierType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.EndpointIdentifierType#getName()
	 * @see #getEndpointIdentifierType()
	 * @generated
	 */
	EAttribute getEndpointIdentifierType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointIdentifierType#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.mule.schema.EndpointIdentifierType#getValue()
	 * @see #getEndpointIdentifierType()
	 * @generated
	 */
	EAttribute getEndpointIdentifierType_Value();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.EndpointType <em>Endpoint Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Endpoint Type</em>'.
	 * @see org.mule.schema.EndpointType
	 * @generated
	 */
	EClass getEndpointType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.EndpointType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.EndpointType#getMixed()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.EndpointType#getTransaction <em>Transaction</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Transaction</em>'.
	 * @see org.mule.schema.EndpointType#getTransaction()
	 * @see #getEndpointType()
	 * @generated
	 */
	EReference getEndpointType_Transaction();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.EndpointType#getFilter <em>Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Filter</em>'.
	 * @see org.mule.schema.EndpointType#getFilter()
	 * @see #getEndpointType()
	 * @generated
	 */
	EReference getEndpointType_Filter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.EndpointType#getSecurityFilter <em>Security Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Security Filter</em>'.
	 * @see org.mule.schema.EndpointType#getSecurityFilter()
	 * @see #getEndpointType()
	 * @generated
	 */
	EReference getEndpointType_SecurityFilter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.EndpointType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.EndpointType#getProperties()
	 * @see #getEndpointType()
	 * @generated
	 */
	EReference getEndpointType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#getAddress <em>Address</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Address</em>'.
	 * @see org.mule.schema.EndpointType#getAddress()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_Address();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#getConnector <em>Connector</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Connector</em>'.
	 * @see org.mule.schema.EndpointType#getConnector()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_Connector();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#getCreateConnector <em>Create Connector</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Create Connector</em>'.
	 * @see org.mule.schema.EndpointType#getCreateConnector()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_CreateConnector();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.EndpointType#getName()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#getRef <em>Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ref</em>'.
	 * @see org.mule.schema.EndpointType#getRef()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_Ref();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#isRemoteSync <em>Remote Sync</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Remote Sync</em>'.
	 * @see org.mule.schema.EndpointType#isRemoteSync()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_RemoteSync();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#getRemoteSyncTimeout <em>Remote Sync Timeout</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Remote Sync Timeout</em>'.
	 * @see org.mule.schema.EndpointType#getRemoteSyncTimeout()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_RemoteSyncTimeout();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#getResponseTransformers <em>Response Transformers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Response Transformers</em>'.
	 * @see org.mule.schema.EndpointType#getResponseTransformers()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_ResponseTransformers();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#isSynchronous <em>Synchronous</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Synchronous</em>'.
	 * @see org.mule.schema.EndpointType#isSynchronous()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_Synchronous();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#getTransformers <em>Transformers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Transformers</em>'.
	 * @see org.mule.schema.EndpointType#getTransformers()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_Transformers();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EndpointType#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see org.mule.schema.EndpointType#getType()
	 * @see #getEndpointType()
	 * @generated
	 */
	EAttribute getEndpointType_Type();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.EntryPointResolverType <em>Entry Point Resolver Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Entry Point Resolver Type</em>'.
	 * @see org.mule.schema.EntryPointResolverType
	 * @generated
	 */
	EClass getEntryPointResolverType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.EntryPointResolverType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.EntryPointResolverType#getMixed()
	 * @see #getEntryPointResolverType()
	 * @generated
	 */
	EAttribute getEntryPointResolverType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EntryPointResolverType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.EntryPointResolverType#getClassName()
	 * @see #getEntryPointResolverType()
	 * @generated
	 */
	EAttribute getEntryPointResolverType_ClassName();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.EntryType <em>Entry Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Entry Type</em>'.
	 * @see org.mule.schema.EntryType
	 * @generated
	 */
	EClass getEntryType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.EntryType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.EntryType#getMixed()
	 * @see #getEntryType()
	 * @generated
	 */
	EAttribute getEntryType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.EntryType#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.mule.schema.EntryType#getValue()
	 * @see #getEntryType()
	 * @generated
	 */
	EAttribute getEntryType_Value();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.EnvironmentPropertiesType <em>Environment Properties Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Environment Properties Type</em>'.
	 * @see org.mule.schema.EnvironmentPropertiesType
	 * @generated
	 */
	EClass getEnvironmentPropertiesType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.EnvironmentPropertiesType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.EnvironmentPropertiesType#getMixed()
	 * @see #getEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getEnvironmentPropertiesType_Mixed();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.EnvironmentPropertiesType#getGroup <em>Group</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Group</em>'.
	 * @see org.mule.schema.EnvironmentPropertiesType#getGroup()
	 * @see #getEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getEnvironmentPropertiesType_Group();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.EnvironmentPropertiesType#getProperty <em>Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Property</em>'.
	 * @see org.mule.schema.EnvironmentPropertiesType#getProperty()
	 * @see #getEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getEnvironmentPropertiesType_Property();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.EnvironmentPropertiesType#getFactoryProperty <em>Factory Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Factory Property</em>'.
	 * @see org.mule.schema.EnvironmentPropertiesType#getFactoryProperty()
	 * @see #getEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getEnvironmentPropertiesType_FactoryProperty();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.EnvironmentPropertiesType#getSystemProperty <em>System Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>System Property</em>'.
	 * @see org.mule.schema.EnvironmentPropertiesType#getSystemProperty()
	 * @see #getEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getEnvironmentPropertiesType_SystemProperty();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.EnvironmentPropertiesType#getMap <em>Map</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Map</em>'.
	 * @see org.mule.schema.EnvironmentPropertiesType#getMap()
	 * @see #getEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getEnvironmentPropertiesType_Map();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.EnvironmentPropertiesType#getList <em>List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>List</em>'.
	 * @see org.mule.schema.EnvironmentPropertiesType#getList()
	 * @see #getEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getEnvironmentPropertiesType_List();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.EnvironmentPropertiesType#getFileProperties <em>File Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>File Properties</em>'.
	 * @see org.mule.schema.EnvironmentPropertiesType#getFileProperties()
	 * @see #getEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getEnvironmentPropertiesType_FileProperties();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ExceptionStrategyType <em>Exception Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Exception Strategy Type</em>'.
	 * @see org.mule.schema.ExceptionStrategyType
	 * @generated
	 */
	EClass getExceptionStrategyType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ExceptionStrategyType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ExceptionStrategyType#getMixed()
	 * @see #getExceptionStrategyType()
	 * @generated
	 */
	EAttribute getExceptionStrategyType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ExceptionStrategyType#getEndpoint <em>Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Endpoint</em>'.
	 * @see org.mule.schema.ExceptionStrategyType#getEndpoint()
	 * @see #getExceptionStrategyType()
	 * @generated
	 */
	EReference getExceptionStrategyType_Endpoint();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ExceptionStrategyType#getGlobalEndpoint <em>Global Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Global Endpoint</em>'.
	 * @see org.mule.schema.ExceptionStrategyType#getGlobalEndpoint()
	 * @see #getExceptionStrategyType()
	 * @generated
	 */
	EReference getExceptionStrategyType_GlobalEndpoint();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ExceptionStrategyType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.ExceptionStrategyType#getProperties()
	 * @see #getExceptionStrategyType()
	 * @generated
	 */
	EReference getExceptionStrategyType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ExceptionStrategyType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.ExceptionStrategyType#getClassName()
	 * @see #getExceptionStrategyType()
	 * @generated
	 */
	EAttribute getExceptionStrategyType_ClassName();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.FactoryEntryType <em>Factory Entry Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Factory Entry Type</em>'.
	 * @see org.mule.schema.FactoryEntryType
	 * @generated
	 */
	EClass getFactoryEntryType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.FactoryEntryType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.FactoryEntryType#getMixed()
	 * @see #getFactoryEntryType()
	 * @generated
	 */
	EAttribute getFactoryEntryType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FactoryEntryType#getFactory <em>Factory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Factory</em>'.
	 * @see org.mule.schema.FactoryEntryType#getFactory()
	 * @see #getFactoryEntryType()
	 * @generated
	 */
	EAttribute getFactoryEntryType_Factory();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.FactoryPropertyType <em>Factory Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Factory Property Type</em>'.
	 * @see org.mule.schema.FactoryPropertyType
	 * @generated
	 */
	EClass getFactoryPropertyType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.FactoryPropertyType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.FactoryPropertyType#getMixed()
	 * @see #getFactoryPropertyType()
	 * @generated
	 */
	EAttribute getFactoryPropertyType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FactoryPropertyType#getFactory <em>Factory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Factory</em>'.
	 * @see org.mule.schema.FactoryPropertyType#getFactory()
	 * @see #getFactoryPropertyType()
	 * @generated
	 */
	EAttribute getFactoryPropertyType_Factory();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FactoryPropertyType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.FactoryPropertyType#getName()
	 * @see #getFactoryPropertyType()
	 * @generated
	 */
	EAttribute getFactoryPropertyType_Name();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.FilePropertiesType <em>File Properties Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>File Properties Type</em>'.
	 * @see org.mule.schema.FilePropertiesType
	 * @generated
	 */
	EClass getFilePropertiesType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.FilePropertiesType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.FilePropertiesType#getMixed()
	 * @see #getFilePropertiesType()
	 * @generated
	 */
	EAttribute getFilePropertiesType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FilePropertiesType#getLocation <em>Location</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Location</em>'.
	 * @see org.mule.schema.FilePropertiesType#getLocation()
	 * @see #getFilePropertiesType()
	 * @generated
	 */
	EAttribute getFilePropertiesType_Location();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FilePropertiesType#isOverride <em>Override</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Override</em>'.
	 * @see org.mule.schema.FilePropertiesType#isOverride()
	 * @see #getFilePropertiesType()
	 * @generated
	 */
	EAttribute getFilePropertiesType_Override();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.FilterType <em>Filter Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Filter Type</em>'.
	 * @see org.mule.schema.FilterType
	 * @generated
	 */
	EClass getFilterType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.FilterType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.FilterType#getMixed()
	 * @see #getFilterType()
	 * @generated
	 */
	EAttribute getFilterType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.FilterType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.FilterType#getProperties()
	 * @see #getFilterType()
	 * @generated
	 */
	EReference getFilterType_Properties();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.FilterType#getFilter <em>Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Filter</em>'.
	 * @see org.mule.schema.FilterType#getFilter()
	 * @see #getFilterType()
	 * @generated
	 */
	EReference getFilterType_Filter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.FilterType#getLeftFilter <em>Left Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Left Filter</em>'.
	 * @see org.mule.schema.FilterType#getLeftFilter()
	 * @see #getFilterType()
	 * @generated
	 */
	EReference getFilterType_LeftFilter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.FilterType#getRightFilter <em>Right Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Right Filter</em>'.
	 * @see org.mule.schema.FilterType#getRightFilter()
	 * @see #getFilterType()
	 * @generated
	 */
	EReference getFilterType_RightFilter();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FilterType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.FilterType#getClassName()
	 * @see #getFilterType()
	 * @generated
	 */
	EAttribute getFilterType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FilterType#getConfigFile <em>Config File</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Config File</em>'.
	 * @see org.mule.schema.FilterType#getConfigFile()
	 * @see #getFilterType()
	 * @generated
	 */
	EAttribute getFilterType_ConfigFile();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FilterType#getExpectedType <em>Expected Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expected Type</em>'.
	 * @see org.mule.schema.FilterType#getExpectedType()
	 * @see #getFilterType()
	 * @generated
	 */
	EAttribute getFilterType_ExpectedType();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FilterType#getExpression <em>Expression</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expression</em>'.
	 * @see org.mule.schema.FilterType#getExpression()
	 * @see #getFilterType()
	 * @generated
	 */
	EAttribute getFilterType_Expression();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FilterType#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see org.mule.schema.FilterType#getPath()
	 * @see #getFilterType()
	 * @generated
	 */
	EAttribute getFilterType_Path();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.FilterType#getPattern <em>Pattern</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Pattern</em>'.
	 * @see org.mule.schema.FilterType#getPattern()
	 * @see #getFilterType()
	 * @generated
	 */
	EAttribute getFilterType_Pattern();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.GlobalEndpointsType <em>Global Endpoints Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Global Endpoints Type</em>'.
	 * @see org.mule.schema.GlobalEndpointsType
	 * @generated
	 */
	EClass getGlobalEndpointsType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.GlobalEndpointsType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.GlobalEndpointsType#getMixed()
	 * @see #getGlobalEndpointsType()
	 * @generated
	 */
	EAttribute getGlobalEndpointsType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.GlobalEndpointsType#getEndpoint <em>Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Endpoint</em>'.
	 * @see org.mule.schema.GlobalEndpointsType#getEndpoint()
	 * @see #getGlobalEndpointsType()
	 * @generated
	 */
	EReference getGlobalEndpointsType_Endpoint();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.GlobalEndpointType <em>Global Endpoint Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Global Endpoint Type</em>'.
	 * @see org.mule.schema.GlobalEndpointType
	 * @generated
	 */
	EClass getGlobalEndpointType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.GlobalEndpointType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getMixed()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EAttribute getGlobalEndpointType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.GlobalEndpointType#getTransaction <em>Transaction</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Transaction</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getTransaction()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EReference getGlobalEndpointType_Transaction();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.GlobalEndpointType#getFilter <em>Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Filter</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getFilter()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EReference getGlobalEndpointType_Filter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.GlobalEndpointType#getSecurityFilter <em>Security Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Security Filter</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getSecurityFilter()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EReference getGlobalEndpointType_SecurityFilter();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.GlobalEndpointType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Properties</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getProperties()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EReference getGlobalEndpointType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.GlobalEndpointType#getAddress <em>Address</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Address</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getAddress()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EAttribute getGlobalEndpointType_Address();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.GlobalEndpointType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getName()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EAttribute getGlobalEndpointType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.GlobalEndpointType#isRemoteSync <em>Remote Sync</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Remote Sync</em>'.
	 * @see org.mule.schema.GlobalEndpointType#isRemoteSync()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EAttribute getGlobalEndpointType_RemoteSync();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.GlobalEndpointType#getRemoteSyncTimeout <em>Remote Sync Timeout</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Remote Sync Timeout</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getRemoteSyncTimeout()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EAttribute getGlobalEndpointType_RemoteSyncTimeout();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.GlobalEndpointType#getResponseTransformers <em>Response Transformers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Response Transformers</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getResponseTransformers()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EAttribute getGlobalEndpointType_ResponseTransformers();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.GlobalEndpointType#isSynchronous <em>Synchronous</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Synchronous</em>'.
	 * @see org.mule.schema.GlobalEndpointType#isSynchronous()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EAttribute getGlobalEndpointType_Synchronous();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.GlobalEndpointType#getTransformers <em>Transformers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Transformers</em>'.
	 * @see org.mule.schema.GlobalEndpointType#getTransformers()
	 * @see #getGlobalEndpointType()
	 * @generated
	 */
	EAttribute getGlobalEndpointType_Transformers();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.InboundRouterType <em>Inbound Router Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Inbound Router Type</em>'.
	 * @see org.mule.schema.InboundRouterType
	 * @generated
	 */
	EClass getInboundRouterType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.InboundRouterType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.InboundRouterType#getMixed()
	 * @see #getInboundRouterType()
	 * @generated
	 */
	EAttribute getInboundRouterType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.InboundRouterType#getCatchAllStrategy <em>Catch All Strategy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Catch All Strategy</em>'.
	 * @see org.mule.schema.InboundRouterType#getCatchAllStrategy()
	 * @see #getInboundRouterType()
	 * @generated
	 */
	EReference getInboundRouterType_CatchAllStrategy();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.InboundRouterType#getEndpoint <em>Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Endpoint</em>'.
	 * @see org.mule.schema.InboundRouterType#getEndpoint()
	 * @see #getInboundRouterType()
	 * @generated
	 */
	EReference getInboundRouterType_Endpoint();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.InboundRouterType#getGlobalEndpoint <em>Global Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Global Endpoint</em>'.
	 * @see org.mule.schema.InboundRouterType#getGlobalEndpoint()
	 * @see #getInboundRouterType()
	 * @generated
	 */
	EReference getInboundRouterType_GlobalEndpoint();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.InboundRouterType#getRouter <em>Router</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Router</em>'.
	 * @see org.mule.schema.InboundRouterType#getRouter()
	 * @see #getInboundRouterType()
	 * @generated
	 */
	EReference getInboundRouterType_Router();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.InboundRouterType#isMatchAll <em>Match All</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Match All</em>'.
	 * @see org.mule.schema.InboundRouterType#isMatchAll()
	 * @see #getInboundRouterType()
	 * @generated
	 */
	EAttribute getInboundRouterType_MatchAll();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.InterceptorStackType <em>Interceptor Stack Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Interceptor Stack Type</em>'.
	 * @see org.mule.schema.InterceptorStackType
	 * @generated
	 */
	EClass getInterceptorStackType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.InterceptorStackType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.InterceptorStackType#getMixed()
	 * @see #getInterceptorStackType()
	 * @generated
	 */
	EAttribute getInterceptorStackType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.InterceptorStackType#getInterceptor <em>Interceptor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Interceptor</em>'.
	 * @see org.mule.schema.InterceptorStackType#getInterceptor()
	 * @see #getInterceptorStackType()
	 * @generated
	 */
	EReference getInterceptorStackType_Interceptor();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.InterceptorStackType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.InterceptorStackType#getName()
	 * @see #getInterceptorStackType()
	 * @generated
	 */
	EAttribute getInterceptorStackType_Name();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.InterceptorType <em>Interceptor Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Interceptor Type</em>'.
	 * @see org.mule.schema.InterceptorType
	 * @generated
	 */
	EClass getInterceptorType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.InterceptorType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.InterceptorType#getMixed()
	 * @see #getInterceptorType()
	 * @generated
	 */
	EAttribute getInterceptorType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.InterceptorType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.InterceptorType#getProperties()
	 * @see #getInterceptorType()
	 * @generated
	 */
	EReference getInterceptorType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.InterceptorType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.InterceptorType#getClassName()
	 * @see #getInterceptorType()
	 * @generated
	 */
	EAttribute getInterceptorType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.InterceptorType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.InterceptorType#getName()
	 * @see #getInterceptorType()
	 * @generated
	 */
	EAttribute getInterceptorType_Name();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.LeftFilterType <em>Left Filter Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Left Filter Type</em>'.
	 * @see org.mule.schema.LeftFilterType
	 * @generated
	 */
	EClass getLeftFilterType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.LeftFilterType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.LeftFilterType#getMixed()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EAttribute getLeftFilterType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.LeftFilterType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.LeftFilterType#getProperties()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EReference getLeftFilterType_Properties();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.LeftFilterType#getFilter <em>Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Filter</em>'.
	 * @see org.mule.schema.LeftFilterType#getFilter()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EReference getLeftFilterType_Filter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.LeftFilterType#getLeftFilter <em>Left Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Left Filter</em>'.
	 * @see org.mule.schema.LeftFilterType#getLeftFilter()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EReference getLeftFilterType_LeftFilter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.LeftFilterType#getRightFilter <em>Right Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Right Filter</em>'.
	 * @see org.mule.schema.LeftFilterType#getRightFilter()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EReference getLeftFilterType_RightFilter();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.LeftFilterType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.LeftFilterType#getClassName()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EAttribute getLeftFilterType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.LeftFilterType#getConfigFile <em>Config File</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Config File</em>'.
	 * @see org.mule.schema.LeftFilterType#getConfigFile()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EAttribute getLeftFilterType_ConfigFile();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.LeftFilterType#getExpectedType <em>Expected Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expected Type</em>'.
	 * @see org.mule.schema.LeftFilterType#getExpectedType()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EAttribute getLeftFilterType_ExpectedType();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.LeftFilterType#getExpression <em>Expression</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expression</em>'.
	 * @see org.mule.schema.LeftFilterType#getExpression()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EAttribute getLeftFilterType_Expression();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.LeftFilterType#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see org.mule.schema.LeftFilterType#getPath()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EAttribute getLeftFilterType_Path();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.LeftFilterType#getPattern <em>Pattern</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Pattern</em>'.
	 * @see org.mule.schema.LeftFilterType#getPattern()
	 * @see #getLeftFilterType()
	 * @generated
	 */
	EAttribute getLeftFilterType_Pattern();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ListType <em>List Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>List Type</em>'.
	 * @see org.mule.schema.ListType
	 * @generated
	 */
	EClass getListType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ListType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ListType#getMixed()
	 * @see #getListType()
	 * @generated
	 */
	EAttribute getListType_Mixed();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ListType#getGroup <em>Group</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Group</em>'.
	 * @see org.mule.schema.ListType#getGroup()
	 * @see #getListType()
	 * @generated
	 */
	EAttribute getListType_Group();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ListType#getEntry <em>Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Entry</em>'.
	 * @see org.mule.schema.ListType#getEntry()
	 * @see #getListType()
	 * @generated
	 */
	EReference getListType_Entry();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ListType#getFactoryEntry <em>Factory Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Factory Entry</em>'.
	 * @see org.mule.schema.ListType#getFactoryEntry()
	 * @see #getListType()
	 * @generated
	 */
	EReference getListType_FactoryEntry();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ListType#getSystemEntry <em>System Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>System Entry</em>'.
	 * @see org.mule.schema.ListType#getSystemEntry()
	 * @see #getListType()
	 * @generated
	 */
	EReference getListType_SystemEntry();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ListType#getContainerEntry <em>Container Entry</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Container Entry</em>'.
	 * @see org.mule.schema.ListType#getContainerEntry()
	 * @see #getListType()
	 * @generated
	 */
	EReference getListType_ContainerEntry();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ListType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.ListType#getName()
	 * @see #getListType()
	 * @generated
	 */
	EAttribute getListType_Name();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.MapType <em>Map Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Map Type</em>'.
	 * @see org.mule.schema.MapType
	 * @generated
	 */
	EClass getMapType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.MapType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.MapType#getMixed()
	 * @see #getMapType()
	 * @generated
	 */
	EAttribute getMapType_Mixed();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.MapType#getGroup <em>Group</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Group</em>'.
	 * @see org.mule.schema.MapType#getGroup()
	 * @see #getMapType()
	 * @generated
	 */
	EAttribute getMapType_Group();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MapType#getProperty <em>Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Property</em>'.
	 * @see org.mule.schema.MapType#getProperty()
	 * @see #getMapType()
	 * @generated
	 */
	EReference getMapType_Property();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MapType#getFactoryProperty <em>Factory Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Factory Property</em>'.
	 * @see org.mule.schema.MapType#getFactoryProperty()
	 * @see #getMapType()
	 * @generated
	 */
	EReference getMapType_FactoryProperty();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MapType#getContainerProperty <em>Container Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Container Property</em>'.
	 * @see org.mule.schema.MapType#getContainerProperty()
	 * @see #getMapType()
	 * @generated
	 */
	EReference getMapType_ContainerProperty();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MapType#getSystemProperty <em>System Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>System Property</em>'.
	 * @see org.mule.schema.MapType#getSystemProperty()
	 * @see #getMapType()
	 * @generated
	 */
	EReference getMapType_SystemProperty();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MapType#getMap <em>Map</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Map</em>'.
	 * @see org.mule.schema.MapType#getMap()
	 * @see #getMapType()
	 * @generated
	 */
	EReference getMapType_Map();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MapType#getList <em>List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>List</em>'.
	 * @see org.mule.schema.MapType#getList()
	 * @see #getMapType()
	 * @generated
	 */
	EReference getMapType_List();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MapType#getFileProperties <em>File Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>File Properties</em>'.
	 * @see org.mule.schema.MapType#getFileProperties()
	 * @see #getMapType()
	 * @generated
	 */
	EReference getMapType_FileProperties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MapType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.MapType#getName()
	 * @see #getMapType()
	 * @generated
	 */
	EAttribute getMapType_Name();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ModelType <em>Model Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Model Type</em>'.
	 * @see org.mule.schema.ModelType
	 * @generated
	 */
	EClass getModelType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ModelType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ModelType#getMixed()
	 * @see #getModelType()
	 * @generated
	 */
	EAttribute getModelType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ModelType#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.mule.schema.ModelType#getDescription()
	 * @see #getModelType()
	 * @generated
	 */
	EAttribute getModelType_Description();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ModelType#getEntryPointResolver <em>Entry Point Resolver</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Entry Point Resolver</em>'.
	 * @see org.mule.schema.ModelType#getEntryPointResolver()
	 * @see #getModelType()
	 * @generated
	 */
	EReference getModelType_EntryPointResolver();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ModelType#getComponentFactory <em>Component Factory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Component Factory</em>'.
	 * @see org.mule.schema.ModelType#getComponentFactory()
	 * @see #getModelType()
	 * @generated
	 */
	EReference getModelType_ComponentFactory();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ModelType#getComponentLifecycleAdapterFactory <em>Component Lifecycle Adapter Factory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Component Lifecycle Adapter Factory</em>'.
	 * @see org.mule.schema.ModelType#getComponentLifecycleAdapterFactory()
	 * @see #getModelType()
	 * @generated
	 */
	EReference getModelType_ComponentLifecycleAdapterFactory();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ModelType#getComponentPoolFactory <em>Component Pool Factory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Component Pool Factory</em>'.
	 * @see org.mule.schema.ModelType#getComponentPoolFactory()
	 * @see #getModelType()
	 * @generated
	 */
	EReference getModelType_ComponentPoolFactory();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.ModelType#getExceptionStrategy <em>Exception Strategy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Exception Strategy</em>'.
	 * @see org.mule.schema.ModelType#getExceptionStrategy()
	 * @see #getModelType()
	 * @generated
	 */
	EReference getModelType_ExceptionStrategy();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ModelType#getMuleDescriptor <em>Mule Descriptor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Mule Descriptor</em>'.
	 * @see org.mule.schema.ModelType#getMuleDescriptor()
	 * @see #getModelType()
	 * @generated
	 */
	EReference getModelType_MuleDescriptor();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ModelType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.ModelType#getClassName()
	 * @see #getModelType()
	 * @generated
	 */
	EAttribute getModelType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ModelType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.ModelType#getName()
	 * @see #getModelType()
	 * @generated
	 */
	EAttribute getModelType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ModelType#getRef <em>Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ref</em>'.
	 * @see org.mule.schema.ModelType#getRef()
	 * @see #getModelType()
	 * @generated
	 */
	EAttribute getModelType_Ref();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ModelType#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see org.mule.schema.ModelType#getType()
	 * @see #getModelType()
	 * @generated
	 */
	EAttribute getModelType_Type();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.MuleConfigurationType <em>Configuration Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Configuration Type</em>'.
	 * @see org.mule.schema.MuleConfigurationType
	 * @generated
	 */
	EClass getMuleConfigurationType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.MuleConfigurationType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getMixed()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EAttribute getMuleConfigurationType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleConfigurationType#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getDescription()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EAttribute getMuleConfigurationType_Description();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleConfigurationType#getEnvironmentProperties <em>Environment Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Environment Properties</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getEnvironmentProperties()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_EnvironmentProperties();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleConfigurationType#getMuleEnvironmentProperties <em>Mule Environment Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Mule Environment Properties</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getMuleEnvironmentProperties()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_MuleEnvironmentProperties();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MuleConfigurationType#getContainerContext <em>Container Context</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Container Context</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getContainerContext()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_ContainerContext();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleConfigurationType#getSecurityManager <em>Security Manager</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Security Manager</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getSecurityManager()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_SecurityManager();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleConfigurationType#getTransactionManager <em>Transaction Manager</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Transaction Manager</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getTransactionManager()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_TransactionManager();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleConfigurationType#getAgents <em>Agents</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Agents</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getAgents()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_Agents();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MuleConfigurationType#getConnector <em>Connector</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Connector</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getConnector()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_Connector();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleConfigurationType#getEndpointIdentifiers <em>Endpoint Identifiers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Endpoint Identifiers</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getEndpointIdentifiers()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_EndpointIdentifiers();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleConfigurationType#getTransformers <em>Transformers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Transformers</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getTransformers()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_Transformers();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleConfigurationType#getGlobalEndpoints <em>Global Endpoints</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Global Endpoints</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getGlobalEndpoints()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_GlobalEndpoints();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MuleConfigurationType#getInterceptorStack <em>Interceptor Stack</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Interceptor Stack</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getInterceptorStack()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_InterceptorStack();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleConfigurationType#getModel <em>Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Model</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getModel()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_Model();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MuleConfigurationType#getMuleDescriptor <em>Mule Descriptor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Mule Descriptor</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getMuleDescriptor()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EReference getMuleConfigurationType_MuleDescriptor();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleConfigurationType#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getId()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EAttribute getMuleConfigurationType_Id();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleConfigurationType#getVersion <em>Version</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Version</em>'.
	 * @see org.mule.schema.MuleConfigurationType#getVersion()
	 * @see #getMuleConfigurationType()
	 * @generated
	 */
	EAttribute getMuleConfigurationType_Version();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.MuleDescriptorType <em>Descriptor Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Descriptor Type</em>'.
	 * @see org.mule.schema.MuleDescriptorType
	 * @generated
	 */
	EClass getMuleDescriptorType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.MuleDescriptorType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getMixed()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleDescriptorType#getInboundRouter <em>Inbound Router</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Inbound Router</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getInboundRouter()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EReference getMuleDescriptorType_InboundRouter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleDescriptorType#getOutboundRouter <em>Outbound Router</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Outbound Router</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getOutboundRouter()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EReference getMuleDescriptorType_OutboundRouter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleDescriptorType#getResponseRouter <em>Response Router</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Response Router</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getResponseRouter()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EReference getMuleDescriptorType_ResponseRouter();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MuleDescriptorType#getInterceptor <em>Interceptor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Interceptor</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getInterceptor()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EReference getMuleDescriptorType_Interceptor();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleDescriptorType#getThreadingProfile <em>Threading Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Threading Profile</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getThreadingProfile()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EReference getMuleDescriptorType_ThreadingProfile();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleDescriptorType#getPoolingProfile <em>Pooling Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Pooling Profile</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getPoolingProfile()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EReference getMuleDescriptorType_PoolingProfile();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleDescriptorType#getQueueProfile <em>Queue Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Queue Profile</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getQueueProfile()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EReference getMuleDescriptorType_QueueProfile();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleDescriptorType#getExceptionStrategy <em>Exception Strategy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Exception Strategy</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getExceptionStrategy()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EReference getMuleDescriptorType_ExceptionStrategy();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleDescriptorType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getProperties()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EReference getMuleDescriptorType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#isContainerManaged <em>Container Managed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Container Managed</em>'.
	 * @see org.mule.schema.MuleDescriptorType#isContainerManaged()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_ContainerManaged();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getImplementation <em>Implementation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Implementation</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getImplementation()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_Implementation();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getInboundEndpoint <em>Inbound Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Inbound Endpoint</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getInboundEndpoint()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_InboundEndpoint();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getInboundTransformer <em>Inbound Transformer</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Inbound Transformer</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getInboundTransformer()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_InboundTransformer();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getInitialState <em>Initial State</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Initial State</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getInitialState()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_InitialState();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getName()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getOutboundEndpoint <em>Outbound Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Outbound Endpoint</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getOutboundEndpoint()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_OutboundEndpoint();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getOutboundTransformer <em>Outbound Transformer</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Outbound Transformer</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getOutboundTransformer()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_OutboundTransformer();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getRef <em>Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ref</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getRef()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_Ref();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getResponseTransformer <em>Response Transformer</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Response Transformer</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getResponseTransformer()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_ResponseTransformer();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#isSingleton <em>Singleton</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Singleton</em>'.
	 * @see org.mule.schema.MuleDescriptorType#isSingleton()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_Singleton();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleDescriptorType#getVersion <em>Version</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Version</em>'.
	 * @see org.mule.schema.MuleDescriptorType#getVersion()
	 * @see #getMuleDescriptorType()
	 * @generated
	 */
	EAttribute getMuleDescriptorType_Version();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.MuleEnvironmentPropertiesType <em>Environment Properties Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Environment Properties Type</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType
	 * @generated
	 */
	EClass getMuleEnvironmentPropertiesType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.MuleEnvironmentPropertiesType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getMixed()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.MuleEnvironmentPropertiesType#getThreadingProfile <em>Threading Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Threading Profile</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getThreadingProfile()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getMuleEnvironmentPropertiesType_ThreadingProfile();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleEnvironmentPropertiesType#getPoolingProfile <em>Pooling Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Pooling Profile</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getPoolingProfile()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getMuleEnvironmentPropertiesType_PoolingProfile();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleEnvironmentPropertiesType#getQueueProfile <em>Queue Profile</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Queue Profile</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getQueueProfile()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getMuleEnvironmentPropertiesType_QueueProfile();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleEnvironmentPropertiesType#getPersistenceStrategy <em>Persistence Strategy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Persistence Strategy</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getPersistenceStrategy()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getMuleEnvironmentPropertiesType_PersistenceStrategy();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.MuleEnvironmentPropertiesType#getConnectionStrategy <em>Connection Strategy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Connection Strategy</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getConnectionStrategy()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EReference getMuleEnvironmentPropertiesType_ConnectionStrategy();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#isClientMode <em>Client Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Client Mode</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#isClientMode()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_ClientMode();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#isEmbedded <em>Embedded</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Embedded</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#isEmbedded()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_Embedded();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#isEnableMessageEvents <em>Enable Message Events</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Enable Message Events</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#isEnableMessageEvents()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_EnableMessageEvents();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#getEncoding <em>Encoding</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Encoding</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getEncoding()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_Encoding();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#getModel <em>Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Model</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getModel()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_Model();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#isRecoverableMode <em>Recoverable Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Recoverable Mode</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#isRecoverableMode()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_RecoverableMode();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#isRemoteSync <em>Remote Sync</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Remote Sync</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#isRemoteSync()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_RemoteSync();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#getServerUrl <em>Server Url</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Server Url</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getServerUrl()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_ServerUrl();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#isSynchronous <em>Synchronous</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Synchronous</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#isSynchronous()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_Synchronous();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#getSynchronousEventTimeout <em>Synchronous Event Timeout</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Synchronous Event Timeout</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getSynchronousEventTimeout()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_SynchronousEventTimeout();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#getTransactionTimeout <em>Transaction Timeout</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Transaction Timeout</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getTransactionTimeout()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_TransactionTimeout();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.MuleEnvironmentPropertiesType#getWorkingDirectory <em>Working Directory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Working Directory</em>'.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType#getWorkingDirectory()
	 * @see #getMuleEnvironmentPropertiesType()
	 * @generated
	 */
	EAttribute getMuleEnvironmentPropertiesType_WorkingDirectory();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.OutboundRouterType <em>Outbound Router Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Outbound Router Type</em>'.
	 * @see org.mule.schema.OutboundRouterType
	 * @generated
	 */
	EClass getOutboundRouterType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.OutboundRouterType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.OutboundRouterType#getMixed()
	 * @see #getOutboundRouterType()
	 * @generated
	 */
	EAttribute getOutboundRouterType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.OutboundRouterType#getCatchAllStrategy <em>Catch All Strategy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Catch All Strategy</em>'.
	 * @see org.mule.schema.OutboundRouterType#getCatchAllStrategy()
	 * @see #getOutboundRouterType()
	 * @generated
	 */
	EReference getOutboundRouterType_CatchAllStrategy();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.OutboundRouterType#getRouter <em>Router</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Router</em>'.
	 * @see org.mule.schema.OutboundRouterType#getRouter()
	 * @see #getOutboundRouterType()
	 * @generated
	 */
	EReference getOutboundRouterType_Router();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.OutboundRouterType#isMatchAll <em>Match All</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Match All</em>'.
	 * @see org.mule.schema.OutboundRouterType#isMatchAll()
	 * @see #getOutboundRouterType()
	 * @generated
	 */
	EAttribute getOutboundRouterType_MatchAll();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.PersistenceStrategyType <em>Persistence Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Persistence Strategy Type</em>'.
	 * @see org.mule.schema.PersistenceStrategyType
	 * @generated
	 */
	EClass getPersistenceStrategyType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.PersistenceStrategyType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.PersistenceStrategyType#getMixed()
	 * @see #getPersistenceStrategyType()
	 * @generated
	 */
	EAttribute getPersistenceStrategyType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.PersistenceStrategyType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.PersistenceStrategyType#getProperties()
	 * @see #getPersistenceStrategyType()
	 * @generated
	 */
	EReference getPersistenceStrategyType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.PersistenceStrategyType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.PersistenceStrategyType#getClassName()
	 * @see #getPersistenceStrategyType()
	 * @generated
	 */
	EAttribute getPersistenceStrategyType_ClassName();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.PoolingProfileType <em>Pooling Profile Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Pooling Profile Type</em>'.
	 * @see org.mule.schema.PoolingProfileType
	 * @generated
	 */
	EClass getPoolingProfileType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.PoolingProfileType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.PoolingProfileType#getMixed()
	 * @see #getPoolingProfileType()
	 * @generated
	 */
	EAttribute getPoolingProfileType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.PoolingProfileType#getExhaustedAction <em>Exhausted Action</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Exhausted Action</em>'.
	 * @see org.mule.schema.PoolingProfileType#getExhaustedAction()
	 * @see #getPoolingProfileType()
	 * @generated
	 */
	EAttribute getPoolingProfileType_ExhaustedAction();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.PoolingProfileType#getFactory <em>Factory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Factory</em>'.
	 * @see org.mule.schema.PoolingProfileType#getFactory()
	 * @see #getPoolingProfileType()
	 * @generated
	 */
	EAttribute getPoolingProfileType_Factory();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.PoolingProfileType#getInitialisationPolicy <em>Initialisation Policy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Initialisation Policy</em>'.
	 * @see org.mule.schema.PoolingProfileType#getInitialisationPolicy()
	 * @see #getPoolingProfileType()
	 * @generated
	 */
	EAttribute getPoolingProfileType_InitialisationPolicy();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.PoolingProfileType#getMaxActive <em>Max Active</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Active</em>'.
	 * @see org.mule.schema.PoolingProfileType#getMaxActive()
	 * @see #getPoolingProfileType()
	 * @generated
	 */
	EAttribute getPoolingProfileType_MaxActive();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.PoolingProfileType#getMaxIdle <em>Max Idle</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Idle</em>'.
	 * @see org.mule.schema.PoolingProfileType#getMaxIdle()
	 * @see #getPoolingProfileType()
	 * @generated
	 */
	EAttribute getPoolingProfileType_MaxIdle();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.PoolingProfileType#getMaxWait <em>Max Wait</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Wait</em>'.
	 * @see org.mule.schema.PoolingProfileType#getMaxWait()
	 * @see #getPoolingProfileType()
	 * @generated
	 */
	EAttribute getPoolingProfileType_MaxWait();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.PropertiesType <em>Properties Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Properties Type</em>'.
	 * @see org.mule.schema.PropertiesType
	 * @generated
	 */
	EClass getPropertiesType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.PropertiesType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.PropertiesType#getMixed()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EAttribute getPropertiesType_Mixed();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.PropertiesType#getGroup <em>Group</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Group</em>'.
	 * @see org.mule.schema.PropertiesType#getGroup()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EAttribute getPropertiesType_Group();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.PropertiesType#getProperty <em>Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Property</em>'.
	 * @see org.mule.schema.PropertiesType#getProperty()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EReference getPropertiesType_Property();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.PropertiesType#getFactoryProperty <em>Factory Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Factory Property</em>'.
	 * @see org.mule.schema.PropertiesType#getFactoryProperty()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EReference getPropertiesType_FactoryProperty();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.PropertiesType#getContainerProperty <em>Container Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Container Property</em>'.
	 * @see org.mule.schema.PropertiesType#getContainerProperty()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EReference getPropertiesType_ContainerProperty();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.PropertiesType#getSystemProperty <em>System Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>System Property</em>'.
	 * @see org.mule.schema.PropertiesType#getSystemProperty()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EReference getPropertiesType_SystemProperty();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.PropertiesType#getMap <em>Map</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Map</em>'.
	 * @see org.mule.schema.PropertiesType#getMap()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EReference getPropertiesType_Map();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.PropertiesType#getList <em>List</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>List</em>'.
	 * @see org.mule.schema.PropertiesType#getList()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EReference getPropertiesType_List();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.PropertiesType#getFileProperties <em>File Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>File Properties</em>'.
	 * @see org.mule.schema.PropertiesType#getFileProperties()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EReference getPropertiesType_FileProperties();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.PropertiesType#getTextProperty <em>Text Property</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Text Property</em>'.
	 * @see org.mule.schema.PropertiesType#getTextProperty()
	 * @see #getPropertiesType()
	 * @generated
	 */
	EReference getPropertiesType_TextProperty();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.PropertyType <em>Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Property Type</em>'.
	 * @see org.mule.schema.PropertyType
	 * @generated
	 */
	EClass getPropertyType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.PropertyType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.PropertyType#getMixed()
	 * @see #getPropertyType()
	 * @generated
	 */
	EAttribute getPropertyType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.PropertyType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.PropertyType#getName()
	 * @see #getPropertyType()
	 * @generated
	 */
	EAttribute getPropertyType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.PropertyType#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.mule.schema.PropertyType#getValue()
	 * @see #getPropertyType()
	 * @generated
	 */
	EAttribute getPropertyType_Value();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.QueueProfileType <em>Queue Profile Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Queue Profile Type</em>'.
	 * @see org.mule.schema.QueueProfileType
	 * @generated
	 */
	EClass getQueueProfileType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.QueueProfileType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.QueueProfileType#getMixed()
	 * @see #getQueueProfileType()
	 * @generated
	 */
	EAttribute getQueueProfileType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.QueueProfileType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.QueueProfileType#getProperties()
	 * @see #getQueueProfileType()
	 * @generated
	 */
	EReference getQueueProfileType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.QueueProfileType#getMaxOutstandingMessages <em>Max Outstanding Messages</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Outstanding Messages</em>'.
	 * @see org.mule.schema.QueueProfileType#getMaxOutstandingMessages()
	 * @see #getQueueProfileType()
	 * @generated
	 */
	EAttribute getQueueProfileType_MaxOutstandingMessages();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.QueueProfileType#isPersistent <em>Persistent</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Persistent</em>'.
	 * @see org.mule.schema.QueueProfileType#isPersistent()
	 * @see #getQueueProfileType()
	 * @generated
	 */
	EAttribute getQueueProfileType_Persistent();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ReplyToType <em>Reply To Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Reply To Type</em>'.
	 * @see org.mule.schema.ReplyToType
	 * @generated
	 */
	EClass getReplyToType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ReplyToType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ReplyToType#getMixed()
	 * @see #getReplyToType()
	 * @generated
	 */
	EAttribute getReplyToType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ReplyToType#getAddress <em>Address</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Address</em>'.
	 * @see org.mule.schema.ReplyToType#getAddress()
	 * @see #getReplyToType()
	 * @generated
	 */
	EAttribute getReplyToType_Address();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ResponseRouterType <em>Response Router Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Response Router Type</em>'.
	 * @see org.mule.schema.ResponseRouterType
	 * @generated
	 */
	EClass getResponseRouterType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ResponseRouterType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ResponseRouterType#getMixed()
	 * @see #getResponseRouterType()
	 * @generated
	 */
	EAttribute getResponseRouterType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ResponseRouterType#getEndpoint <em>Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Endpoint</em>'.
	 * @see org.mule.schema.ResponseRouterType#getEndpoint()
	 * @see #getResponseRouterType()
	 * @generated
	 */
	EReference getResponseRouterType_Endpoint();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ResponseRouterType#getGlobalEndpoint <em>Global Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Global Endpoint</em>'.
	 * @see org.mule.schema.ResponseRouterType#getGlobalEndpoint()
	 * @see #getResponseRouterType()
	 * @generated
	 */
	EReference getResponseRouterType_GlobalEndpoint();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.ResponseRouterType#getRouter <em>Router</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Router</em>'.
	 * @see org.mule.schema.ResponseRouterType#getRouter()
	 * @see #getResponseRouterType()
	 * @generated
	 */
	EReference getResponseRouterType_Router();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ResponseRouterType#getTimeout <em>Timeout</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Timeout</em>'.
	 * @see org.mule.schema.ResponseRouterType#getTimeout()
	 * @see #getResponseRouterType()
	 * @generated
	 */
	EAttribute getResponseRouterType_Timeout();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.RightFilterType <em>Right Filter Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Right Filter Type</em>'.
	 * @see org.mule.schema.RightFilterType
	 * @generated
	 */
	EClass getRightFilterType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.RightFilterType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.RightFilterType#getMixed()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EAttribute getRightFilterType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.RightFilterType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.RightFilterType#getProperties()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EReference getRightFilterType_Properties();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.RightFilterType#getFilter <em>Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Filter</em>'.
	 * @see org.mule.schema.RightFilterType#getFilter()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EReference getRightFilterType_Filter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.RightFilterType#getLeftFilter <em>Left Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Left Filter</em>'.
	 * @see org.mule.schema.RightFilterType#getLeftFilter()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EReference getRightFilterType_LeftFilter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.RightFilterType#getRightFilter <em>Right Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Right Filter</em>'.
	 * @see org.mule.schema.RightFilterType#getRightFilter()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EReference getRightFilterType_RightFilter();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.RightFilterType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.RightFilterType#getClassName()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EAttribute getRightFilterType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.RightFilterType#getConfigFile <em>Config File</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Config File</em>'.
	 * @see org.mule.schema.RightFilterType#getConfigFile()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EAttribute getRightFilterType_ConfigFile();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.RightFilterType#getExpectedType <em>Expected Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expected Type</em>'.
	 * @see org.mule.schema.RightFilterType#getExpectedType()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EAttribute getRightFilterType_ExpectedType();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.RightFilterType#getExpression <em>Expression</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Expression</em>'.
	 * @see org.mule.schema.RightFilterType#getExpression()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EAttribute getRightFilterType_Expression();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.RightFilterType#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see org.mule.schema.RightFilterType#getPath()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EAttribute getRightFilterType_Path();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.RightFilterType#getPattern <em>Pattern</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Pattern</em>'.
	 * @see org.mule.schema.RightFilterType#getPattern()
	 * @see #getRightFilterType()
	 * @generated
	 */
	EAttribute getRightFilterType_Pattern();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.RouterType <em>Router Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Router Type</em>'.
	 * @see org.mule.schema.RouterType
	 * @generated
	 */
	EClass getRouterType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.RouterType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.RouterType#getMixed()
	 * @see #getRouterType()
	 * @generated
	 */
	EAttribute getRouterType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.RouterType#getEndpoint <em>Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Endpoint</em>'.
	 * @see org.mule.schema.RouterType#getEndpoint()
	 * @see #getRouterType()
	 * @generated
	 */
	EReference getRouterType_Endpoint();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.RouterType#getGlobalEndpoint <em>Global Endpoint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Global Endpoint</em>'.
	 * @see org.mule.schema.RouterType#getGlobalEndpoint()
	 * @see #getRouterType()
	 * @generated
	 */
	EReference getRouterType_GlobalEndpoint();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.RouterType#getReplyTo <em>Reply To</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Reply To</em>'.
	 * @see org.mule.schema.RouterType#getReplyTo()
	 * @see #getRouterType()
	 * @generated
	 */
	EReference getRouterType_ReplyTo();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.RouterType#getTransaction <em>Transaction</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Transaction</em>'.
	 * @see org.mule.schema.RouterType#getTransaction()
	 * @see #getRouterType()
	 * @generated
	 */
	EReference getRouterType_Transaction();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.RouterType#getFilter <em>Filter</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Filter</em>'.
	 * @see org.mule.schema.RouterType#getFilter()
	 * @see #getRouterType()
	 * @generated
	 */
	EReference getRouterType_Filter();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.RouterType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.RouterType#getProperties()
	 * @see #getRouterType()
	 * @generated
	 */
	EReference getRouterType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.RouterType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.RouterType#getClassName()
	 * @see #getRouterType()
	 * @generated
	 */
	EAttribute getRouterType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.RouterType#getEnableCorrelation <em>Enable Correlation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Enable Correlation</em>'.
	 * @see org.mule.schema.RouterType#getEnableCorrelation()
	 * @see #getRouterType()
	 * @generated
	 */
	EAttribute getRouterType_EnableCorrelation();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.RouterType#getPropertyExtractor <em>Property Extractor</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Property Extractor</em>'.
	 * @see org.mule.schema.RouterType#getPropertyExtractor()
	 * @see #getRouterType()
	 * @generated
	 */
	EAttribute getRouterType_PropertyExtractor();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.SecurityFilterType <em>Security Filter Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Security Filter Type</em>'.
	 * @see org.mule.schema.SecurityFilterType
	 * @generated
	 */
	EClass getSecurityFilterType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.SecurityFilterType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.SecurityFilterType#getMixed()
	 * @see #getSecurityFilterType()
	 * @generated
	 */
	EAttribute getSecurityFilterType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.SecurityFilterType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.SecurityFilterType#getProperties()
	 * @see #getSecurityFilterType()
	 * @generated
	 */
	EReference getSecurityFilterType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SecurityFilterType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.SecurityFilterType#getClassName()
	 * @see #getSecurityFilterType()
	 * @generated
	 */
	EAttribute getSecurityFilterType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SecurityFilterType#getUseProviders <em>Use Providers</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Use Providers</em>'.
	 * @see org.mule.schema.SecurityFilterType#getUseProviders()
	 * @see #getSecurityFilterType()
	 * @generated
	 */
	EAttribute getSecurityFilterType_UseProviders();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.SecurityManagerType <em>Security Manager Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Security Manager Type</em>'.
	 * @see org.mule.schema.SecurityManagerType
	 * @generated
	 */
	EClass getSecurityManagerType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.SecurityManagerType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.SecurityManagerType#getMixed()
	 * @see #getSecurityManagerType()
	 * @generated
	 */
	EAttribute getSecurityManagerType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.SecurityManagerType#getSecurityProvider <em>Security Provider</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Security Provider</em>'.
	 * @see org.mule.schema.SecurityManagerType#getSecurityProvider()
	 * @see #getSecurityManagerType()
	 * @generated
	 */
	EReference getSecurityManagerType_SecurityProvider();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.SecurityManagerType#getEncryptionStrategy <em>Encryption Strategy</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Encryption Strategy</em>'.
	 * @see org.mule.schema.SecurityManagerType#getEncryptionStrategy()
	 * @see #getSecurityManagerType()
	 * @generated
	 */
	EReference getSecurityManagerType_EncryptionStrategy();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SecurityManagerType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.SecurityManagerType#getClassName()
	 * @see #getSecurityManagerType()
	 * @generated
	 */
	EAttribute getSecurityManagerType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SecurityManagerType#getRef <em>Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ref</em>'.
	 * @see org.mule.schema.SecurityManagerType#getRef()
	 * @see #getSecurityManagerType()
	 * @generated
	 */
	EAttribute getSecurityManagerType_Ref();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.SecurityProviderType <em>Security Provider Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Security Provider Type</em>'.
	 * @see org.mule.schema.SecurityProviderType
	 * @generated
	 */
	EClass getSecurityProviderType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.SecurityProviderType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.SecurityProviderType#getMixed()
	 * @see #getSecurityProviderType()
	 * @generated
	 */
	EAttribute getSecurityProviderType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.SecurityProviderType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.SecurityProviderType#getProperties()
	 * @see #getSecurityProviderType()
	 * @generated
	 */
	EReference getSecurityProviderType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SecurityProviderType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.SecurityProviderType#getClassName()
	 * @see #getSecurityProviderType()
	 * @generated
	 */
	EAttribute getSecurityProviderType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SecurityProviderType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.SecurityProviderType#getName()
	 * @see #getSecurityProviderType()
	 * @generated
	 */
	EAttribute getSecurityProviderType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SecurityProviderType#getRef <em>Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ref</em>'.
	 * @see org.mule.schema.SecurityProviderType#getRef()
	 * @see #getSecurityProviderType()
	 * @generated
	 */
	EAttribute getSecurityProviderType_Ref();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.SystemEntryType <em>System Entry Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>System Entry Type</em>'.
	 * @see org.mule.schema.SystemEntryType
	 * @generated
	 */
	EClass getSystemEntryType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.SystemEntryType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.SystemEntryType#getMixed()
	 * @see #getSystemEntryType()
	 * @generated
	 */
	EAttribute getSystemEntryType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SystemEntryType#getDefaultValue <em>Default Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Default Value</em>'.
	 * @see org.mule.schema.SystemEntryType#getDefaultValue()
	 * @see #getSystemEntryType()
	 * @generated
	 */
	EAttribute getSystemEntryType_DefaultValue();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SystemEntryType#getKey <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see org.mule.schema.SystemEntryType#getKey()
	 * @see #getSystemEntryType()
	 * @generated
	 */
	EAttribute getSystemEntryType_Key();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.SystemPropertyType <em>System Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>System Property Type</em>'.
	 * @see org.mule.schema.SystemPropertyType
	 * @generated
	 */
	EClass getSystemPropertyType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.SystemPropertyType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.SystemPropertyType#getMixed()
	 * @see #getSystemPropertyType()
	 * @generated
	 */
	EAttribute getSystemPropertyType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SystemPropertyType#getDefaultValue <em>Default Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Default Value</em>'.
	 * @see org.mule.schema.SystemPropertyType#getDefaultValue()
	 * @see #getSystemPropertyType()
	 * @generated
	 */
	EAttribute getSystemPropertyType_DefaultValue();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SystemPropertyType#getKey <em>Key</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Key</em>'.
	 * @see org.mule.schema.SystemPropertyType#getKey()
	 * @see #getSystemPropertyType()
	 * @generated
	 */
	EAttribute getSystemPropertyType_Key();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.SystemPropertyType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.SystemPropertyType#getName()
	 * @see #getSystemPropertyType()
	 * @generated
	 */
	EAttribute getSystemPropertyType_Name();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.TextPropertyType <em>Text Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Text Property Type</em>'.
	 * @see org.mule.schema.TextPropertyType
	 * @generated
	 */
	EClass getTextPropertyType();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TextPropertyType#getValue <em>Value</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Value</em>'.
	 * @see org.mule.schema.TextPropertyType#getValue()
	 * @see #getTextPropertyType()
	 * @generated
	 */
	EAttribute getTextPropertyType_Value();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TextPropertyType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.TextPropertyType#getName()
	 * @see #getTextPropertyType()
	 * @generated
	 */
	EAttribute getTextPropertyType_Name();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.ThreadingProfileType <em>Threading Profile Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Threading Profile Type</em>'.
	 * @see org.mule.schema.ThreadingProfileType
	 * @generated
	 */
	EClass getThreadingProfileType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.ThreadingProfileType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.ThreadingProfileType#getMixed()
	 * @see #getThreadingProfileType()
	 * @generated
	 */
	EAttribute getThreadingProfileType_Mixed();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ThreadingProfileType#isDoThreading <em>Do Threading</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Do Threading</em>'.
	 * @see org.mule.schema.ThreadingProfileType#isDoThreading()
	 * @see #getThreadingProfileType()
	 * @generated
	 */
	EAttribute getThreadingProfileType_DoThreading();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ThreadingProfileType#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see org.mule.schema.ThreadingProfileType#getId()
	 * @see #getThreadingProfileType()
	 * @generated
	 */
	EAttribute getThreadingProfileType_Id();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ThreadingProfileType#getMaxBufferSize <em>Max Buffer Size</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Buffer Size</em>'.
	 * @see org.mule.schema.ThreadingProfileType#getMaxBufferSize()
	 * @see #getThreadingProfileType()
	 * @generated
	 */
	EAttribute getThreadingProfileType_MaxBufferSize();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ThreadingProfileType#getMaxThreadsActive <em>Max Threads Active</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Threads Active</em>'.
	 * @see org.mule.schema.ThreadingProfileType#getMaxThreadsActive()
	 * @see #getThreadingProfileType()
	 * @generated
	 */
	EAttribute getThreadingProfileType_MaxThreadsActive();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ThreadingProfileType#getMaxThreadsIdle <em>Max Threads Idle</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Threads Idle</em>'.
	 * @see org.mule.schema.ThreadingProfileType#getMaxThreadsIdle()
	 * @see #getThreadingProfileType()
	 * @generated
	 */
	EAttribute getThreadingProfileType_MaxThreadsIdle();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ThreadingProfileType#getPoolExhaustedAction <em>Pool Exhausted Action</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Pool Exhausted Action</em>'.
	 * @see org.mule.schema.ThreadingProfileType#getPoolExhaustedAction()
	 * @see #getThreadingProfileType()
	 * @generated
	 */
	EAttribute getThreadingProfileType_PoolExhaustedAction();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ThreadingProfileType#getThreadTTL <em>Thread TTL</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Thread TTL</em>'.
	 * @see org.mule.schema.ThreadingProfileType#getThreadTTL()
	 * @see #getThreadingProfileType()
	 * @generated
	 */
	EAttribute getThreadingProfileType_ThreadTTL();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.ThreadingProfileType#getThreadWaitTimeout <em>Thread Wait Timeout</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Thread Wait Timeout</em>'.
	 * @see org.mule.schema.ThreadingProfileType#getThreadWaitTimeout()
	 * @see #getThreadingProfileType()
	 * @generated
	 */
	EAttribute getThreadingProfileType_ThreadWaitTimeout();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.TransactionManagerType <em>Transaction Manager Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Transaction Manager Type</em>'.
	 * @see org.mule.schema.TransactionManagerType
	 * @generated
	 */
	EClass getTransactionManagerType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.TransactionManagerType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.TransactionManagerType#getMixed()
	 * @see #getTransactionManagerType()
	 * @generated
	 */
	EAttribute getTransactionManagerType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.TransactionManagerType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.TransactionManagerType#getProperties()
	 * @see #getTransactionManagerType()
	 * @generated
	 */
	EReference getTransactionManagerType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransactionManagerType#getFactory <em>Factory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Factory</em>'.
	 * @see org.mule.schema.TransactionManagerType#getFactory()
	 * @see #getTransactionManagerType()
	 * @generated
	 */
	EAttribute getTransactionManagerType_Factory();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransactionManagerType#getRef <em>Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ref</em>'.
	 * @see org.mule.schema.TransactionManagerType#getRef()
	 * @see #getTransactionManagerType()
	 * @generated
	 */
	EAttribute getTransactionManagerType_Ref();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.TransactionType <em>Transaction Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Transaction Type</em>'.
	 * @see org.mule.schema.TransactionType
	 * @generated
	 */
	EClass getTransactionType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.TransactionType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.TransactionType#getMixed()
	 * @see #getTransactionType()
	 * @generated
	 */
	EAttribute getTransactionType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.TransactionType#getConstraint <em>Constraint</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Constraint</em>'.
	 * @see org.mule.schema.TransactionType#getConstraint()
	 * @see #getTransactionType()
	 * @generated
	 */
	EReference getTransactionType_Constraint();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransactionType#getAction <em>Action</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Action</em>'.
	 * @see org.mule.schema.TransactionType#getAction()
	 * @see #getTransactionType()
	 * @generated
	 */
	EAttribute getTransactionType_Action();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransactionType#getFactory <em>Factory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Factory</em>'.
	 * @see org.mule.schema.TransactionType#getFactory()
	 * @see #getTransactionType()
	 * @generated
	 */
	EAttribute getTransactionType_Factory();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransactionType#getTimeout <em>Timeout</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Timeout</em>'.
	 * @see org.mule.schema.TransactionType#getTimeout()
	 * @see #getTransactionType()
	 * @generated
	 */
	EAttribute getTransactionType_Timeout();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.TransformersType <em>Transformers Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Transformers Type</em>'.
	 * @see org.mule.schema.TransformersType
	 * @generated
	 */
	EClass getTransformersType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.TransformersType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.TransformersType#getMixed()
	 * @see #getTransformersType()
	 * @generated
	 */
	EAttribute getTransformersType_Mixed();

	/**
	 * Returns the meta object for the containment reference list '{@link org.mule.schema.TransformersType#getTransformer <em>Transformer</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Transformer</em>'.
	 * @see org.mule.schema.TransformersType#getTransformer()
	 * @see #getTransformersType()
	 * @generated
	 */
	EReference getTransformersType_Transformer();

	/**
	 * Returns the meta object for class '{@link org.mule.schema.TransformerType <em>Transformer Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Transformer Type</em>'.
	 * @see org.mule.schema.TransformerType
	 * @generated
	 */
	EClass getTransformerType();

	/**
	 * Returns the meta object for the attribute list '{@link org.mule.schema.TransformerType#getMixed <em>Mixed</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Mixed</em>'.
	 * @see org.mule.schema.TransformerType#getMixed()
	 * @see #getTransformerType()
	 * @generated
	 */
	EAttribute getTransformerType_Mixed();

	/**
	 * Returns the meta object for the containment reference '{@link org.mule.schema.TransformerType#getProperties <em>Properties</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Properties</em>'.
	 * @see org.mule.schema.TransformerType#getProperties()
	 * @see #getTransformerType()
	 * @generated
	 */
	EReference getTransformerType_Properties();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransformerType#getClassName <em>Class Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Class Name</em>'.
	 * @see org.mule.schema.TransformerType#getClassName()
	 * @see #getTransformerType()
	 * @generated
	 */
	EAttribute getTransformerType_ClassName();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransformerType#isIgnoreBadInput <em>Ignore Bad Input</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ignore Bad Input</em>'.
	 * @see org.mule.schema.TransformerType#isIgnoreBadInput()
	 * @see #getTransformerType()
	 * @generated
	 */
	EAttribute getTransformerType_IgnoreBadInput();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransformerType#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see org.mule.schema.TransformerType#getName()
	 * @see #getTransformerType()
	 * @generated
	 */
	EAttribute getTransformerType_Name();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransformerType#getRef <em>Ref</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ref</em>'.
	 * @see org.mule.schema.TransformerType#getRef()
	 * @see #getTransformerType()
	 * @generated
	 */
	EAttribute getTransformerType_Ref();

	/**
	 * Returns the meta object for the attribute '{@link org.mule.schema.TransformerType#getReturnClass <em>Return Class</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Return Class</em>'.
	 * @see org.mule.schema.TransformerType#getReturnClass()
	 * @see #getTransformerType()
	 * @generated
	 */
	EAttribute getTransformerType_ReturnClass();

	/**
	 * Returns the meta object for enum '{@link org.mule.schema.ActionType <em>Action Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Action Type</em>'.
	 * @see org.mule.schema.ActionType
	 * @generated
	 */
	EEnum getActionType();

	/**
	 * Returns the meta object for enum '{@link org.mule.schema.CreateConnectorType <em>Create Connector Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Create Connector Type</em>'.
	 * @see org.mule.schema.CreateConnectorType
	 * @generated
	 */
	EEnum getCreateConnectorType();

	/**
	 * Returns the meta object for enum '{@link org.mule.schema.EnableCorrelationType <em>Enable Correlation Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Enable Correlation Type</em>'.
	 * @see org.mule.schema.EnableCorrelationType
	 * @generated
	 */
	EEnum getEnableCorrelationType();

	/**
	 * Returns the meta object for enum '{@link org.mule.schema.ExhaustedActionType <em>Exhausted Action Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Exhausted Action Type</em>'.
	 * @see org.mule.schema.ExhaustedActionType
	 * @generated
	 */
	EEnum getExhaustedActionType();

	/**
	 * Returns the meta object for enum '{@link org.mule.schema.IdType <em>Id Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Id Type</em>'.
	 * @see org.mule.schema.IdType
	 * @generated
	 */
	EEnum getIdType();

	/**
	 * Returns the meta object for enum '{@link org.mule.schema.InitialisationPolicyType <em>Initialisation Policy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Initialisation Policy Type</em>'.
	 * @see org.mule.schema.InitialisationPolicyType
	 * @generated
	 */
	EEnum getInitialisationPolicyType();

	/**
	 * Returns the meta object for enum '{@link org.mule.schema.InitialStateType <em>Initial State Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Initial State Type</em>'.
	 * @see org.mule.schema.InitialStateType
	 * @generated
	 */
	EEnum getInitialStateType();

	/**
	 * Returns the meta object for enum '{@link org.mule.schema.PoolExhaustedActionType <em>Pool Exhausted Action Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Pool Exhausted Action Type</em>'.
	 * @see org.mule.schema.PoolExhaustedActionType
	 * @generated
	 */
	EEnum getPoolExhaustedActionType();

	/**
	 * Returns the meta object for enum '{@link org.mule.schema.TypeType1 <em>Type Type1</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Type Type1</em>'.
	 * @see org.mule.schema.TypeType1
	 * @generated
	 */
	EEnum getTypeType1();

	/**
	 * Returns the meta object for data type '{@link org.mule.schema.ActionType <em>Action Type Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Action Type Object</em>'.
	 * @see org.mule.schema.ActionType
	 * @model instanceClass="org.mule.schema.ActionType"
	 *        extendedMetaData="name='action_._type:Object' baseType='action_._type'" 
	 * @generated
	 */
	EDataType getActionTypeObject();

	/**
	 * Returns the meta object for data type '{@link org.mule.schema.CreateConnectorType <em>Create Connector Type Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Create Connector Type Object</em>'.
	 * @see org.mule.schema.CreateConnectorType
	 * @model instanceClass="org.mule.schema.CreateConnectorType"
	 *        extendedMetaData="name='createConnector_._type:Object' baseType='createConnector_._type'" 
	 * @generated
	 */
	EDataType getCreateConnectorTypeObject();

	/**
	 * Returns the meta object for data type '{@link org.mule.schema.EnableCorrelationType <em>Enable Correlation Type Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Enable Correlation Type Object</em>'.
	 * @see org.mule.schema.EnableCorrelationType
	 * @model instanceClass="org.mule.schema.EnableCorrelationType"
	 *        extendedMetaData="name='enableCorrelation_._type:Object' baseType='enableCorrelation_._type'" 
	 * @generated
	 */
	EDataType getEnableCorrelationTypeObject();

	/**
	 * Returns the meta object for data type '{@link org.mule.schema.ExhaustedActionType <em>Exhausted Action Type Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Exhausted Action Type Object</em>'.
	 * @see org.mule.schema.ExhaustedActionType
	 * @model instanceClass="org.mule.schema.ExhaustedActionType"
	 *        extendedMetaData="name='exhaustedAction_._type:Object' baseType='exhaustedAction_._type'" 
	 * @generated
	 */
	EDataType getExhaustedActionTypeObject();

	/**
	 * Returns the meta object for data type '{@link org.mule.schema.IdType <em>Id Type Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Id Type Object</em>'.
	 * @see org.mule.schema.IdType
	 * @model instanceClass="org.mule.schema.IdType"
	 *        extendedMetaData="name='id_._type:Object' baseType='id_._type'" 
	 * @generated
	 */
	EDataType getIdTypeObject();

	/**
	 * Returns the meta object for data type '{@link org.mule.schema.InitialisationPolicyType <em>Initialisation Policy Type Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Initialisation Policy Type Object</em>'.
	 * @see org.mule.schema.InitialisationPolicyType
	 * @model instanceClass="org.mule.schema.InitialisationPolicyType"
	 *        extendedMetaData="name='initialisationPolicy_._type:Object' baseType='initialisationPolicy_._type'" 
	 * @generated
	 */
	EDataType getInitialisationPolicyTypeObject();

	/**
	 * Returns the meta object for data type '{@link org.mule.schema.InitialStateType <em>Initial State Type Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Initial State Type Object</em>'.
	 * @see org.mule.schema.InitialStateType
	 * @model instanceClass="org.mule.schema.InitialStateType"
	 *        extendedMetaData="name='initialState_._type:Object' baseType='initialState_._type'" 
	 * @generated
	 */
	EDataType getInitialStateTypeObject();

	/**
	 * Returns the meta object for data type '{@link org.mule.schema.PoolExhaustedActionType <em>Pool Exhausted Action Type Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Pool Exhausted Action Type Object</em>'.
	 * @see org.mule.schema.PoolExhaustedActionType
	 * @model instanceClass="org.mule.schema.PoolExhaustedActionType"
	 *        extendedMetaData="name='poolExhaustedAction_._type:Object' baseType='poolExhaustedAction_._type'" 
	 * @generated
	 */
	EDataType getPoolExhaustedActionTypeObject();

	/**
	 * Returns the meta object for data type '{@link java.lang.String <em>Type Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Type Type</em>'.
	 * @see java.lang.String
	 * @model instanceClass="java.lang.String"
	 *        extendedMetaData="name='type_._type' baseType='http://www.eclipse.org/emf/2003/XMLType#NMTOKEN' enumeration='seda direct pipeline jms jms-clustered jcyclone custom'" 
	 * @generated
	 */
	EDataType getTypeType();

	/**
	 * Returns the meta object for data type '{@link org.mule.schema.TypeType1 <em>Type Type Object</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Type Type Object</em>'.
	 * @see org.mule.schema.TypeType1
	 * @model instanceClass="org.mule.schema.TypeType1"
	 *        extendedMetaData="name='type_._1_._type:Object' baseType='type_._1_._type'" 
	 * @generated
	 */
	EDataType getTypeTypeObject();

	/**
	 * Returns the meta object for data type '{@link java.lang.String <em>Version Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for data type '<em>Version Type</em>'.
	 * @see java.lang.String
	 * @model instanceClass="java.lang.String"
	 *        extendedMetaData="name='version_._type' baseType='http://www.eclipse.org/emf/2003/XMLType#NMTOKEN' enumeration='1.0'" 
	 * @generated
	 */
	EDataType getVersionType();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	MuleFactory getMuleFactory();

} //MulePackage
