/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.impl;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EValidator;

import org.eclipse.emf.ecore.impl.EPackageImpl;

import org.eclipse.emf.ecore.xml.type.XMLTypePackage;

import org.eclipse.emf.ecore.xml.type.impl.XMLTypePackageImpl;

import org.mule.schema.ActionType;
import org.mule.schema.AgentType;
import org.mule.schema.AgentsType;
import org.mule.schema.CatchAllStrategyType;
import org.mule.schema.ComponentFactoryType;
import org.mule.schema.ComponentLifecycleAdapterFactoryType;
import org.mule.schema.ComponentPoolFactoryType;
import org.mule.schema.ConnectionStrategyType;
import org.mule.schema.ConnectorType;
import org.mule.schema.ConstraintType;
import org.mule.schema.ContainerContextType;
import org.mule.schema.ContainerEntryType;
import org.mule.schema.ContainerPropertyType;
import org.mule.schema.CreateConnectorType;
import org.mule.schema.DocumentRoot;
import org.mule.schema.EnableCorrelationType;
import org.mule.schema.EncryptionStrategyType;
import org.mule.schema.EndpointIdentifierType;
import org.mule.schema.EndpointIdentifiersType;
import org.mule.schema.EndpointType;
import org.mule.schema.EntryPointResolverType;
import org.mule.schema.EntryType;
import org.mule.schema.EnvironmentPropertiesType;
import org.mule.schema.ExceptionStrategyType;
import org.mule.schema.ExhaustedActionType;
import org.mule.schema.FactoryEntryType;
import org.mule.schema.FactoryPropertyType;
import org.mule.schema.FilePropertiesType;
import org.mule.schema.FilterType;
import org.mule.schema.GlobalEndpointType;
import org.mule.schema.GlobalEndpointsType;
import org.mule.schema.IdType;
import org.mule.schema.InboundRouterType;
import org.mule.schema.InitialStateType;
import org.mule.schema.InitialisationPolicyType;
import org.mule.schema.InterceptorStackType;
import org.mule.schema.InterceptorType;
import org.mule.schema.LeftFilterType;
import org.mule.schema.ListType;
import org.mule.schema.MapType;
import org.mule.schema.ModelType;
import org.mule.schema.MuleConfigurationType;
import org.mule.schema.MuleDescriptorType;
import org.mule.schema.MuleEnvironmentPropertiesType;
import org.mule.schema.MuleFactory;
import org.mule.schema.MulePackage;
import org.mule.schema.OutboundRouterType;
import org.mule.schema.PersistenceStrategyType;
import org.mule.schema.PoolExhaustedActionType;
import org.mule.schema.PoolingProfileType;
import org.mule.schema.PropertiesType;
import org.mule.schema.PropertyType;
import org.mule.schema.QueueProfileType;
import org.mule.schema.ReplyToType;
import org.mule.schema.ResponseRouterType;
import org.mule.schema.RightFilterType;
import org.mule.schema.RouterType;
import org.mule.schema.SecurityFilterType;
import org.mule.schema.SecurityManagerType;
import org.mule.schema.SecurityProviderType;
import org.mule.schema.SystemEntryType;
import org.mule.schema.SystemPropertyType;
import org.mule.schema.TextPropertyType;
import org.mule.schema.ThreadingProfileType;
import org.mule.schema.TransactionManagerType;
import org.mule.schema.TransactionType;
import org.mule.schema.TransformerType;
import org.mule.schema.TransformersType;
import org.mule.schema.TypeType1;

import org.mule.schema.util.MuleValidator;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class MulePackageImpl extends EPackageImpl implements MulePackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass agentsTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass agentTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass catchAllStrategyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass componentFactoryTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass componentLifecycleAdapterFactoryTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass componentPoolFactoryTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass connectionStrategyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass connectorTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass constraintTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass containerContextTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass containerEntryTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass containerPropertyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass documentRootEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass encryptionStrategyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass endpointIdentifiersTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass endpointIdentifierTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass endpointTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass entryPointResolverTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass entryTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass environmentPropertiesTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass exceptionStrategyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass factoryEntryTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass factoryPropertyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass filePropertiesTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass filterTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass globalEndpointsTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass globalEndpointTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass inboundRouterTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass interceptorStackTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass interceptorTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass leftFilterTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass listTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass mapTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass modelTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass muleConfigurationTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass muleDescriptorTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass muleEnvironmentPropertiesTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass outboundRouterTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass persistenceStrategyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass poolingProfileTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass propertiesTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass propertyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass queueProfileTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass replyToTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass responseRouterTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass rightFilterTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass routerTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass securityFilterTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass securityManagerTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass securityProviderTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass systemEntryTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass systemPropertyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass textPropertyTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass threadingProfileTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass transactionManagerTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass transactionTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass transformersTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass transformerTypeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum actionTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum createConnectorTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum enableCorrelationTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum exhaustedActionTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum idTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum initialisationPolicyTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum initialStateTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum poolExhaustedActionTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum typeType1EEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType actionTypeObjectEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType createConnectorTypeObjectEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType enableCorrelationTypeObjectEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType exhaustedActionTypeObjectEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType idTypeObjectEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType initialisationPolicyTypeObjectEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType initialStateTypeObjectEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType poolExhaustedActionTypeObjectEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType typeTypeEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType typeTypeObjectEDataType = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EDataType versionTypeEDataType = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see org.mule.schema.MulePackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private MulePackageImpl() {
		super(eNS_URI, MuleFactory.eINSTANCE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this
	 * model, and for any others upon which it depends.  Simple
	 * dependencies are satisfied by calling this method on all
	 * dependent packages before doing anything else.  This method drives
	 * initialization for interdependent packages directly, in parallel
	 * with this package, itself.
	 * <p>Of this package and its interdependencies, all packages which
	 * have not yet been registered by their URI values are first created
	 * and registered.  The packages are then initialized in two steps:
	 * meta-model objects for all of the packages are created before any
	 * are initialized, since one package's meta-model objects may refer to
	 * those of another.
	 * <p>Invocation of this method will not affect any packages that have
	 * already been initialized.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static MulePackage init() {
		if (isInited) return (MulePackage)EPackage.Registry.INSTANCE.getEPackage(MulePackage.eNS_URI);

		// Obtain or create and register package
		MulePackageImpl theMulePackage = (MulePackageImpl)(EPackage.Registry.INSTANCE.getEPackage(eNS_URI) instanceof MulePackageImpl ? EPackage.Registry.INSTANCE.getEPackage(eNS_URI) : new MulePackageImpl());

		isInited = true;

		// Initialize simple dependencies
		XMLTypePackageImpl.init();

		// Create package meta-data objects
		theMulePackage.createPackageContents();

		// Initialize created meta-data
		theMulePackage.initializePackageContents();

		// Register package validator
		EValidator.Registry.INSTANCE.put
			(theMulePackage, 
			 new EValidator.Descriptor() {
				 public EValidator getEValidator() {
					 return MuleValidator.INSTANCE;
				 }
			 });

		// Mark meta-data to indicate it can't be changed
		theMulePackage.freeze();

		return theMulePackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getAgentsType() {
		return agentsTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getAgentsType_Mixed() {
		return (EAttribute)agentsTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getAgentsType_Agent() {
		return (EReference)agentsTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getAgentType() {
		return agentTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getAgentType_Mixed() {
		return (EAttribute)agentTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getAgentType_Properties() {
		return (EReference)agentTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getAgentType_ClassName() {
		return (EAttribute)agentTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getAgentType_Name() {
		return (EAttribute)agentTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getAgentType_Ref() {
		return (EAttribute)agentTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getCatchAllStrategyType() {
		return catchAllStrategyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getCatchAllStrategyType_Mixed() {
		return (EAttribute)catchAllStrategyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getCatchAllStrategyType_Endpoint() {
		return (EReference)catchAllStrategyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getCatchAllStrategyType_GlobalEndpoint() {
		return (EReference)catchAllStrategyTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getCatchAllStrategyType_Properties() {
		return (EReference)catchAllStrategyTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getCatchAllStrategyType_ClassName() {
		return (EAttribute)catchAllStrategyTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getComponentFactoryType() {
		return componentFactoryTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getComponentFactoryType_Mixed() {
		return (EAttribute)componentFactoryTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getComponentFactoryType_ClassName() {
		return (EAttribute)componentFactoryTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getComponentLifecycleAdapterFactoryType() {
		return componentLifecycleAdapterFactoryTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getComponentLifecycleAdapterFactoryType_Mixed() {
		return (EAttribute)componentLifecycleAdapterFactoryTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getComponentLifecycleAdapterFactoryType_ClassName() {
		return (EAttribute)componentLifecycleAdapterFactoryTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getComponentPoolFactoryType() {
		return componentPoolFactoryTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getComponentPoolFactoryType_Mixed() {
		return (EAttribute)componentPoolFactoryTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getComponentPoolFactoryType_Properties() {
		return (EReference)componentPoolFactoryTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getComponentPoolFactoryType_ClassName() {
		return (EAttribute)componentPoolFactoryTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getConnectionStrategyType() {
		return connectionStrategyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConnectionStrategyType_Mixed() {
		return (EAttribute)connectionStrategyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConnectionStrategyType_Properties() {
		return (EReference)connectionStrategyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConnectionStrategyType_ClassName() {
		return (EAttribute)connectionStrategyTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getConnectorType() {
		return connectorTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConnectorType_Mixed() {
		return (EAttribute)connectorTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConnectorType_Properties() {
		return (EReference)connectorTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConnectorType_ThreadingProfile() {
		return (EReference)connectorTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConnectorType_ExceptionStrategy() {
		return (EReference)connectorTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConnectorType_ConnectionStrategy() {
		return (EReference)connectorTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConnectorType_ClassName() {
		return (EAttribute)connectorTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConnectorType_Name() {
		return (EAttribute)connectorTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConnectorType_Ref() {
		return (EAttribute)connectorTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getConstraintType() {
		return constraintTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintType_Mixed() {
		return (EAttribute)constraintTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConstraintType_LeftFilter() {
		return (EReference)constraintTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConstraintType_RightFilter() {
		return (EReference)constraintTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getConstraintType_Filter() {
		return (EReference)constraintTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintType_BatchSize() {
		return (EAttribute)constraintTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintType_ClassName() {
		return (EAttribute)constraintTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintType_ExpectedType() {
		return (EAttribute)constraintTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintType_Expression() {
		return (EAttribute)constraintTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintType_Frequency() {
		return (EAttribute)constraintTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintType_Path() {
		return (EAttribute)constraintTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getConstraintType_Pattern() {
		return (EAttribute)constraintTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getContainerContextType() {
		return containerContextTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerContextType_Mixed() {
		return (EAttribute)containerContextTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getContainerContextType_Properties() {
		return (EReference)containerContextTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerContextType_ClassName() {
		return (EAttribute)containerContextTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerContextType_Name() {
		return (EAttribute)containerContextTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getContainerEntryType() {
		return containerEntryTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerEntryType_Mixed() {
		return (EAttribute)containerEntryTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerEntryType_Reference() {
		return (EAttribute)containerEntryTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerEntryType_Required() {
		return (EAttribute)containerEntryTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getContainerPropertyType() {
		return containerPropertyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerPropertyType_Mixed() {
		return (EAttribute)containerPropertyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerPropertyType_Container() {
		return (EAttribute)containerPropertyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerPropertyType_Name() {
		return (EAttribute)containerPropertyTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerPropertyType_Reference() {
		return (EAttribute)containerPropertyTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getContainerPropertyType_Required() {
		return (EAttribute)containerPropertyTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getDocumentRoot() {
		return documentRootEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getDocumentRoot_Mixed() {
		return (EAttribute)documentRootEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDocumentRoot_XMLNSPrefixMap() {
		return (EReference)documentRootEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDocumentRoot_XSISchemaLocation() {
		return (EReference)documentRootEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getDocumentRoot_MuleConfiguration() {
		return (EReference)documentRootEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getEncryptionStrategyType() {
		return encryptionStrategyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEncryptionStrategyType_Mixed() {
		return (EAttribute)encryptionStrategyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEncryptionStrategyType_Properties() {
		return (EReference)encryptionStrategyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEncryptionStrategyType_ClassName() {
		return (EAttribute)encryptionStrategyTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEncryptionStrategyType_Name() {
		return (EAttribute)encryptionStrategyTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getEndpointIdentifiersType() {
		return endpointIdentifiersTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointIdentifiersType_Mixed() {
		return (EAttribute)endpointIdentifiersTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEndpointIdentifiersType_EndpointIdentifier() {
		return (EReference)endpointIdentifiersTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getEndpointIdentifierType() {
		return endpointIdentifierTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointIdentifierType_Mixed() {
		return (EAttribute)endpointIdentifierTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointIdentifierType_Name() {
		return (EAttribute)endpointIdentifierTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointIdentifierType_Value() {
		return (EAttribute)endpointIdentifierTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getEndpointType() {
		return endpointTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_Mixed() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEndpointType_Transaction() {
		return (EReference)endpointTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEndpointType_Filter() {
		return (EReference)endpointTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEndpointType_SecurityFilter() {
		return (EReference)endpointTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEndpointType_Properties() {
		return (EReference)endpointTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_Address() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_Connector() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_CreateConnector() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_Name() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_Ref() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_RemoteSync() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_RemoteSyncTimeout() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_ResponseTransformers() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_Synchronous() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_Transformers() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(14);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEndpointType_Type() {
		return (EAttribute)endpointTypeEClass.getEStructuralFeatures().get(15);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getEntryPointResolverType() {
		return entryPointResolverTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEntryPointResolverType_Mixed() {
		return (EAttribute)entryPointResolverTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEntryPointResolverType_ClassName() {
		return (EAttribute)entryPointResolverTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getEntryType() {
		return entryTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEntryType_Mixed() {
		return (EAttribute)entryTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEntryType_Value() {
		return (EAttribute)entryTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getEnvironmentPropertiesType() {
		return environmentPropertiesTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEnvironmentPropertiesType_Mixed() {
		return (EAttribute)environmentPropertiesTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getEnvironmentPropertiesType_Group() {
		return (EAttribute)environmentPropertiesTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEnvironmentPropertiesType_Property() {
		return (EReference)environmentPropertiesTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEnvironmentPropertiesType_FactoryProperty() {
		return (EReference)environmentPropertiesTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEnvironmentPropertiesType_SystemProperty() {
		return (EReference)environmentPropertiesTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEnvironmentPropertiesType_Map() {
		return (EReference)environmentPropertiesTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEnvironmentPropertiesType_List() {
		return (EReference)environmentPropertiesTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getEnvironmentPropertiesType_FileProperties() {
		return (EReference)environmentPropertiesTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getExceptionStrategyType() {
		return exceptionStrategyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getExceptionStrategyType_Mixed() {
		return (EAttribute)exceptionStrategyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getExceptionStrategyType_Endpoint() {
		return (EReference)exceptionStrategyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getExceptionStrategyType_GlobalEndpoint() {
		return (EReference)exceptionStrategyTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getExceptionStrategyType_Properties() {
		return (EReference)exceptionStrategyTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getExceptionStrategyType_ClassName() {
		return (EAttribute)exceptionStrategyTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getFactoryEntryType() {
		return factoryEntryTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFactoryEntryType_Mixed() {
		return (EAttribute)factoryEntryTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFactoryEntryType_Factory() {
		return (EAttribute)factoryEntryTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getFactoryPropertyType() {
		return factoryPropertyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFactoryPropertyType_Mixed() {
		return (EAttribute)factoryPropertyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFactoryPropertyType_Factory() {
		return (EAttribute)factoryPropertyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFactoryPropertyType_Name() {
		return (EAttribute)factoryPropertyTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getFilePropertiesType() {
		return filePropertiesTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilePropertiesType_Mixed() {
		return (EAttribute)filePropertiesTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilePropertiesType_Location() {
		return (EAttribute)filePropertiesTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilePropertiesType_Override() {
		return (EAttribute)filePropertiesTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getFilterType() {
		return filterTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilterType_Mixed() {
		return (EAttribute)filterTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFilterType_Properties() {
		return (EReference)filterTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFilterType_Filter() {
		return (EReference)filterTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFilterType_LeftFilter() {
		return (EReference)filterTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getFilterType_RightFilter() {
		return (EReference)filterTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilterType_ClassName() {
		return (EAttribute)filterTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilterType_ConfigFile() {
		return (EAttribute)filterTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilterType_ExpectedType() {
		return (EAttribute)filterTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilterType_Expression() {
		return (EAttribute)filterTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilterType_Path() {
		return (EAttribute)filterTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getFilterType_Pattern() {
		return (EAttribute)filterTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getGlobalEndpointsType() {
		return globalEndpointsTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getGlobalEndpointsType_Mixed() {
		return (EAttribute)globalEndpointsTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getGlobalEndpointsType_Endpoint() {
		return (EReference)globalEndpointsTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getGlobalEndpointType() {
		return globalEndpointTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getGlobalEndpointType_Mixed() {
		return (EAttribute)globalEndpointTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getGlobalEndpointType_Transaction() {
		return (EReference)globalEndpointTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getGlobalEndpointType_Filter() {
		return (EReference)globalEndpointTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getGlobalEndpointType_SecurityFilter() {
		return (EReference)globalEndpointTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getGlobalEndpointType_Properties() {
		return (EReference)globalEndpointTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getGlobalEndpointType_Address() {
		return (EAttribute)globalEndpointTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getGlobalEndpointType_Name() {
		return (EAttribute)globalEndpointTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getGlobalEndpointType_RemoteSync() {
		return (EAttribute)globalEndpointTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getGlobalEndpointType_RemoteSyncTimeout() {
		return (EAttribute)globalEndpointTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getGlobalEndpointType_ResponseTransformers() {
		return (EAttribute)globalEndpointTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getGlobalEndpointType_Synchronous() {
		return (EAttribute)globalEndpointTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getGlobalEndpointType_Transformers() {
		return (EAttribute)globalEndpointTypeEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getInboundRouterType() {
		return inboundRouterTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getInboundRouterType_Mixed() {
		return (EAttribute)inboundRouterTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInboundRouterType_CatchAllStrategy() {
		return (EReference)inboundRouterTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInboundRouterType_Endpoint() {
		return (EReference)inboundRouterTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInboundRouterType_GlobalEndpoint() {
		return (EReference)inboundRouterTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInboundRouterType_Router() {
		return (EReference)inboundRouterTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getInboundRouterType_MatchAll() {
		return (EAttribute)inboundRouterTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getInterceptorStackType() {
		return interceptorStackTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getInterceptorStackType_Mixed() {
		return (EAttribute)interceptorStackTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInterceptorStackType_Interceptor() {
		return (EReference)interceptorStackTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getInterceptorStackType_Name() {
		return (EAttribute)interceptorStackTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getInterceptorType() {
		return interceptorTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getInterceptorType_Mixed() {
		return (EAttribute)interceptorTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getInterceptorType_Properties() {
		return (EReference)interceptorTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getInterceptorType_ClassName() {
		return (EAttribute)interceptorTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getInterceptorType_Name() {
		return (EAttribute)interceptorTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getLeftFilterType() {
		return leftFilterTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getLeftFilterType_Mixed() {
		return (EAttribute)leftFilterTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getLeftFilterType_Properties() {
		return (EReference)leftFilterTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getLeftFilterType_Filter() {
		return (EReference)leftFilterTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getLeftFilterType_LeftFilter() {
		return (EReference)leftFilterTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getLeftFilterType_RightFilter() {
		return (EReference)leftFilterTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getLeftFilterType_ClassName() {
		return (EAttribute)leftFilterTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getLeftFilterType_ConfigFile() {
		return (EAttribute)leftFilterTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getLeftFilterType_ExpectedType() {
		return (EAttribute)leftFilterTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getLeftFilterType_Expression() {
		return (EAttribute)leftFilterTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getLeftFilterType_Path() {
		return (EAttribute)leftFilterTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getLeftFilterType_Pattern() {
		return (EAttribute)leftFilterTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getListType() {
		return listTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListType_Mixed() {
		return (EAttribute)listTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListType_Group() {
		return (EAttribute)listTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getListType_Entry() {
		return (EReference)listTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getListType_FactoryEntry() {
		return (EReference)listTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getListType_SystemEntry() {
		return (EReference)listTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getListType_ContainerEntry() {
		return (EReference)listTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getListType_Name() {
		return (EAttribute)listTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getMapType() {
		return mapTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMapType_Mixed() {
		return (EAttribute)mapTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMapType_Group() {
		return (EAttribute)mapTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMapType_Property() {
		return (EReference)mapTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMapType_FactoryProperty() {
		return (EReference)mapTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMapType_ContainerProperty() {
		return (EReference)mapTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMapType_SystemProperty() {
		return (EReference)mapTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMapType_Map() {
		return (EReference)mapTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMapType_List() {
		return (EReference)mapTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMapType_FileProperties() {
		return (EReference)mapTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMapType_Name() {
		return (EAttribute)mapTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getModelType() {
		return modelTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getModelType_Mixed() {
		return (EAttribute)modelTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getModelType_Description() {
		return (EAttribute)modelTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getModelType_EntryPointResolver() {
		return (EReference)modelTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getModelType_ComponentFactory() {
		return (EReference)modelTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getModelType_ComponentLifecycleAdapterFactory() {
		return (EReference)modelTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getModelType_ComponentPoolFactory() {
		return (EReference)modelTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getModelType_ExceptionStrategy() {
		return (EReference)modelTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getModelType_MuleDescriptor() {
		return (EReference)modelTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getModelType_ClassName() {
		return (EAttribute)modelTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getModelType_Name() {
		return (EAttribute)modelTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getModelType_Ref() {
		return (EAttribute)modelTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getModelType_Type() {
		return (EAttribute)modelTypeEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getMuleConfigurationType() {
		return muleConfigurationTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleConfigurationType_Mixed() {
		return (EAttribute)muleConfigurationTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleConfigurationType_Description() {
		return (EAttribute)muleConfigurationTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_EnvironmentProperties() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_MuleEnvironmentProperties() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_ContainerContext() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_SecurityManager() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_TransactionManager() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_Agents() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_Connector() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_EndpointIdentifiers() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_Transformers() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_GlobalEndpoints() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_InterceptorStack() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_Model() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleConfigurationType_MuleDescriptor() {
		return (EReference)muleConfigurationTypeEClass.getEStructuralFeatures().get(14);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleConfigurationType_Id() {
		return (EAttribute)muleConfigurationTypeEClass.getEStructuralFeatures().get(15);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleConfigurationType_Version() {
		return (EAttribute)muleConfigurationTypeEClass.getEStructuralFeatures().get(16);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getMuleDescriptorType() {
		return muleDescriptorTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_Mixed() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleDescriptorType_InboundRouter() {
		return (EReference)muleDescriptorTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleDescriptorType_OutboundRouter() {
		return (EReference)muleDescriptorTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleDescriptorType_ResponseRouter() {
		return (EReference)muleDescriptorTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleDescriptorType_Interceptor() {
		return (EReference)muleDescriptorTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleDescriptorType_ThreadingProfile() {
		return (EReference)muleDescriptorTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleDescriptorType_PoolingProfile() {
		return (EReference)muleDescriptorTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleDescriptorType_QueueProfile() {
		return (EReference)muleDescriptorTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleDescriptorType_ExceptionStrategy() {
		return (EReference)muleDescriptorTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleDescriptorType_Properties() {
		return (EReference)muleDescriptorTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_ContainerManaged() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_Implementation() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_InboundEndpoint() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_InboundTransformer() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_InitialState() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(14);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_Name() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(15);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_OutboundEndpoint() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(16);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_OutboundTransformer() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(17);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_Ref() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(18);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_ResponseTransformer() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(19);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_Singleton() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(20);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleDescriptorType_Version() {
		return (EAttribute)muleDescriptorTypeEClass.getEStructuralFeatures().get(21);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getMuleEnvironmentPropertiesType() {
		return muleEnvironmentPropertiesTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_Mixed() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleEnvironmentPropertiesType_ThreadingProfile() {
		return (EReference)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleEnvironmentPropertiesType_PoolingProfile() {
		return (EReference)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleEnvironmentPropertiesType_QueueProfile() {
		return (EReference)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleEnvironmentPropertiesType_PersistenceStrategy() {
		return (EReference)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getMuleEnvironmentPropertiesType_ConnectionStrategy() {
		return (EReference)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_ClientMode() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_Embedded() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_EnableMessageEvents() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_Encoding() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_Model() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_RecoverableMode() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_RemoteSync() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_ServerUrl() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_Synchronous() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(14);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_SynchronousEventTimeout() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(15);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_TransactionTimeout() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(16);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getMuleEnvironmentPropertiesType_WorkingDirectory() {
		return (EAttribute)muleEnvironmentPropertiesTypeEClass.getEStructuralFeatures().get(17);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getOutboundRouterType() {
		return outboundRouterTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getOutboundRouterType_Mixed() {
		return (EAttribute)outboundRouterTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getOutboundRouterType_CatchAllStrategy() {
		return (EReference)outboundRouterTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getOutboundRouterType_Router() {
		return (EReference)outboundRouterTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getOutboundRouterType_MatchAll() {
		return (EAttribute)outboundRouterTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getPersistenceStrategyType() {
		return persistenceStrategyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPersistenceStrategyType_Mixed() {
		return (EAttribute)persistenceStrategyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getPersistenceStrategyType_Properties() {
		return (EReference)persistenceStrategyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPersistenceStrategyType_ClassName() {
		return (EAttribute)persistenceStrategyTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getPoolingProfileType() {
		return poolingProfileTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPoolingProfileType_Mixed() {
		return (EAttribute)poolingProfileTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPoolingProfileType_ExhaustedAction() {
		return (EAttribute)poolingProfileTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPoolingProfileType_Factory() {
		return (EAttribute)poolingProfileTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPoolingProfileType_InitialisationPolicy() {
		return (EAttribute)poolingProfileTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPoolingProfileType_MaxActive() {
		return (EAttribute)poolingProfileTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPoolingProfileType_MaxIdle() {
		return (EAttribute)poolingProfileTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPoolingProfileType_MaxWait() {
		return (EAttribute)poolingProfileTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getPropertiesType() {
		return propertiesTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPropertiesType_Mixed() {
		return (EAttribute)propertiesTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPropertiesType_Group() {
		return (EAttribute)propertiesTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getPropertiesType_Property() {
		return (EReference)propertiesTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getPropertiesType_FactoryProperty() {
		return (EReference)propertiesTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getPropertiesType_ContainerProperty() {
		return (EReference)propertiesTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getPropertiesType_SystemProperty() {
		return (EReference)propertiesTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getPropertiesType_Map() {
		return (EReference)propertiesTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getPropertiesType_List() {
		return (EReference)propertiesTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getPropertiesType_FileProperties() {
		return (EReference)propertiesTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getPropertiesType_TextProperty() {
		return (EReference)propertiesTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getPropertyType() {
		return propertyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPropertyType_Mixed() {
		return (EAttribute)propertyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPropertyType_Name() {
		return (EAttribute)propertyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getPropertyType_Value() {
		return (EAttribute)propertyTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getQueueProfileType() {
		return queueProfileTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getQueueProfileType_Mixed() {
		return (EAttribute)queueProfileTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getQueueProfileType_Properties() {
		return (EReference)queueProfileTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getQueueProfileType_MaxOutstandingMessages() {
		return (EAttribute)queueProfileTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getQueueProfileType_Persistent() {
		return (EAttribute)queueProfileTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getReplyToType() {
		return replyToTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getReplyToType_Mixed() {
		return (EAttribute)replyToTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getReplyToType_Address() {
		return (EAttribute)replyToTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getResponseRouterType() {
		return responseRouterTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getResponseRouterType_Mixed() {
		return (EAttribute)responseRouterTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getResponseRouterType_Endpoint() {
		return (EReference)responseRouterTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getResponseRouterType_GlobalEndpoint() {
		return (EReference)responseRouterTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getResponseRouterType_Router() {
		return (EReference)responseRouterTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getResponseRouterType_Timeout() {
		return (EAttribute)responseRouterTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRightFilterType() {
		return rightFilterTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRightFilterType_Mixed() {
		return (EAttribute)rightFilterTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRightFilterType_Properties() {
		return (EReference)rightFilterTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRightFilterType_Filter() {
		return (EReference)rightFilterTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRightFilterType_LeftFilter() {
		return (EReference)rightFilterTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRightFilterType_RightFilter() {
		return (EReference)rightFilterTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRightFilterType_ClassName() {
		return (EAttribute)rightFilterTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRightFilterType_ConfigFile() {
		return (EAttribute)rightFilterTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRightFilterType_ExpectedType() {
		return (EAttribute)rightFilterTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRightFilterType_Expression() {
		return (EAttribute)rightFilterTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRightFilterType_Path() {
		return (EAttribute)rightFilterTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRightFilterType_Pattern() {
		return (EAttribute)rightFilterTypeEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getRouterType() {
		return routerTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRouterType_Mixed() {
		return (EAttribute)routerTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRouterType_Endpoint() {
		return (EReference)routerTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRouterType_GlobalEndpoint() {
		return (EReference)routerTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRouterType_ReplyTo() {
		return (EReference)routerTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRouterType_Transaction() {
		return (EReference)routerTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRouterType_Filter() {
		return (EReference)routerTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getRouterType_Properties() {
		return (EReference)routerTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRouterType_ClassName() {
		return (EAttribute)routerTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRouterType_EnableCorrelation() {
		return (EAttribute)routerTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getRouterType_PropertyExtractor() {
		return (EAttribute)routerTypeEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getSecurityFilterType() {
		return securityFilterTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityFilterType_Mixed() {
		return (EAttribute)securityFilterTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getSecurityFilterType_Properties() {
		return (EReference)securityFilterTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityFilterType_ClassName() {
		return (EAttribute)securityFilterTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityFilterType_UseProviders() {
		return (EAttribute)securityFilterTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getSecurityManagerType() {
		return securityManagerTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityManagerType_Mixed() {
		return (EAttribute)securityManagerTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getSecurityManagerType_SecurityProvider() {
		return (EReference)securityManagerTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getSecurityManagerType_EncryptionStrategy() {
		return (EReference)securityManagerTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityManagerType_ClassName() {
		return (EAttribute)securityManagerTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityManagerType_Ref() {
		return (EAttribute)securityManagerTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getSecurityProviderType() {
		return securityProviderTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityProviderType_Mixed() {
		return (EAttribute)securityProviderTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getSecurityProviderType_Properties() {
		return (EReference)securityProviderTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityProviderType_ClassName() {
		return (EAttribute)securityProviderTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityProviderType_Name() {
		return (EAttribute)securityProviderTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSecurityProviderType_Ref() {
		return (EAttribute)securityProviderTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getSystemEntryType() {
		return systemEntryTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSystemEntryType_Mixed() {
		return (EAttribute)systemEntryTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSystemEntryType_DefaultValue() {
		return (EAttribute)systemEntryTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSystemEntryType_Key() {
		return (EAttribute)systemEntryTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getSystemPropertyType() {
		return systemPropertyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSystemPropertyType_Mixed() {
		return (EAttribute)systemPropertyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSystemPropertyType_DefaultValue() {
		return (EAttribute)systemPropertyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSystemPropertyType_Key() {
		return (EAttribute)systemPropertyTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getSystemPropertyType_Name() {
		return (EAttribute)systemPropertyTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getTextPropertyType() {
		return textPropertyTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTextPropertyType_Value() {
		return (EAttribute)textPropertyTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTextPropertyType_Name() {
		return (EAttribute)textPropertyTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getThreadingProfileType() {
		return threadingProfileTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getThreadingProfileType_Mixed() {
		return (EAttribute)threadingProfileTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getThreadingProfileType_DoThreading() {
		return (EAttribute)threadingProfileTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getThreadingProfileType_Id() {
		return (EAttribute)threadingProfileTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getThreadingProfileType_MaxBufferSize() {
		return (EAttribute)threadingProfileTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getThreadingProfileType_MaxThreadsActive() {
		return (EAttribute)threadingProfileTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getThreadingProfileType_MaxThreadsIdle() {
		return (EAttribute)threadingProfileTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getThreadingProfileType_PoolExhaustedAction() {
		return (EAttribute)threadingProfileTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getThreadingProfileType_ThreadTTL() {
		return (EAttribute)threadingProfileTypeEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getThreadingProfileType_ThreadWaitTimeout() {
		return (EAttribute)threadingProfileTypeEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getTransactionManagerType() {
		return transactionManagerTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransactionManagerType_Mixed() {
		return (EAttribute)transactionManagerTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getTransactionManagerType_Properties() {
		return (EReference)transactionManagerTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransactionManagerType_Factory() {
		return (EAttribute)transactionManagerTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransactionManagerType_Ref() {
		return (EAttribute)transactionManagerTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getTransactionType() {
		return transactionTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransactionType_Mixed() {
		return (EAttribute)transactionTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getTransactionType_Constraint() {
		return (EReference)transactionTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransactionType_Action() {
		return (EAttribute)transactionTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransactionType_Factory() {
		return (EAttribute)transactionTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransactionType_Timeout() {
		return (EAttribute)transactionTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getTransformersType() {
		return transformersTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransformersType_Mixed() {
		return (EAttribute)transformersTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getTransformersType_Transformer() {
		return (EReference)transformersTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EClass getTransformerType() {
		return transformerTypeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransformerType_Mixed() {
		return (EAttribute)transformerTypeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EReference getTransformerType_Properties() {
		return (EReference)transformerTypeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransformerType_ClassName() {
		return (EAttribute)transformerTypeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransformerType_IgnoreBadInput() {
		return (EAttribute)transformerTypeEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransformerType_Name() {
		return (EAttribute)transformerTypeEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransformerType_Ref() {
		return (EAttribute)transformerTypeEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EAttribute getTransformerType_ReturnClass() {
		return (EAttribute)transformerTypeEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getActionType() {
		return actionTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getCreateConnectorType() {
		return createConnectorTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getEnableCorrelationType() {
		return enableCorrelationTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getExhaustedActionType() {
		return exhaustedActionTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getIdType() {
		return idTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getInitialisationPolicyType() {
		return initialisationPolicyTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getInitialStateType() {
		return initialStateTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getPoolExhaustedActionType() {
		return poolExhaustedActionTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EEnum getTypeType1() {
		return typeType1EEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getActionTypeObject() {
		return actionTypeObjectEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getCreateConnectorTypeObject() {
		return createConnectorTypeObjectEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getEnableCorrelationTypeObject() {
		return enableCorrelationTypeObjectEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getExhaustedActionTypeObject() {
		return exhaustedActionTypeObjectEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getIdTypeObject() {
		return idTypeObjectEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getInitialisationPolicyTypeObject() {
		return initialisationPolicyTypeObjectEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getInitialStateTypeObject() {
		return initialStateTypeObjectEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getPoolExhaustedActionTypeObject() {
		return poolExhaustedActionTypeObjectEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getTypeType() {
		return typeTypeEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getTypeTypeObject() {
		return typeTypeObjectEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EDataType getVersionType() {
		return versionTypeEDataType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MuleFactory getMuleFactory() {
		return (MuleFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		agentsTypeEClass = createEClass(AGENTS_TYPE);
		createEAttribute(agentsTypeEClass, AGENTS_TYPE__MIXED);
		createEReference(agentsTypeEClass, AGENTS_TYPE__AGENT);

		agentTypeEClass = createEClass(AGENT_TYPE);
		createEAttribute(agentTypeEClass, AGENT_TYPE__MIXED);
		createEReference(agentTypeEClass, AGENT_TYPE__PROPERTIES);
		createEAttribute(agentTypeEClass, AGENT_TYPE__CLASS_NAME);
		createEAttribute(agentTypeEClass, AGENT_TYPE__NAME);
		createEAttribute(agentTypeEClass, AGENT_TYPE__REF);

		catchAllStrategyTypeEClass = createEClass(CATCH_ALL_STRATEGY_TYPE);
		createEAttribute(catchAllStrategyTypeEClass, CATCH_ALL_STRATEGY_TYPE__MIXED);
		createEReference(catchAllStrategyTypeEClass, CATCH_ALL_STRATEGY_TYPE__ENDPOINT);
		createEReference(catchAllStrategyTypeEClass, CATCH_ALL_STRATEGY_TYPE__GLOBAL_ENDPOINT);
		createEReference(catchAllStrategyTypeEClass, CATCH_ALL_STRATEGY_TYPE__PROPERTIES);
		createEAttribute(catchAllStrategyTypeEClass, CATCH_ALL_STRATEGY_TYPE__CLASS_NAME);

		componentFactoryTypeEClass = createEClass(COMPONENT_FACTORY_TYPE);
		createEAttribute(componentFactoryTypeEClass, COMPONENT_FACTORY_TYPE__MIXED);
		createEAttribute(componentFactoryTypeEClass, COMPONENT_FACTORY_TYPE__CLASS_NAME);

		componentLifecycleAdapterFactoryTypeEClass = createEClass(COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE);
		createEAttribute(componentLifecycleAdapterFactoryTypeEClass, COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE__MIXED);
		createEAttribute(componentLifecycleAdapterFactoryTypeEClass, COMPONENT_LIFECYCLE_ADAPTER_FACTORY_TYPE__CLASS_NAME);

		componentPoolFactoryTypeEClass = createEClass(COMPONENT_POOL_FACTORY_TYPE);
		createEAttribute(componentPoolFactoryTypeEClass, COMPONENT_POOL_FACTORY_TYPE__MIXED);
		createEReference(componentPoolFactoryTypeEClass, COMPONENT_POOL_FACTORY_TYPE__PROPERTIES);
		createEAttribute(componentPoolFactoryTypeEClass, COMPONENT_POOL_FACTORY_TYPE__CLASS_NAME);

		connectionStrategyTypeEClass = createEClass(CONNECTION_STRATEGY_TYPE);
		createEAttribute(connectionStrategyTypeEClass, CONNECTION_STRATEGY_TYPE__MIXED);
		createEReference(connectionStrategyTypeEClass, CONNECTION_STRATEGY_TYPE__PROPERTIES);
		createEAttribute(connectionStrategyTypeEClass, CONNECTION_STRATEGY_TYPE__CLASS_NAME);

		connectorTypeEClass = createEClass(CONNECTOR_TYPE);
		createEAttribute(connectorTypeEClass, CONNECTOR_TYPE__MIXED);
		createEReference(connectorTypeEClass, CONNECTOR_TYPE__PROPERTIES);
		createEReference(connectorTypeEClass, CONNECTOR_TYPE__THREADING_PROFILE);
		createEReference(connectorTypeEClass, CONNECTOR_TYPE__EXCEPTION_STRATEGY);
		createEReference(connectorTypeEClass, CONNECTOR_TYPE__CONNECTION_STRATEGY);
		createEAttribute(connectorTypeEClass, CONNECTOR_TYPE__CLASS_NAME);
		createEAttribute(connectorTypeEClass, CONNECTOR_TYPE__NAME);
		createEAttribute(connectorTypeEClass, CONNECTOR_TYPE__REF);

		constraintTypeEClass = createEClass(CONSTRAINT_TYPE);
		createEAttribute(constraintTypeEClass, CONSTRAINT_TYPE__MIXED);
		createEReference(constraintTypeEClass, CONSTRAINT_TYPE__LEFT_FILTER);
		createEReference(constraintTypeEClass, CONSTRAINT_TYPE__RIGHT_FILTER);
		createEReference(constraintTypeEClass, CONSTRAINT_TYPE__FILTER);
		createEAttribute(constraintTypeEClass, CONSTRAINT_TYPE__BATCH_SIZE);
		createEAttribute(constraintTypeEClass, CONSTRAINT_TYPE__CLASS_NAME);
		createEAttribute(constraintTypeEClass, CONSTRAINT_TYPE__EXPECTED_TYPE);
		createEAttribute(constraintTypeEClass, CONSTRAINT_TYPE__EXPRESSION);
		createEAttribute(constraintTypeEClass, CONSTRAINT_TYPE__FREQUENCY);
		createEAttribute(constraintTypeEClass, CONSTRAINT_TYPE__PATH);
		createEAttribute(constraintTypeEClass, CONSTRAINT_TYPE__PATTERN);

		containerContextTypeEClass = createEClass(CONTAINER_CONTEXT_TYPE);
		createEAttribute(containerContextTypeEClass, CONTAINER_CONTEXT_TYPE__MIXED);
		createEReference(containerContextTypeEClass, CONTAINER_CONTEXT_TYPE__PROPERTIES);
		createEAttribute(containerContextTypeEClass, CONTAINER_CONTEXT_TYPE__CLASS_NAME);
		createEAttribute(containerContextTypeEClass, CONTAINER_CONTEXT_TYPE__NAME);

		containerEntryTypeEClass = createEClass(CONTAINER_ENTRY_TYPE);
		createEAttribute(containerEntryTypeEClass, CONTAINER_ENTRY_TYPE__MIXED);
		createEAttribute(containerEntryTypeEClass, CONTAINER_ENTRY_TYPE__REFERENCE);
		createEAttribute(containerEntryTypeEClass, CONTAINER_ENTRY_TYPE__REQUIRED);

		containerPropertyTypeEClass = createEClass(CONTAINER_PROPERTY_TYPE);
		createEAttribute(containerPropertyTypeEClass, CONTAINER_PROPERTY_TYPE__MIXED);
		createEAttribute(containerPropertyTypeEClass, CONTAINER_PROPERTY_TYPE__CONTAINER);
		createEAttribute(containerPropertyTypeEClass, CONTAINER_PROPERTY_TYPE__NAME);
		createEAttribute(containerPropertyTypeEClass, CONTAINER_PROPERTY_TYPE__REFERENCE);
		createEAttribute(containerPropertyTypeEClass, CONTAINER_PROPERTY_TYPE__REQUIRED);

		documentRootEClass = createEClass(DOCUMENT_ROOT);
		createEAttribute(documentRootEClass, DOCUMENT_ROOT__MIXED);
		createEReference(documentRootEClass, DOCUMENT_ROOT__XMLNS_PREFIX_MAP);
		createEReference(documentRootEClass, DOCUMENT_ROOT__XSI_SCHEMA_LOCATION);
		createEReference(documentRootEClass, DOCUMENT_ROOT__MULE_CONFIGURATION);

		encryptionStrategyTypeEClass = createEClass(ENCRYPTION_STRATEGY_TYPE);
		createEAttribute(encryptionStrategyTypeEClass, ENCRYPTION_STRATEGY_TYPE__MIXED);
		createEReference(encryptionStrategyTypeEClass, ENCRYPTION_STRATEGY_TYPE__PROPERTIES);
		createEAttribute(encryptionStrategyTypeEClass, ENCRYPTION_STRATEGY_TYPE__CLASS_NAME);
		createEAttribute(encryptionStrategyTypeEClass, ENCRYPTION_STRATEGY_TYPE__NAME);

		endpointIdentifiersTypeEClass = createEClass(ENDPOINT_IDENTIFIERS_TYPE);
		createEAttribute(endpointIdentifiersTypeEClass, ENDPOINT_IDENTIFIERS_TYPE__MIXED);
		createEReference(endpointIdentifiersTypeEClass, ENDPOINT_IDENTIFIERS_TYPE__ENDPOINT_IDENTIFIER);

		endpointIdentifierTypeEClass = createEClass(ENDPOINT_IDENTIFIER_TYPE);
		createEAttribute(endpointIdentifierTypeEClass, ENDPOINT_IDENTIFIER_TYPE__MIXED);
		createEAttribute(endpointIdentifierTypeEClass, ENDPOINT_IDENTIFIER_TYPE__NAME);
		createEAttribute(endpointIdentifierTypeEClass, ENDPOINT_IDENTIFIER_TYPE__VALUE);

		endpointTypeEClass = createEClass(ENDPOINT_TYPE);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__MIXED);
		createEReference(endpointTypeEClass, ENDPOINT_TYPE__TRANSACTION);
		createEReference(endpointTypeEClass, ENDPOINT_TYPE__FILTER);
		createEReference(endpointTypeEClass, ENDPOINT_TYPE__SECURITY_FILTER);
		createEReference(endpointTypeEClass, ENDPOINT_TYPE__PROPERTIES);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__ADDRESS);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__CONNECTOR);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__CREATE_CONNECTOR);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__NAME);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__REF);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__REMOTE_SYNC);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__RESPONSE_TRANSFORMERS);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__SYNCHRONOUS);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__TRANSFORMERS);
		createEAttribute(endpointTypeEClass, ENDPOINT_TYPE__TYPE);

		entryPointResolverTypeEClass = createEClass(ENTRY_POINT_RESOLVER_TYPE);
		createEAttribute(entryPointResolverTypeEClass, ENTRY_POINT_RESOLVER_TYPE__MIXED);
		createEAttribute(entryPointResolverTypeEClass, ENTRY_POINT_RESOLVER_TYPE__CLASS_NAME);

		entryTypeEClass = createEClass(ENTRY_TYPE);
		createEAttribute(entryTypeEClass, ENTRY_TYPE__MIXED);
		createEAttribute(entryTypeEClass, ENTRY_TYPE__VALUE);

		environmentPropertiesTypeEClass = createEClass(ENVIRONMENT_PROPERTIES_TYPE);
		createEAttribute(environmentPropertiesTypeEClass, ENVIRONMENT_PROPERTIES_TYPE__MIXED);
		createEAttribute(environmentPropertiesTypeEClass, ENVIRONMENT_PROPERTIES_TYPE__GROUP);
		createEReference(environmentPropertiesTypeEClass, ENVIRONMENT_PROPERTIES_TYPE__PROPERTY);
		createEReference(environmentPropertiesTypeEClass, ENVIRONMENT_PROPERTIES_TYPE__FACTORY_PROPERTY);
		createEReference(environmentPropertiesTypeEClass, ENVIRONMENT_PROPERTIES_TYPE__SYSTEM_PROPERTY);
		createEReference(environmentPropertiesTypeEClass, ENVIRONMENT_PROPERTIES_TYPE__MAP);
		createEReference(environmentPropertiesTypeEClass, ENVIRONMENT_PROPERTIES_TYPE__LIST);
		createEReference(environmentPropertiesTypeEClass, ENVIRONMENT_PROPERTIES_TYPE__FILE_PROPERTIES);

		exceptionStrategyTypeEClass = createEClass(EXCEPTION_STRATEGY_TYPE);
		createEAttribute(exceptionStrategyTypeEClass, EXCEPTION_STRATEGY_TYPE__MIXED);
		createEReference(exceptionStrategyTypeEClass, EXCEPTION_STRATEGY_TYPE__ENDPOINT);
		createEReference(exceptionStrategyTypeEClass, EXCEPTION_STRATEGY_TYPE__GLOBAL_ENDPOINT);
		createEReference(exceptionStrategyTypeEClass, EXCEPTION_STRATEGY_TYPE__PROPERTIES);
		createEAttribute(exceptionStrategyTypeEClass, EXCEPTION_STRATEGY_TYPE__CLASS_NAME);

		factoryEntryTypeEClass = createEClass(FACTORY_ENTRY_TYPE);
		createEAttribute(factoryEntryTypeEClass, FACTORY_ENTRY_TYPE__MIXED);
		createEAttribute(factoryEntryTypeEClass, FACTORY_ENTRY_TYPE__FACTORY);

		factoryPropertyTypeEClass = createEClass(FACTORY_PROPERTY_TYPE);
		createEAttribute(factoryPropertyTypeEClass, FACTORY_PROPERTY_TYPE__MIXED);
		createEAttribute(factoryPropertyTypeEClass, FACTORY_PROPERTY_TYPE__FACTORY);
		createEAttribute(factoryPropertyTypeEClass, FACTORY_PROPERTY_TYPE__NAME);

		filePropertiesTypeEClass = createEClass(FILE_PROPERTIES_TYPE);
		createEAttribute(filePropertiesTypeEClass, FILE_PROPERTIES_TYPE__MIXED);
		createEAttribute(filePropertiesTypeEClass, FILE_PROPERTIES_TYPE__LOCATION);
		createEAttribute(filePropertiesTypeEClass, FILE_PROPERTIES_TYPE__OVERRIDE);

		filterTypeEClass = createEClass(FILTER_TYPE);
		createEAttribute(filterTypeEClass, FILTER_TYPE__MIXED);
		createEReference(filterTypeEClass, FILTER_TYPE__PROPERTIES);
		createEReference(filterTypeEClass, FILTER_TYPE__FILTER);
		createEReference(filterTypeEClass, FILTER_TYPE__LEFT_FILTER);
		createEReference(filterTypeEClass, FILTER_TYPE__RIGHT_FILTER);
		createEAttribute(filterTypeEClass, FILTER_TYPE__CLASS_NAME);
		createEAttribute(filterTypeEClass, FILTER_TYPE__CONFIG_FILE);
		createEAttribute(filterTypeEClass, FILTER_TYPE__EXPECTED_TYPE);
		createEAttribute(filterTypeEClass, FILTER_TYPE__EXPRESSION);
		createEAttribute(filterTypeEClass, FILTER_TYPE__PATH);
		createEAttribute(filterTypeEClass, FILTER_TYPE__PATTERN);

		globalEndpointsTypeEClass = createEClass(GLOBAL_ENDPOINTS_TYPE);
		createEAttribute(globalEndpointsTypeEClass, GLOBAL_ENDPOINTS_TYPE__MIXED);
		createEReference(globalEndpointsTypeEClass, GLOBAL_ENDPOINTS_TYPE__ENDPOINT);

		globalEndpointTypeEClass = createEClass(GLOBAL_ENDPOINT_TYPE);
		createEAttribute(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__MIXED);
		createEReference(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__TRANSACTION);
		createEReference(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__FILTER);
		createEReference(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__SECURITY_FILTER);
		createEReference(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__PROPERTIES);
		createEAttribute(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__ADDRESS);
		createEAttribute(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__NAME);
		createEAttribute(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC);
		createEAttribute(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__REMOTE_SYNC_TIMEOUT);
		createEAttribute(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__RESPONSE_TRANSFORMERS);
		createEAttribute(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__SYNCHRONOUS);
		createEAttribute(globalEndpointTypeEClass, GLOBAL_ENDPOINT_TYPE__TRANSFORMERS);

		inboundRouterTypeEClass = createEClass(INBOUND_ROUTER_TYPE);
		createEAttribute(inboundRouterTypeEClass, INBOUND_ROUTER_TYPE__MIXED);
		createEReference(inboundRouterTypeEClass, INBOUND_ROUTER_TYPE__CATCH_ALL_STRATEGY);
		createEReference(inboundRouterTypeEClass, INBOUND_ROUTER_TYPE__ENDPOINT);
		createEReference(inboundRouterTypeEClass, INBOUND_ROUTER_TYPE__GLOBAL_ENDPOINT);
		createEReference(inboundRouterTypeEClass, INBOUND_ROUTER_TYPE__ROUTER);
		createEAttribute(inboundRouterTypeEClass, INBOUND_ROUTER_TYPE__MATCH_ALL);

		interceptorStackTypeEClass = createEClass(INTERCEPTOR_STACK_TYPE);
		createEAttribute(interceptorStackTypeEClass, INTERCEPTOR_STACK_TYPE__MIXED);
		createEReference(interceptorStackTypeEClass, INTERCEPTOR_STACK_TYPE__INTERCEPTOR);
		createEAttribute(interceptorStackTypeEClass, INTERCEPTOR_STACK_TYPE__NAME);

		interceptorTypeEClass = createEClass(INTERCEPTOR_TYPE);
		createEAttribute(interceptorTypeEClass, INTERCEPTOR_TYPE__MIXED);
		createEReference(interceptorTypeEClass, INTERCEPTOR_TYPE__PROPERTIES);
		createEAttribute(interceptorTypeEClass, INTERCEPTOR_TYPE__CLASS_NAME);
		createEAttribute(interceptorTypeEClass, INTERCEPTOR_TYPE__NAME);

		leftFilterTypeEClass = createEClass(LEFT_FILTER_TYPE);
		createEAttribute(leftFilterTypeEClass, LEFT_FILTER_TYPE__MIXED);
		createEReference(leftFilterTypeEClass, LEFT_FILTER_TYPE__PROPERTIES);
		createEReference(leftFilterTypeEClass, LEFT_FILTER_TYPE__FILTER);
		createEReference(leftFilterTypeEClass, LEFT_FILTER_TYPE__LEFT_FILTER);
		createEReference(leftFilterTypeEClass, LEFT_FILTER_TYPE__RIGHT_FILTER);
		createEAttribute(leftFilterTypeEClass, LEFT_FILTER_TYPE__CLASS_NAME);
		createEAttribute(leftFilterTypeEClass, LEFT_FILTER_TYPE__CONFIG_FILE);
		createEAttribute(leftFilterTypeEClass, LEFT_FILTER_TYPE__EXPECTED_TYPE);
		createEAttribute(leftFilterTypeEClass, LEFT_FILTER_TYPE__EXPRESSION);
		createEAttribute(leftFilterTypeEClass, LEFT_FILTER_TYPE__PATH);
		createEAttribute(leftFilterTypeEClass, LEFT_FILTER_TYPE__PATTERN);

		listTypeEClass = createEClass(LIST_TYPE);
		createEAttribute(listTypeEClass, LIST_TYPE__MIXED);
		createEAttribute(listTypeEClass, LIST_TYPE__GROUP);
		createEReference(listTypeEClass, LIST_TYPE__ENTRY);
		createEReference(listTypeEClass, LIST_TYPE__FACTORY_ENTRY);
		createEReference(listTypeEClass, LIST_TYPE__SYSTEM_ENTRY);
		createEReference(listTypeEClass, LIST_TYPE__CONTAINER_ENTRY);
		createEAttribute(listTypeEClass, LIST_TYPE__NAME);

		mapTypeEClass = createEClass(MAP_TYPE);
		createEAttribute(mapTypeEClass, MAP_TYPE__MIXED);
		createEAttribute(mapTypeEClass, MAP_TYPE__GROUP);
		createEReference(mapTypeEClass, MAP_TYPE__PROPERTY);
		createEReference(mapTypeEClass, MAP_TYPE__FACTORY_PROPERTY);
		createEReference(mapTypeEClass, MAP_TYPE__CONTAINER_PROPERTY);
		createEReference(mapTypeEClass, MAP_TYPE__SYSTEM_PROPERTY);
		createEReference(mapTypeEClass, MAP_TYPE__MAP);
		createEReference(mapTypeEClass, MAP_TYPE__LIST);
		createEReference(mapTypeEClass, MAP_TYPE__FILE_PROPERTIES);
		createEAttribute(mapTypeEClass, MAP_TYPE__NAME);

		modelTypeEClass = createEClass(MODEL_TYPE);
		createEAttribute(modelTypeEClass, MODEL_TYPE__MIXED);
		createEAttribute(modelTypeEClass, MODEL_TYPE__DESCRIPTION);
		createEReference(modelTypeEClass, MODEL_TYPE__ENTRY_POINT_RESOLVER);
		createEReference(modelTypeEClass, MODEL_TYPE__COMPONENT_FACTORY);
		createEReference(modelTypeEClass, MODEL_TYPE__COMPONENT_LIFECYCLE_ADAPTER_FACTORY);
		createEReference(modelTypeEClass, MODEL_TYPE__COMPONENT_POOL_FACTORY);
		createEReference(modelTypeEClass, MODEL_TYPE__EXCEPTION_STRATEGY);
		createEReference(modelTypeEClass, MODEL_TYPE__MULE_DESCRIPTOR);
		createEAttribute(modelTypeEClass, MODEL_TYPE__CLASS_NAME);
		createEAttribute(modelTypeEClass, MODEL_TYPE__NAME);
		createEAttribute(modelTypeEClass, MODEL_TYPE__REF);
		createEAttribute(modelTypeEClass, MODEL_TYPE__TYPE);

		muleConfigurationTypeEClass = createEClass(MULE_CONFIGURATION_TYPE);
		createEAttribute(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__MIXED);
		createEAttribute(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__DESCRIPTION);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__ENVIRONMENT_PROPERTIES);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__MULE_ENVIRONMENT_PROPERTIES);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__CONTAINER_CONTEXT);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__SECURITY_MANAGER);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__TRANSACTION_MANAGER);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__AGENTS);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__CONNECTOR);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__ENDPOINT_IDENTIFIERS);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__TRANSFORMERS);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__GLOBAL_ENDPOINTS);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__INTERCEPTOR_STACK);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__MODEL);
		createEReference(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__MULE_DESCRIPTOR);
		createEAttribute(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__ID);
		createEAttribute(muleConfigurationTypeEClass, MULE_CONFIGURATION_TYPE__VERSION);

		muleDescriptorTypeEClass = createEClass(MULE_DESCRIPTOR_TYPE);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__MIXED);
		createEReference(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__INBOUND_ROUTER);
		createEReference(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__OUTBOUND_ROUTER);
		createEReference(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__RESPONSE_ROUTER);
		createEReference(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__INTERCEPTOR);
		createEReference(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__THREADING_PROFILE);
		createEReference(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__POOLING_PROFILE);
		createEReference(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__QUEUE_PROFILE);
		createEReference(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__EXCEPTION_STRATEGY);
		createEReference(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__PROPERTIES);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__CONTAINER_MANAGED);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__IMPLEMENTATION);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__INBOUND_ENDPOINT);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__INBOUND_TRANSFORMER);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__INITIAL_STATE);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__NAME);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__OUTBOUND_ENDPOINT);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__OUTBOUND_TRANSFORMER);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__REF);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__RESPONSE_TRANSFORMER);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__SINGLETON);
		createEAttribute(muleDescriptorTypeEClass, MULE_DESCRIPTOR_TYPE__VERSION);

		muleEnvironmentPropertiesTypeEClass = createEClass(MULE_ENVIRONMENT_PROPERTIES_TYPE);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__MIXED);
		createEReference(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__THREADING_PROFILE);
		createEReference(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__POOLING_PROFILE);
		createEReference(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__QUEUE_PROFILE);
		createEReference(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__PERSISTENCE_STRATEGY);
		createEReference(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__CONNECTION_STRATEGY);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__CLIENT_MODE);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__EMBEDDED);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__ENABLE_MESSAGE_EVENTS);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__ENCODING);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__MODEL);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__RECOVERABLE_MODE);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__REMOTE_SYNC);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__SERVER_URL);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__SYNCHRONOUS_EVENT_TIMEOUT);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__TRANSACTION_TIMEOUT);
		createEAttribute(muleEnvironmentPropertiesTypeEClass, MULE_ENVIRONMENT_PROPERTIES_TYPE__WORKING_DIRECTORY);

		outboundRouterTypeEClass = createEClass(OUTBOUND_ROUTER_TYPE);
		createEAttribute(outboundRouterTypeEClass, OUTBOUND_ROUTER_TYPE__MIXED);
		createEReference(outboundRouterTypeEClass, OUTBOUND_ROUTER_TYPE__CATCH_ALL_STRATEGY);
		createEReference(outboundRouterTypeEClass, OUTBOUND_ROUTER_TYPE__ROUTER);
		createEAttribute(outboundRouterTypeEClass, OUTBOUND_ROUTER_TYPE__MATCH_ALL);

		persistenceStrategyTypeEClass = createEClass(PERSISTENCE_STRATEGY_TYPE);
		createEAttribute(persistenceStrategyTypeEClass, PERSISTENCE_STRATEGY_TYPE__MIXED);
		createEReference(persistenceStrategyTypeEClass, PERSISTENCE_STRATEGY_TYPE__PROPERTIES);
		createEAttribute(persistenceStrategyTypeEClass, PERSISTENCE_STRATEGY_TYPE__CLASS_NAME);

		poolingProfileTypeEClass = createEClass(POOLING_PROFILE_TYPE);
		createEAttribute(poolingProfileTypeEClass, POOLING_PROFILE_TYPE__MIXED);
		createEAttribute(poolingProfileTypeEClass, POOLING_PROFILE_TYPE__EXHAUSTED_ACTION);
		createEAttribute(poolingProfileTypeEClass, POOLING_PROFILE_TYPE__FACTORY);
		createEAttribute(poolingProfileTypeEClass, POOLING_PROFILE_TYPE__INITIALISATION_POLICY);
		createEAttribute(poolingProfileTypeEClass, POOLING_PROFILE_TYPE__MAX_ACTIVE);
		createEAttribute(poolingProfileTypeEClass, POOLING_PROFILE_TYPE__MAX_IDLE);
		createEAttribute(poolingProfileTypeEClass, POOLING_PROFILE_TYPE__MAX_WAIT);

		propertiesTypeEClass = createEClass(PROPERTIES_TYPE);
		createEAttribute(propertiesTypeEClass, PROPERTIES_TYPE__MIXED);
		createEAttribute(propertiesTypeEClass, PROPERTIES_TYPE__GROUP);
		createEReference(propertiesTypeEClass, PROPERTIES_TYPE__PROPERTY);
		createEReference(propertiesTypeEClass, PROPERTIES_TYPE__FACTORY_PROPERTY);
		createEReference(propertiesTypeEClass, PROPERTIES_TYPE__CONTAINER_PROPERTY);
		createEReference(propertiesTypeEClass, PROPERTIES_TYPE__SYSTEM_PROPERTY);
		createEReference(propertiesTypeEClass, PROPERTIES_TYPE__MAP);
		createEReference(propertiesTypeEClass, PROPERTIES_TYPE__LIST);
		createEReference(propertiesTypeEClass, PROPERTIES_TYPE__FILE_PROPERTIES);
		createEReference(propertiesTypeEClass, PROPERTIES_TYPE__TEXT_PROPERTY);

		propertyTypeEClass = createEClass(PROPERTY_TYPE);
		createEAttribute(propertyTypeEClass, PROPERTY_TYPE__MIXED);
		createEAttribute(propertyTypeEClass, PROPERTY_TYPE__NAME);
		createEAttribute(propertyTypeEClass, PROPERTY_TYPE__VALUE);

		queueProfileTypeEClass = createEClass(QUEUE_PROFILE_TYPE);
		createEAttribute(queueProfileTypeEClass, QUEUE_PROFILE_TYPE__MIXED);
		createEReference(queueProfileTypeEClass, QUEUE_PROFILE_TYPE__PROPERTIES);
		createEAttribute(queueProfileTypeEClass, QUEUE_PROFILE_TYPE__MAX_OUTSTANDING_MESSAGES);
		createEAttribute(queueProfileTypeEClass, QUEUE_PROFILE_TYPE__PERSISTENT);

		replyToTypeEClass = createEClass(REPLY_TO_TYPE);
		createEAttribute(replyToTypeEClass, REPLY_TO_TYPE__MIXED);
		createEAttribute(replyToTypeEClass, REPLY_TO_TYPE__ADDRESS);

		responseRouterTypeEClass = createEClass(RESPONSE_ROUTER_TYPE);
		createEAttribute(responseRouterTypeEClass, RESPONSE_ROUTER_TYPE__MIXED);
		createEReference(responseRouterTypeEClass, RESPONSE_ROUTER_TYPE__ENDPOINT);
		createEReference(responseRouterTypeEClass, RESPONSE_ROUTER_TYPE__GLOBAL_ENDPOINT);
		createEReference(responseRouterTypeEClass, RESPONSE_ROUTER_TYPE__ROUTER);
		createEAttribute(responseRouterTypeEClass, RESPONSE_ROUTER_TYPE__TIMEOUT);

		rightFilterTypeEClass = createEClass(RIGHT_FILTER_TYPE);
		createEAttribute(rightFilterTypeEClass, RIGHT_FILTER_TYPE__MIXED);
		createEReference(rightFilterTypeEClass, RIGHT_FILTER_TYPE__PROPERTIES);
		createEReference(rightFilterTypeEClass, RIGHT_FILTER_TYPE__FILTER);
		createEReference(rightFilterTypeEClass, RIGHT_FILTER_TYPE__LEFT_FILTER);
		createEReference(rightFilterTypeEClass, RIGHT_FILTER_TYPE__RIGHT_FILTER);
		createEAttribute(rightFilterTypeEClass, RIGHT_FILTER_TYPE__CLASS_NAME);
		createEAttribute(rightFilterTypeEClass, RIGHT_FILTER_TYPE__CONFIG_FILE);
		createEAttribute(rightFilterTypeEClass, RIGHT_FILTER_TYPE__EXPECTED_TYPE);
		createEAttribute(rightFilterTypeEClass, RIGHT_FILTER_TYPE__EXPRESSION);
		createEAttribute(rightFilterTypeEClass, RIGHT_FILTER_TYPE__PATH);
		createEAttribute(rightFilterTypeEClass, RIGHT_FILTER_TYPE__PATTERN);

		routerTypeEClass = createEClass(ROUTER_TYPE);
		createEAttribute(routerTypeEClass, ROUTER_TYPE__MIXED);
		createEReference(routerTypeEClass, ROUTER_TYPE__ENDPOINT);
		createEReference(routerTypeEClass, ROUTER_TYPE__GLOBAL_ENDPOINT);
		createEReference(routerTypeEClass, ROUTER_TYPE__REPLY_TO);
		createEReference(routerTypeEClass, ROUTER_TYPE__TRANSACTION);
		createEReference(routerTypeEClass, ROUTER_TYPE__FILTER);
		createEReference(routerTypeEClass, ROUTER_TYPE__PROPERTIES);
		createEAttribute(routerTypeEClass, ROUTER_TYPE__CLASS_NAME);
		createEAttribute(routerTypeEClass, ROUTER_TYPE__ENABLE_CORRELATION);
		createEAttribute(routerTypeEClass, ROUTER_TYPE__PROPERTY_EXTRACTOR);

		securityFilterTypeEClass = createEClass(SECURITY_FILTER_TYPE);
		createEAttribute(securityFilterTypeEClass, SECURITY_FILTER_TYPE__MIXED);
		createEReference(securityFilterTypeEClass, SECURITY_FILTER_TYPE__PROPERTIES);
		createEAttribute(securityFilterTypeEClass, SECURITY_FILTER_TYPE__CLASS_NAME);
		createEAttribute(securityFilterTypeEClass, SECURITY_FILTER_TYPE__USE_PROVIDERS);

		securityManagerTypeEClass = createEClass(SECURITY_MANAGER_TYPE);
		createEAttribute(securityManagerTypeEClass, SECURITY_MANAGER_TYPE__MIXED);
		createEReference(securityManagerTypeEClass, SECURITY_MANAGER_TYPE__SECURITY_PROVIDER);
		createEReference(securityManagerTypeEClass, SECURITY_MANAGER_TYPE__ENCRYPTION_STRATEGY);
		createEAttribute(securityManagerTypeEClass, SECURITY_MANAGER_TYPE__CLASS_NAME);
		createEAttribute(securityManagerTypeEClass, SECURITY_MANAGER_TYPE__REF);

		securityProviderTypeEClass = createEClass(SECURITY_PROVIDER_TYPE);
		createEAttribute(securityProviderTypeEClass, SECURITY_PROVIDER_TYPE__MIXED);
		createEReference(securityProviderTypeEClass, SECURITY_PROVIDER_TYPE__PROPERTIES);
		createEAttribute(securityProviderTypeEClass, SECURITY_PROVIDER_TYPE__CLASS_NAME);
		createEAttribute(securityProviderTypeEClass, SECURITY_PROVIDER_TYPE__NAME);
		createEAttribute(securityProviderTypeEClass, SECURITY_PROVIDER_TYPE__REF);

		systemEntryTypeEClass = createEClass(SYSTEM_ENTRY_TYPE);
		createEAttribute(systemEntryTypeEClass, SYSTEM_ENTRY_TYPE__MIXED);
		createEAttribute(systemEntryTypeEClass, SYSTEM_ENTRY_TYPE__DEFAULT_VALUE);
		createEAttribute(systemEntryTypeEClass, SYSTEM_ENTRY_TYPE__KEY);

		systemPropertyTypeEClass = createEClass(SYSTEM_PROPERTY_TYPE);
		createEAttribute(systemPropertyTypeEClass, SYSTEM_PROPERTY_TYPE__MIXED);
		createEAttribute(systemPropertyTypeEClass, SYSTEM_PROPERTY_TYPE__DEFAULT_VALUE);
		createEAttribute(systemPropertyTypeEClass, SYSTEM_PROPERTY_TYPE__KEY);
		createEAttribute(systemPropertyTypeEClass, SYSTEM_PROPERTY_TYPE__NAME);

		textPropertyTypeEClass = createEClass(TEXT_PROPERTY_TYPE);
		createEAttribute(textPropertyTypeEClass, TEXT_PROPERTY_TYPE__VALUE);
		createEAttribute(textPropertyTypeEClass, TEXT_PROPERTY_TYPE__NAME);

		threadingProfileTypeEClass = createEClass(THREADING_PROFILE_TYPE);
		createEAttribute(threadingProfileTypeEClass, THREADING_PROFILE_TYPE__MIXED);
		createEAttribute(threadingProfileTypeEClass, THREADING_PROFILE_TYPE__DO_THREADING);
		createEAttribute(threadingProfileTypeEClass, THREADING_PROFILE_TYPE__ID);
		createEAttribute(threadingProfileTypeEClass, THREADING_PROFILE_TYPE__MAX_BUFFER_SIZE);
		createEAttribute(threadingProfileTypeEClass, THREADING_PROFILE_TYPE__MAX_THREADS_ACTIVE);
		createEAttribute(threadingProfileTypeEClass, THREADING_PROFILE_TYPE__MAX_THREADS_IDLE);
		createEAttribute(threadingProfileTypeEClass, THREADING_PROFILE_TYPE__POOL_EXHAUSTED_ACTION);
		createEAttribute(threadingProfileTypeEClass, THREADING_PROFILE_TYPE__THREAD_TTL);
		createEAttribute(threadingProfileTypeEClass, THREADING_PROFILE_TYPE__THREAD_WAIT_TIMEOUT);

		transactionManagerTypeEClass = createEClass(TRANSACTION_MANAGER_TYPE);
		createEAttribute(transactionManagerTypeEClass, TRANSACTION_MANAGER_TYPE__MIXED);
		createEReference(transactionManagerTypeEClass, TRANSACTION_MANAGER_TYPE__PROPERTIES);
		createEAttribute(transactionManagerTypeEClass, TRANSACTION_MANAGER_TYPE__FACTORY);
		createEAttribute(transactionManagerTypeEClass, TRANSACTION_MANAGER_TYPE__REF);

		transactionTypeEClass = createEClass(TRANSACTION_TYPE);
		createEAttribute(transactionTypeEClass, TRANSACTION_TYPE__MIXED);
		createEReference(transactionTypeEClass, TRANSACTION_TYPE__CONSTRAINT);
		createEAttribute(transactionTypeEClass, TRANSACTION_TYPE__ACTION);
		createEAttribute(transactionTypeEClass, TRANSACTION_TYPE__FACTORY);
		createEAttribute(transactionTypeEClass, TRANSACTION_TYPE__TIMEOUT);

		transformersTypeEClass = createEClass(TRANSFORMERS_TYPE);
		createEAttribute(transformersTypeEClass, TRANSFORMERS_TYPE__MIXED);
		createEReference(transformersTypeEClass, TRANSFORMERS_TYPE__TRANSFORMER);

		transformerTypeEClass = createEClass(TRANSFORMER_TYPE);
		createEAttribute(transformerTypeEClass, TRANSFORMER_TYPE__MIXED);
		createEReference(transformerTypeEClass, TRANSFORMER_TYPE__PROPERTIES);
		createEAttribute(transformerTypeEClass, TRANSFORMER_TYPE__CLASS_NAME);
		createEAttribute(transformerTypeEClass, TRANSFORMER_TYPE__IGNORE_BAD_INPUT);
		createEAttribute(transformerTypeEClass, TRANSFORMER_TYPE__NAME);
		createEAttribute(transformerTypeEClass, TRANSFORMER_TYPE__REF);
		createEAttribute(transformerTypeEClass, TRANSFORMER_TYPE__RETURN_CLASS);

		// Create enums
		actionTypeEEnum = createEEnum(ACTION_TYPE);
		createConnectorTypeEEnum = createEEnum(CREATE_CONNECTOR_TYPE);
		enableCorrelationTypeEEnum = createEEnum(ENABLE_CORRELATION_TYPE);
		exhaustedActionTypeEEnum = createEEnum(EXHAUSTED_ACTION_TYPE);
		idTypeEEnum = createEEnum(ID_TYPE);
		initialisationPolicyTypeEEnum = createEEnum(INITIALISATION_POLICY_TYPE);
		initialStateTypeEEnum = createEEnum(INITIAL_STATE_TYPE);
		poolExhaustedActionTypeEEnum = createEEnum(POOL_EXHAUSTED_ACTION_TYPE);
		typeType1EEnum = createEEnum(TYPE_TYPE1);

		// Create data types
		actionTypeObjectEDataType = createEDataType(ACTION_TYPE_OBJECT);
		createConnectorTypeObjectEDataType = createEDataType(CREATE_CONNECTOR_TYPE_OBJECT);
		enableCorrelationTypeObjectEDataType = createEDataType(ENABLE_CORRELATION_TYPE_OBJECT);
		exhaustedActionTypeObjectEDataType = createEDataType(EXHAUSTED_ACTION_TYPE_OBJECT);
		idTypeObjectEDataType = createEDataType(ID_TYPE_OBJECT);
		initialisationPolicyTypeObjectEDataType = createEDataType(INITIALISATION_POLICY_TYPE_OBJECT);
		initialStateTypeObjectEDataType = createEDataType(INITIAL_STATE_TYPE_OBJECT);
		poolExhaustedActionTypeObjectEDataType = createEDataType(POOL_EXHAUSTED_ACTION_TYPE_OBJECT);
		typeTypeEDataType = createEDataType(TYPE_TYPE);
		typeTypeObjectEDataType = createEDataType(TYPE_TYPE_OBJECT);
		versionTypeEDataType = createEDataType(VERSION_TYPE);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Obtain other dependent packages
		XMLTypePackageImpl theXMLTypePackage = (XMLTypePackageImpl)EPackage.Registry.INSTANCE.getEPackage(XMLTypePackage.eNS_URI);

		// Add supertypes to classes

		// Initialize classes and features; add operations and parameters
		initEClass(agentsTypeEClass, AgentsType.class, "AgentsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getAgentsType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, AgentsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getAgentsType_Agent(), this.getAgentType(), null, "agent", null, 1, -1, AgentsType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(agentTypeEClass, AgentType.class, "AgentType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getAgentType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, AgentType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getAgentType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, AgentType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getAgentType_ClassName(), theXMLTypePackage.getString(), "className", null, 0, 1, AgentType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAgentType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, AgentType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAgentType_Ref(), theXMLTypePackage.getString(), "ref", null, 0, 1, AgentType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(catchAllStrategyTypeEClass, CatchAllStrategyType.class, "CatchAllStrategyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getCatchAllStrategyType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, CatchAllStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getCatchAllStrategyType_Endpoint(), this.getEndpointType(), null, "endpoint", null, 0, 1, CatchAllStrategyType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getCatchAllStrategyType_GlobalEndpoint(), this.getGlobalEndpointType(), null, "globalEndpoint", null, 0, 1, CatchAllStrategyType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getCatchAllStrategyType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, CatchAllStrategyType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getCatchAllStrategyType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, CatchAllStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(componentFactoryTypeEClass, ComponentFactoryType.class, "ComponentFactoryType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getComponentFactoryType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ComponentFactoryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentFactoryType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, ComponentFactoryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(componentLifecycleAdapterFactoryTypeEClass, ComponentLifecycleAdapterFactoryType.class, "ComponentLifecycleAdapterFactoryType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getComponentLifecycleAdapterFactoryType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ComponentLifecycleAdapterFactoryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentLifecycleAdapterFactoryType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, ComponentLifecycleAdapterFactoryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(componentPoolFactoryTypeEClass, ComponentPoolFactoryType.class, "ComponentPoolFactoryType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getComponentPoolFactoryType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ComponentPoolFactoryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getComponentPoolFactoryType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, ComponentPoolFactoryType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getComponentPoolFactoryType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, ComponentPoolFactoryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(connectionStrategyTypeEClass, ConnectionStrategyType.class, "ConnectionStrategyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getConnectionStrategyType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ConnectionStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getConnectionStrategyType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, ConnectionStrategyType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getConnectionStrategyType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, ConnectionStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(connectorTypeEClass, ConnectorType.class, "ConnectorType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getConnectorType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ConnectorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getConnectorType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, ConnectorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getConnectorType_ThreadingProfile(), this.getThreadingProfileType(), null, "threadingProfile", null, 0, 1, ConnectorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getConnectorType_ExceptionStrategy(), this.getExceptionStrategyType(), null, "exceptionStrategy", null, 0, 1, ConnectorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getConnectorType_ConnectionStrategy(), this.getConnectionStrategyType(), null, "connectionStrategy", null, 0, 1, ConnectorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getConnectorType_ClassName(), theXMLTypePackage.getString(), "className", null, 0, 1, ConnectorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConnectorType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, ConnectorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConnectorType_Ref(), theXMLTypePackage.getString(), "ref", null, 0, 1, ConnectorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(constraintTypeEClass, ConstraintType.class, "ConstraintType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getConstraintType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ConstraintType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getConstraintType_LeftFilter(), this.getLeftFilterType(), null, "leftFilter", null, 0, 1, ConstraintType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getConstraintType_RightFilter(), this.getRightFilterType(), null, "rightFilter", null, 0, 1, ConstraintType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getConstraintType_Filter(), this.getFilterType(), null, "filter", null, 0, 1, ConstraintType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getConstraintType_BatchSize(), theXMLTypePackage.getString(), "batchSize", null, 0, 1, ConstraintType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConstraintType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, ConstraintType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConstraintType_ExpectedType(), theXMLTypePackage.getString(), "expectedType", null, 0, 1, ConstraintType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConstraintType_Expression(), theXMLTypePackage.getString(), "expression", null, 0, 1, ConstraintType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConstraintType_Frequency(), theXMLTypePackage.getString(), "frequency", null, 0, 1, ConstraintType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConstraintType_Path(), theXMLTypePackage.getString(), "path", null, 0, 1, ConstraintType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getConstraintType_Pattern(), theXMLTypePackage.getString(), "pattern", null, 0, 1, ConstraintType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(containerContextTypeEClass, ContainerContextType.class, "ContainerContextType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getContainerContextType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ContainerContextType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getContainerContextType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, ContainerContextType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getContainerContextType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, ContainerContextType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getContainerContextType_Name(), theXMLTypePackage.getString(), "name", null, 0, 1, ContainerContextType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(containerEntryTypeEClass, ContainerEntryType.class, "ContainerEntryType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getContainerEntryType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ContainerEntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getContainerEntryType_Reference(), theXMLTypePackage.getString(), "reference", null, 1, 1, ContainerEntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getContainerEntryType_Required(), theXMLTypePackage.getBoolean(), "required", "true", 0, 1, ContainerEntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(containerPropertyTypeEClass, ContainerPropertyType.class, "ContainerPropertyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getContainerPropertyType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ContainerPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getContainerPropertyType_Container(), theXMLTypePackage.getString(), "container", null, 0, 1, ContainerPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getContainerPropertyType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, ContainerPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getContainerPropertyType_Reference(), theXMLTypePackage.getString(), "reference", null, 1, 1, ContainerPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getContainerPropertyType_Required(), theXMLTypePackage.getBoolean(), "required", "true", 0, 1, ContainerPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(documentRootEClass, DocumentRoot.class, "DocumentRoot", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getDocumentRoot_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, null, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getDocumentRoot_XMLNSPrefixMap(), ecorePackage.getEStringToStringMapEntry(), null, "xMLNSPrefixMap", null, 0, -1, null, IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getDocumentRoot_XSISchemaLocation(), ecorePackage.getEStringToStringMapEntry(), null, "xSISchemaLocation", null, 0, -1, null, IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getDocumentRoot_MuleConfiguration(), this.getMuleConfigurationType(), null, "muleConfiguration", null, 0, -2, null, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(encryptionStrategyTypeEClass, EncryptionStrategyType.class, "EncryptionStrategyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEncryptionStrategyType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, EncryptionStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getEncryptionStrategyType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, EncryptionStrategyType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getEncryptionStrategyType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, EncryptionStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEncryptionStrategyType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, EncryptionStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(endpointIdentifiersTypeEClass, EndpointIdentifiersType.class, "EndpointIdentifiersType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEndpointIdentifiersType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, EndpointIdentifiersType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getEndpointIdentifiersType_EndpointIdentifier(), this.getEndpointIdentifierType(), null, "endpointIdentifier", null, 1, -1, EndpointIdentifiersType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(endpointIdentifierTypeEClass, EndpointIdentifierType.class, "EndpointIdentifierType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEndpointIdentifierType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, EndpointIdentifierType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointIdentifierType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, EndpointIdentifierType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointIdentifierType_Value(), theXMLTypePackage.getString(), "value", null, 1, 1, EndpointIdentifierType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(endpointTypeEClass, EndpointType.class, "EndpointType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEndpointType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getEndpointType_Transaction(), this.getTransactionType(), null, "transaction", null, 0, 1, EndpointType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getEndpointType_Filter(), this.getFilterType(), null, "filter", null, 0, 1, EndpointType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getEndpointType_SecurityFilter(), this.getSecurityFilterType(), null, "securityFilter", null, 0, 1, EndpointType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getEndpointType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, EndpointType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_Address(), theXMLTypePackage.getString(), "address", null, 1, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_Connector(), theXMLTypePackage.getString(), "connector", null, 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_CreateConnector(), this.getCreateConnectorType(), "createConnector", "GET_OR_CREATE", 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_Name(), theXMLTypePackage.getString(), "name", null, 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_Ref(), theXMLTypePackage.getString(), "ref", null, 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_RemoteSync(), theXMLTypePackage.getBoolean(), "remoteSync", null, 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_RemoteSyncTimeout(), theXMLTypePackage.getString(), "remoteSyncTimeout", null, 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_ResponseTransformers(), theXMLTypePackage.getIDREFS(), "responseTransformers", null, 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_Synchronous(), theXMLTypePackage.getBoolean(), "synchronous", null, 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_Transformers(), theXMLTypePackage.getString(), "transformers", null, 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEndpointType_Type(), this.getTypeType1(), "type", "senderAndReceiver", 0, 1, EndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(entryPointResolverTypeEClass, EntryPointResolverType.class, "EntryPointResolverType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEntryPointResolverType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, EntryPointResolverType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEntryPointResolverType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, EntryPointResolverType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(entryTypeEClass, EntryType.class, "EntryType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEntryType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, EntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEntryType_Value(), theXMLTypePackage.getString(), "value", null, 1, 1, EntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(environmentPropertiesTypeEClass, EnvironmentPropertiesType.class, "EnvironmentPropertiesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEnvironmentPropertiesType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, EnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEnvironmentPropertiesType_Group(), ecorePackage.getEFeatureMapEntry(), "group", null, 0, -1, EnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getEnvironmentPropertiesType_Property(), this.getPropertyType(), null, "property", null, 0, -1, EnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getEnvironmentPropertiesType_FactoryProperty(), this.getFactoryPropertyType(), null, "factoryProperty", null, 0, -1, EnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getEnvironmentPropertiesType_SystemProperty(), this.getSystemPropertyType(), null, "systemProperty", null, 0, -1, EnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getEnvironmentPropertiesType_Map(), this.getMapType(), null, "map", null, 0, -1, EnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getEnvironmentPropertiesType_List(), this.getListType(), null, "list", null, 0, -1, EnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getEnvironmentPropertiesType_FileProperties(), this.getFilePropertiesType(), null, "fileProperties", null, 0, -1, EnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(exceptionStrategyTypeEClass, ExceptionStrategyType.class, "ExceptionStrategyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getExceptionStrategyType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ExceptionStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getExceptionStrategyType_Endpoint(), this.getEndpointType(), null, "endpoint", null, 0, -1, ExceptionStrategyType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getExceptionStrategyType_GlobalEndpoint(), this.getGlobalEndpointType(), null, "globalEndpoint", null, 0, -1, ExceptionStrategyType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getExceptionStrategyType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, ExceptionStrategyType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getExceptionStrategyType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, ExceptionStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(factoryEntryTypeEClass, FactoryEntryType.class, "FactoryEntryType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFactoryEntryType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, FactoryEntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFactoryEntryType_Factory(), theXMLTypePackage.getString(), "factory", null, 1, 1, FactoryEntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(factoryPropertyTypeEClass, FactoryPropertyType.class, "FactoryPropertyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFactoryPropertyType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, FactoryPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFactoryPropertyType_Factory(), theXMLTypePackage.getString(), "factory", null, 1, 1, FactoryPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFactoryPropertyType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, FactoryPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(filePropertiesTypeEClass, FilePropertiesType.class, "FilePropertiesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFilePropertiesType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, FilePropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFilePropertiesType_Location(), theXMLTypePackage.getString(), "location", null, 1, 1, FilePropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFilePropertiesType_Override(), theXMLTypePackage.getBoolean(), "override", "true", 0, 1, FilePropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(filterTypeEClass, FilterType.class, "FilterType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFilterType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, FilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getFilterType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, FilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getFilterType_Filter(), this.getFilterType(), null, "filter", null, 0, 1, FilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getFilterType_LeftFilter(), this.getLeftFilterType(), null, "leftFilter", null, 0, 1, FilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getFilterType_RightFilter(), this.getRightFilterType(), null, "rightFilter", null, 0, 1, FilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getFilterType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, FilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFilterType_ConfigFile(), theXMLTypePackage.getString(), "configFile", null, 0, 1, FilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFilterType_ExpectedType(), theXMLTypePackage.getString(), "expectedType", null, 0, 1, FilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFilterType_Expression(), theXMLTypePackage.getString(), "expression", null, 0, 1, FilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFilterType_Path(), theXMLTypePackage.getString(), "path", null, 0, 1, FilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFilterType_Pattern(), theXMLTypePackage.getString(), "pattern", null, 0, 1, FilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(globalEndpointsTypeEClass, GlobalEndpointsType.class, "GlobalEndpointsType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getGlobalEndpointsType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, GlobalEndpointsType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getGlobalEndpointsType_Endpoint(), this.getEndpointType(), null, "endpoint", null, 1, -1, GlobalEndpointsType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(globalEndpointTypeEClass, GlobalEndpointType.class, "GlobalEndpointType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getGlobalEndpointType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, GlobalEndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getGlobalEndpointType_Transaction(), this.getTransactionType(), null, "transaction", null, 0, 1, GlobalEndpointType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getGlobalEndpointType_Filter(), this.getFilterType(), null, "filter", null, 0, 1, GlobalEndpointType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getGlobalEndpointType_SecurityFilter(), this.getSecurityFilterType(), null, "securityFilter", null, 0, 1, GlobalEndpointType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getGlobalEndpointType_Properties(), this.getPropertiesType(), null, "properties", null, 0, -1, GlobalEndpointType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getGlobalEndpointType_Address(), theXMLTypePackage.getString(), "address", null, 0, 1, GlobalEndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGlobalEndpointType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, GlobalEndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGlobalEndpointType_RemoteSync(), theXMLTypePackage.getBoolean(), "remoteSync", null, 0, 1, GlobalEndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGlobalEndpointType_RemoteSyncTimeout(), theXMLTypePackage.getString(), "remoteSyncTimeout", null, 0, 1, GlobalEndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGlobalEndpointType_ResponseTransformers(), theXMLTypePackage.getString(), "responseTransformers", null, 0, 1, GlobalEndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGlobalEndpointType_Synchronous(), theXMLTypePackage.getBoolean(), "synchronous", null, 0, 1, GlobalEndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGlobalEndpointType_Transformers(), theXMLTypePackage.getString(), "transformers", null, 0, 1, GlobalEndpointType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(inboundRouterTypeEClass, InboundRouterType.class, "InboundRouterType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getInboundRouterType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, InboundRouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInboundRouterType_CatchAllStrategy(), this.getCatchAllStrategyType(), null, "catchAllStrategy", null, 0, 1, InboundRouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getInboundRouterType_Endpoint(), this.getEndpointType(), null, "endpoint", null, 0, -1, InboundRouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getInboundRouterType_GlobalEndpoint(), this.getGlobalEndpointType(), null, "globalEndpoint", null, 0, -1, InboundRouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getInboundRouterType_Router(), this.getRouterType(), null, "router", null, 0, -1, InboundRouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getInboundRouterType_MatchAll(), theXMLTypePackage.getBoolean(), "matchAll", "false", 0, 1, InboundRouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(interceptorStackTypeEClass, InterceptorStackType.class, "InterceptorStackType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getInterceptorStackType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, InterceptorStackType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInterceptorStackType_Interceptor(), this.getInterceptorType(), null, "interceptor", null, 1, -1, InterceptorStackType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getInterceptorStackType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, InterceptorStackType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(interceptorTypeEClass, InterceptorType.class, "InterceptorType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getInterceptorType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, InterceptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getInterceptorType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, InterceptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getInterceptorType_ClassName(), theXMLTypePackage.getString(), "className", null, 0, 1, InterceptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getInterceptorType_Name(), theXMLTypePackage.getString(), "name", null, 0, 1, InterceptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(leftFilterTypeEClass, LeftFilterType.class, "LeftFilterType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getLeftFilterType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, LeftFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getLeftFilterType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, LeftFilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getLeftFilterType_Filter(), this.getFilterType(), null, "filter", null, 0, 1, LeftFilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getLeftFilterType_LeftFilter(), this.getLeftFilterType(), null, "leftFilter", null, 0, 1, LeftFilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getLeftFilterType_RightFilter(), this.getRightFilterType(), null, "rightFilter", null, 0, 1, LeftFilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getLeftFilterType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, LeftFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getLeftFilterType_ConfigFile(), theXMLTypePackage.getString(), "configFile", null, 0, 1, LeftFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getLeftFilterType_ExpectedType(), theXMLTypePackage.getString(), "expectedType", null, 0, 1, LeftFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getLeftFilterType_Expression(), theXMLTypePackage.getString(), "expression", null, 0, 1, LeftFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getLeftFilterType_Path(), theXMLTypePackage.getString(), "path", null, 0, 1, LeftFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getLeftFilterType_Pattern(), theXMLTypePackage.getString(), "pattern", null, 0, 1, LeftFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(listTypeEClass, ListType.class, "ListType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getListType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ListType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getListType_Group(), ecorePackage.getEFeatureMapEntry(), "group", null, 0, -1, ListType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getListType_Entry(), this.getEntryType(), null, "entry", null, 0, -1, ListType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getListType_FactoryEntry(), this.getFactoryEntryType(), null, "factoryEntry", null, 0, -1, ListType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getListType_SystemEntry(), this.getSystemEntryType(), null, "systemEntry", null, 0, -1, ListType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getListType_ContainerEntry(), this.getContainerEntryType(), null, "containerEntry", null, 0, -1, ListType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getListType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, ListType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(mapTypeEClass, MapType.class, "MapType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMapType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, MapType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMapType_Group(), ecorePackage.getEFeatureMapEntry(), "group", null, 0, -1, MapType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMapType_Property(), this.getPropertyType(), null, "property", null, 0, -1, MapType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMapType_FactoryProperty(), this.getFactoryPropertyType(), null, "factoryProperty", null, 0, -1, MapType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMapType_ContainerProperty(), this.getContainerPropertyType(), null, "containerProperty", null, 0, -1, MapType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMapType_SystemProperty(), this.getSystemPropertyType(), null, "systemProperty", null, 0, -1, MapType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMapType_Map(), this.getMapType(), null, "map", null, 0, -1, MapType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMapType_List(), this.getListType(), null, "list", null, 0, -1, MapType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMapType_FileProperties(), this.getFilePropertiesType(), null, "fileProperties", null, 0, -1, MapType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getMapType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, MapType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(modelTypeEClass, ModelType.class, "ModelType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getModelType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ModelType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getModelType_Description(), theXMLTypePackage.getString(), "description", null, 0, 1, ModelType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getModelType_EntryPointResolver(), this.getEntryPointResolverType(), null, "entryPointResolver", null, 0, 1, ModelType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getModelType_ComponentFactory(), this.getComponentFactoryType(), null, "componentFactory", null, 0, 1, ModelType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getModelType_ComponentLifecycleAdapterFactory(), this.getComponentLifecycleAdapterFactoryType(), null, "componentLifecycleAdapterFactory", null, 0, 1, ModelType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getModelType_ComponentPoolFactory(), this.getComponentPoolFactoryType(), null, "componentPoolFactory", null, 0, 1, ModelType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getModelType_ExceptionStrategy(), this.getExceptionStrategyType(), null, "exceptionStrategy", null, 0, 1, ModelType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getModelType_MuleDescriptor(), this.getMuleDescriptorType(), null, "muleDescriptor", null, 0, -1, ModelType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getModelType_ClassName(), theXMLTypePackage.getString(), "className", null, 0, 1, ModelType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getModelType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, ModelType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getModelType_Ref(), theXMLTypePackage.getString(), "ref", null, 0, 1, ModelType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getModelType_Type(), this.getTypeType(), "type", "seda", 0, 1, ModelType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(muleConfigurationTypeEClass, MuleConfigurationType.class, "MuleConfigurationType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMuleConfigurationType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, MuleConfigurationType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleConfigurationType_Description(), theXMLTypePackage.getString(), "description", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_EnvironmentProperties(), this.getEnvironmentPropertiesType(), null, "environmentProperties", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_MuleEnvironmentProperties(), this.getMuleEnvironmentPropertiesType(), null, "muleEnvironmentProperties", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_ContainerContext(), this.getContainerContextType(), null, "containerContext", null, 0, -1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_SecurityManager(), this.getSecurityManagerType(), null, "securityManager", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_TransactionManager(), this.getTransactionManagerType(), null, "transactionManager", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_Agents(), this.getAgentsType(), null, "agents", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_Connector(), this.getConnectorType(), null, "connector", null, 0, -1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_EndpointIdentifiers(), this.getEndpointIdentifiersType(), null, "endpointIdentifiers", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_Transformers(), this.getTransformersType(), null, "transformers", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_GlobalEndpoints(), this.getGlobalEndpointsType(), null, "globalEndpoints", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_InterceptorStack(), this.getInterceptorStackType(), null, "interceptorStack", null, 0, -1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_Model(), this.getModelType(), null, "model", null, 0, 1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleConfigurationType_MuleDescriptor(), this.getMuleDescriptorType(), null, "muleDescriptor", null, 0, -1, MuleConfigurationType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleConfigurationType_Id(), theXMLTypePackage.getString(), "id", null, 0, 1, MuleConfigurationType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleConfigurationType_Version(), this.getVersionType(), "version", null, 1, 1, MuleConfigurationType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(muleDescriptorTypeEClass, MuleDescriptorType.class, "MuleDescriptorType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMuleDescriptorType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getMuleDescriptorType_InboundRouter(), this.getInboundRouterType(), null, "inboundRouter", null, 0, 1, MuleDescriptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleDescriptorType_OutboundRouter(), this.getOutboundRouterType(), null, "outboundRouter", null, 0, 1, MuleDescriptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleDescriptorType_ResponseRouter(), this.getResponseRouterType(), null, "responseRouter", null, 0, 1, MuleDescriptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleDescriptorType_Interceptor(), this.getInterceptorType(), null, "interceptor", null, 0, -1, MuleDescriptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleDescriptorType_ThreadingProfile(), this.getThreadingProfileType(), null, "threadingProfile", null, 0, 1, MuleDescriptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleDescriptorType_PoolingProfile(), this.getPoolingProfileType(), null, "poolingProfile", null, 0, 1, MuleDescriptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleDescriptorType_QueueProfile(), this.getQueueProfileType(), null, "queueProfile", null, 0, 1, MuleDescriptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleDescriptorType_ExceptionStrategy(), this.getExceptionStrategyType(), null, "exceptionStrategy", null, 0, 1, MuleDescriptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleDescriptorType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, MuleDescriptorType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_ContainerManaged(), theXMLTypePackage.getBoolean(), "containerManaged", "true", 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_Implementation(), theXMLTypePackage.getString(), "implementation", null, 1, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_InboundEndpoint(), theXMLTypePackage.getString(), "inboundEndpoint", null, 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_InboundTransformer(), theXMLTypePackage.getString(), "inboundTransformer", null, 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_InitialState(), this.getInitialStateType(), "initialState", "started", 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_OutboundEndpoint(), theXMLTypePackage.getString(), "outboundEndpoint", null, 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_OutboundTransformer(), theXMLTypePackage.getString(), "outboundTransformer", null, 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_Ref(), theXMLTypePackage.getString(), "ref", null, 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_ResponseTransformer(), theXMLTypePackage.getString(), "responseTransformer", null, 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_Singleton(), theXMLTypePackage.getBoolean(), "singleton", "false", 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleDescriptorType_Version(), theXMLTypePackage.getString(), "version", null, 0, 1, MuleDescriptorType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(muleEnvironmentPropertiesTypeEClass, MuleEnvironmentPropertiesType.class, "MuleEnvironmentPropertiesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMuleEnvironmentPropertiesType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getMuleEnvironmentPropertiesType_ThreadingProfile(), this.getThreadingProfileType(), null, "threadingProfile", null, 0, -1, MuleEnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleEnvironmentPropertiesType_PoolingProfile(), this.getPoolingProfileType(), null, "poolingProfile", null, 0, 1, MuleEnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleEnvironmentPropertiesType_QueueProfile(), this.getQueueProfileType(), null, "queueProfile", null, 0, 1, MuleEnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleEnvironmentPropertiesType_PersistenceStrategy(), this.getPersistenceStrategyType(), null, "persistenceStrategy", null, 0, 1, MuleEnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getMuleEnvironmentPropertiesType_ConnectionStrategy(), this.getConnectionStrategyType(), null, "connectionStrategy", null, 0, 1, MuleEnvironmentPropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_ClientMode(), theXMLTypePackage.getBoolean(), "clientMode", "false", 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_Embedded(), theXMLTypePackage.getBoolean(), "embedded", "false", 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_EnableMessageEvents(), theXMLTypePackage.getBoolean(), "enableMessageEvents", "false", 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_Encoding(), theXMLTypePackage.getString(), "encoding", null, 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_Model(), theXMLTypePackage.getString(), "model", null, 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_RecoverableMode(), theXMLTypePackage.getBoolean(), "recoverableMode", "false", 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_RemoteSync(), theXMLTypePackage.getBoolean(), "remoteSync", "false", 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_ServerUrl(), theXMLTypePackage.getString(), "serverUrl", null, 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_Synchronous(), theXMLTypePackage.getBoolean(), "synchronous", "false", 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_SynchronousEventTimeout(), theXMLTypePackage.getString(), "synchronousEventTimeout", null, 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_TransactionTimeout(), theXMLTypePackage.getString(), "transactionTimeout", null, 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMuleEnvironmentPropertiesType_WorkingDirectory(), theXMLTypePackage.getString(), "workingDirectory", "./.mule", 0, 1, MuleEnvironmentPropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(outboundRouterTypeEClass, OutboundRouterType.class, "OutboundRouterType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getOutboundRouterType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, OutboundRouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOutboundRouterType_CatchAllStrategy(), this.getCatchAllStrategyType(), null, "catchAllStrategy", null, 0, 1, OutboundRouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getOutboundRouterType_Router(), this.getRouterType(), null, "router", null, 1, -1, OutboundRouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getOutboundRouterType_MatchAll(), theXMLTypePackage.getBoolean(), "matchAll", "false", 0, 1, OutboundRouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(persistenceStrategyTypeEClass, PersistenceStrategyType.class, "PersistenceStrategyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getPersistenceStrategyType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, PersistenceStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getPersistenceStrategyType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, PersistenceStrategyType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getPersistenceStrategyType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, PersistenceStrategyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(poolingProfileTypeEClass, PoolingProfileType.class, "PoolingProfileType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getPoolingProfileType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, PoolingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPoolingProfileType_ExhaustedAction(), this.getExhaustedActionType(), "exhaustedAction", "GROW", 0, 1, PoolingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPoolingProfileType_Factory(), theXMLTypePackage.getString(), "factory", "org.mule.config.pool.CommonsPoolFactory", 0, 1, PoolingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPoolingProfileType_InitialisationPolicy(), this.getInitialisationPolicyType(), "initialisationPolicy", "INITIALISE_FIRST", 0, 1, PoolingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPoolingProfileType_MaxActive(), theXMLTypePackage.getString(), "maxActive", null, 0, 1, PoolingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPoolingProfileType_MaxIdle(), theXMLTypePackage.getString(), "maxIdle", null, 0, 1, PoolingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPoolingProfileType_MaxWait(), theXMLTypePackage.getString(), "maxWait", null, 0, 1, PoolingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(propertiesTypeEClass, PropertiesType.class, "PropertiesType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getPropertiesType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, PropertiesType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPropertiesType_Group(), ecorePackage.getEFeatureMapEntry(), "group", null, 0, -1, PropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getPropertiesType_Property(), this.getPropertyType(), null, "property", null, 0, -1, PropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getPropertiesType_FactoryProperty(), this.getFactoryPropertyType(), null, "factoryProperty", null, 0, -1, PropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getPropertiesType_ContainerProperty(), this.getContainerPropertyType(), null, "containerProperty", null, 0, -1, PropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getPropertiesType_SystemProperty(), this.getSystemPropertyType(), null, "systemProperty", null, 0, -1, PropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getPropertiesType_Map(), this.getMapType(), null, "map", null, 0, -1, PropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getPropertiesType_List(), this.getListType(), null, "list", null, 0, -1, PropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getPropertiesType_FileProperties(), this.getFilePropertiesType(), null, "fileProperties", null, 0, -1, PropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getPropertiesType_TextProperty(), this.getTextPropertyType(), null, "textProperty", null, 0, -1, PropertiesType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(propertyTypeEClass, PropertyType.class, "PropertyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getPropertyType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, PropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPropertyType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, PropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPropertyType_Value(), theXMLTypePackage.getString(), "value", null, 1, 1, PropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(queueProfileTypeEClass, QueueProfileType.class, "QueueProfileType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getQueueProfileType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, QueueProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getQueueProfileType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, QueueProfileType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getQueueProfileType_MaxOutstandingMessages(), theXMLTypePackage.getString(), "maxOutstandingMessages", null, 0, 1, QueueProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getQueueProfileType_Persistent(), theXMLTypePackage.getBoolean(), "persistent", "false", 0, 1, QueueProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(replyToTypeEClass, ReplyToType.class, "ReplyToType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getReplyToType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ReplyToType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getReplyToType_Address(), theXMLTypePackage.getString(), "address", null, 1, 1, ReplyToType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(responseRouterTypeEClass, ResponseRouterType.class, "ResponseRouterType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getResponseRouterType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ResponseRouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getResponseRouterType_Endpoint(), this.getEndpointType(), null, "endpoint", null, 0, -1, ResponseRouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getResponseRouterType_GlobalEndpoint(), this.getGlobalEndpointType(), null, "globalEndpoint", null, 0, -1, ResponseRouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getResponseRouterType_Router(), this.getRouterType(), null, "router", null, 0, -1, ResponseRouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getResponseRouterType_Timeout(), theXMLTypePackage.getString(), "timeout", null, 0, 1, ResponseRouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(rightFilterTypeEClass, RightFilterType.class, "RightFilterType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRightFilterType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, RightFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRightFilterType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, RightFilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getRightFilterType_Filter(), this.getFilterType(), null, "filter", null, 0, 1, RightFilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getRightFilterType_LeftFilter(), this.getLeftFilterType(), null, "leftFilter", null, 0, 1, RightFilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getRightFilterType_RightFilter(), this.getRightFilterType(), null, "rightFilter", null, 0, 1, RightFilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getRightFilterType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, RightFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRightFilterType_ConfigFile(), theXMLTypePackage.getString(), "configFile", null, 0, 1, RightFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRightFilterType_ExpectedType(), theXMLTypePackage.getString(), "expectedType", null, 0, 1, RightFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRightFilterType_Expression(), theXMLTypePackage.getString(), "expression", null, 0, 1, RightFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRightFilterType_Path(), theXMLTypePackage.getString(), "path", null, 0, 1, RightFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRightFilterType_Pattern(), theXMLTypePackage.getString(), "pattern", null, 0, 1, RightFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(routerTypeEClass, RouterType.class, "RouterType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRouterType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, RouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getRouterType_Endpoint(), this.getEndpointType(), null, "endpoint", null, 0, -1, RouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getRouterType_GlobalEndpoint(), this.getGlobalEndpointType(), null, "globalEndpoint", null, 0, -1, RouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getRouterType_ReplyTo(), this.getReplyToType(), null, "replyTo", null, 0, 1, RouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getRouterType_Transaction(), this.getTransactionType(), null, "transaction", null, 0, 1, RouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getRouterType_Filter(), this.getFilterType(), null, "filter", null, 0, 1, RouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getRouterType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, RouterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getRouterType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, RouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRouterType_EnableCorrelation(), this.getEnableCorrelationType(), "enableCorrelation", "IF_NOT_SET", 0, 1, RouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRouterType_PropertyExtractor(), theXMLTypePackage.getString(), "propertyExtractor", null, 0, 1, RouterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(securityFilterTypeEClass, SecurityFilterType.class, "SecurityFilterType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getSecurityFilterType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, SecurityFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getSecurityFilterType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, SecurityFilterType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getSecurityFilterType_ClassName(), theXMLTypePackage.getString(), "className", null, 1, 1, SecurityFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSecurityFilterType_UseProviders(), theXMLTypePackage.getString(), "useProviders", null, 0, 1, SecurityFilterType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(securityManagerTypeEClass, SecurityManagerType.class, "SecurityManagerType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getSecurityManagerType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, SecurityManagerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getSecurityManagerType_SecurityProvider(), this.getSecurityProviderType(), null, "securityProvider", null, 0, -1, SecurityManagerType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEReference(getSecurityManagerType_EncryptionStrategy(), this.getEncryptionStrategyType(), null, "encryptionStrategy", null, 0, -1, SecurityManagerType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getSecurityManagerType_ClassName(), theXMLTypePackage.getString(), "className", null, 0, 1, SecurityManagerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSecurityManagerType_Ref(), theXMLTypePackage.getString(), "ref", null, 0, 1, SecurityManagerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(securityProviderTypeEClass, SecurityProviderType.class, "SecurityProviderType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getSecurityProviderType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, SecurityProviderType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getSecurityProviderType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, SecurityProviderType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getSecurityProviderType_ClassName(), theXMLTypePackage.getString(), "className", null, 0, 1, SecurityProviderType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSecurityProviderType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, SecurityProviderType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSecurityProviderType_Ref(), theXMLTypePackage.getString(), "ref", null, 0, 1, SecurityProviderType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(systemEntryTypeEClass, SystemEntryType.class, "SystemEntryType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getSystemEntryType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, SystemEntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSystemEntryType_DefaultValue(), theXMLTypePackage.getString(), "defaultValue", null, 0, 1, SystemEntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSystemEntryType_Key(), theXMLTypePackage.getString(), "key", null, 1, 1, SystemEntryType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(systemPropertyTypeEClass, SystemPropertyType.class, "SystemPropertyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getSystemPropertyType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, SystemPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSystemPropertyType_DefaultValue(), theXMLTypePackage.getString(), "defaultValue", null, 0, 1, SystemPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSystemPropertyType_Key(), theXMLTypePackage.getString(), "key", null, 1, 1, SystemPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSystemPropertyType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, SystemPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(textPropertyTypeEClass, TextPropertyType.class, "TextPropertyType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTextPropertyType_Value(), theXMLTypePackage.getString(), "value", null, 0, 1, TextPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTextPropertyType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, TextPropertyType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(threadingProfileTypeEClass, ThreadingProfileType.class, "ThreadingProfileType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getThreadingProfileType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, ThreadingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getThreadingProfileType_DoThreading(), theXMLTypePackage.getBoolean(), "doThreading", "true", 0, 1, ThreadingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getThreadingProfileType_Id(), this.getIdType(), "id", "default", 0, 1, ThreadingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getThreadingProfileType_MaxBufferSize(), theXMLTypePackage.getString(), "maxBufferSize", null, 0, 1, ThreadingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getThreadingProfileType_MaxThreadsActive(), theXMLTypePackage.getString(), "maxThreadsActive", null, 0, 1, ThreadingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getThreadingProfileType_MaxThreadsIdle(), theXMLTypePackage.getString(), "maxThreadsIdle", null, 0, 1, ThreadingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getThreadingProfileType_PoolExhaustedAction(), this.getPoolExhaustedActionType(), "poolExhaustedAction", "RUN", 0, 1, ThreadingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getThreadingProfileType_ThreadTTL(), theXMLTypePackage.getString(), "threadTTL", null, 0, 1, ThreadingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getThreadingProfileType_ThreadWaitTimeout(), theXMLTypePackage.getString(), "threadWaitTimeout", null, 0, 1, ThreadingProfileType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(transactionManagerTypeEClass, TransactionManagerType.class, "TransactionManagerType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTransactionManagerType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, TransactionManagerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTransactionManagerType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, TransactionManagerType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransactionManagerType_Factory(), theXMLTypePackage.getString(), "factory", null, 0, 1, TransactionManagerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransactionManagerType_Ref(), theXMLTypePackage.getString(), "ref", null, 0, 1, TransactionManagerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(transactionTypeEClass, TransactionType.class, "TransactionType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTransactionType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, TransactionType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTransactionType_Constraint(), this.getConstraintType(), null, "constraint", null, 0, 1, TransactionType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransactionType_Action(), this.getActionType(), "action", "NONE", 0, 1, TransactionType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransactionType_Factory(), theXMLTypePackage.getString(), "factory", null, 0, 1, TransactionType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransactionType_Timeout(), theXMLTypePackage.getString(), "timeout", null, 0, 1, TransactionType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(transformersTypeEClass, TransformersType.class, "TransformersType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTransformersType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, TransformersType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTransformersType_Transformer(), this.getTransformerType(), null, "transformer", null, 1, -1, TransformersType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);

		initEClass(transformerTypeEClass, TransformerType.class, "TransformerType", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTransformerType_Mixed(), ecorePackage.getEFeatureMapEntry(), "mixed", null, 0, -1, TransformerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTransformerType_Properties(), this.getPropertiesType(), null, "properties", null, 0, 1, TransformerType.class, IS_TRANSIENT, IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransformerType_ClassName(), theXMLTypePackage.getString(), "className", null, 0, 1, TransformerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransformerType_IgnoreBadInput(), theXMLTypePackage.getBoolean(), "ignoreBadInput", null, 0, 1, TransformerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransformerType_Name(), theXMLTypePackage.getString(), "name", null, 1, 1, TransformerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransformerType_Ref(), theXMLTypePackage.getString(), "ref", null, 0, 1, TransformerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTransformerType_ReturnClass(), theXMLTypePackage.getString(), "returnClass", "java.lang.Object", 0, 1, TransformerType.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_UNSETTABLE, !IS_ID, !IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Initialize enums and add enum literals
		initEEnum(actionTypeEEnum, ActionType.class, "ActionType");
		addEEnumLiteral(actionTypeEEnum, ActionType.NONE_LITERAL);
		addEEnumLiteral(actionTypeEEnum, ActionType.ALWAYS_BEGIN_LITERAL);
		addEEnumLiteral(actionTypeEEnum, ActionType.BEGIN_OR_JOIN_LITERAL);
		addEEnumLiteral(actionTypeEEnum, ActionType.ALWAYS_JOIN_LITERAL);
		addEEnumLiteral(actionTypeEEnum, ActionType.JOIN_IF_POSSIBLE_LITERAL);

		initEEnum(createConnectorTypeEEnum, CreateConnectorType.class, "CreateConnectorType");
		addEEnumLiteral(createConnectorTypeEEnum, CreateConnectorType.GET_OR_CREATE_LITERAL);
		addEEnumLiteral(createConnectorTypeEEnum, CreateConnectorType.ALWAYS_CREATE_LITERAL);
		addEEnumLiteral(createConnectorTypeEEnum, CreateConnectorType.NEVER_CREATE_LITERAL);

		initEEnum(enableCorrelationTypeEEnum, EnableCorrelationType.class, "EnableCorrelationType");
		addEEnumLiteral(enableCorrelationTypeEEnum, EnableCorrelationType.ALWAYS_LITERAL);
		addEEnumLiteral(enableCorrelationTypeEEnum, EnableCorrelationType.NEVER_LITERAL);
		addEEnumLiteral(enableCorrelationTypeEEnum, EnableCorrelationType.IF_NOT_SET_LITERAL);

		initEEnum(exhaustedActionTypeEEnum, ExhaustedActionType.class, "ExhaustedActionType");
		addEEnumLiteral(exhaustedActionTypeEEnum, ExhaustedActionType.GROW_LITERAL);
		addEEnumLiteral(exhaustedActionTypeEEnum, ExhaustedActionType.WAIT_LITERAL);
		addEEnumLiteral(exhaustedActionTypeEEnum, ExhaustedActionType.FAIL_LITERAL);

		initEEnum(idTypeEEnum, IdType.class, "IdType");
		addEEnumLiteral(idTypeEEnum, IdType.RECEIVER_LITERAL);
		addEEnumLiteral(idTypeEEnum, IdType.DISPATCHER_LITERAL);
		addEEnumLiteral(idTypeEEnum, IdType.COMPONENT_LITERAL);
		addEEnumLiteral(idTypeEEnum, IdType.DEFAULT_LITERAL);

		initEEnum(initialisationPolicyTypeEEnum, InitialisationPolicyType.class, "InitialisationPolicyType");
		addEEnumLiteral(initialisationPolicyTypeEEnum, InitialisationPolicyType.INITIALISE_NONE_LITERAL);
		addEEnumLiteral(initialisationPolicyTypeEEnum, InitialisationPolicyType.INITIALISE_FIRST_LITERAL);
		addEEnumLiteral(initialisationPolicyTypeEEnum, InitialisationPolicyType.INITIALISE_ALL_LITERAL);

		initEEnum(initialStateTypeEEnum, InitialStateType.class, "InitialStateType");
		addEEnumLiteral(initialStateTypeEEnum, InitialStateType.STARTED_LITERAL);
		addEEnumLiteral(initialStateTypeEEnum, InitialStateType.STOPPED_LITERAL);

		initEEnum(poolExhaustedActionTypeEEnum, PoolExhaustedActionType.class, "PoolExhaustedActionType");
		addEEnumLiteral(poolExhaustedActionTypeEEnum, PoolExhaustedActionType.WAIT_LITERAL);
		addEEnumLiteral(poolExhaustedActionTypeEEnum, PoolExhaustedActionType.DISCARD_LITERAL);
		addEEnumLiteral(poolExhaustedActionTypeEEnum, PoolExhaustedActionType.DISCARD_OLDEST_LITERAL);
		addEEnumLiteral(poolExhaustedActionTypeEEnum, PoolExhaustedActionType.ABORT_LITERAL);
		addEEnumLiteral(poolExhaustedActionTypeEEnum, PoolExhaustedActionType.RUN_LITERAL);

		initEEnum(typeType1EEnum, TypeType1.class, "TypeType1");
		addEEnumLiteral(typeType1EEnum, TypeType1.SENDER_LITERAL);
		addEEnumLiteral(typeType1EEnum, TypeType1.RECEIVER_LITERAL);
		addEEnumLiteral(typeType1EEnum, TypeType1.SENDER_AND_RECEIVER_LITERAL);

		// Initialize data types
		initEDataType(actionTypeObjectEDataType, ActionType.class, "ActionTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
		initEDataType(createConnectorTypeObjectEDataType, CreateConnectorType.class, "CreateConnectorTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
		initEDataType(enableCorrelationTypeObjectEDataType, EnableCorrelationType.class, "EnableCorrelationTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
		initEDataType(exhaustedActionTypeObjectEDataType, ExhaustedActionType.class, "ExhaustedActionTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
		initEDataType(idTypeObjectEDataType, IdType.class, "IdTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
		initEDataType(initialisationPolicyTypeObjectEDataType, InitialisationPolicyType.class, "InitialisationPolicyTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
		initEDataType(initialStateTypeObjectEDataType, InitialStateType.class, "InitialStateTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
		initEDataType(poolExhaustedActionTypeObjectEDataType, PoolExhaustedActionType.class, "PoolExhaustedActionTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
		initEDataType(typeTypeEDataType, String.class, "TypeType", IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS);
		initEDataType(typeTypeObjectEDataType, TypeType1.class, "TypeTypeObject", IS_SERIALIZABLE, IS_GENERATED_INSTANCE_CLASS);
		initEDataType(versionTypeEDataType, String.class, "VersionType", IS_SERIALIZABLE, !IS_GENERATED_INSTANCE_CLASS);

		// Create resource
		createResource(eNS_URI);

		// Create annotations
		// http:///org/eclipse/emf/ecore/util/ExtendedMetaData
		createExtendedMetaDataAnnotations();
	}

	/**
	 * Initializes the annotations for <b>http:///org/eclipse/emf/ecore/util/ExtendedMetaData</b>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected void createExtendedMetaDataAnnotations() {
		String source = "http:///org/eclipse/emf/ecore/util/ExtendedMetaData";		
		addAnnotation
		  (this, 
		   source, 
		   new String[] {
			 "qualified", "false"
		   });		
		addAnnotation
		  (actionTypeEEnum, 
		   source, 
		   new String[] {
			 "name", "action_._type"
		   });		
		addAnnotation
		  (actionTypeObjectEDataType, 
		   source, 
		   new String[] {
			 "name", "action_._type:Object",
			 "baseType", "action_._type"
		   });		
		addAnnotation
		  (agentsTypeEClass, 
		   source, 
		   new String[] {
			 "name", "agentsType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getAgentsType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getAgentsType_Agent(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "agent",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (agentTypeEClass, 
		   source, 
		   new String[] {
			 "name", "agentType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getAgentType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getAgentType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getAgentType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getAgentType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getAgentType_Ref(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ref",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (catchAllStrategyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "catch-all-strategyType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getCatchAllStrategyType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getCatchAllStrategyType_Endpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getCatchAllStrategyType_GlobalEndpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "global-endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getCatchAllStrategyType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getCatchAllStrategyType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (componentFactoryTypeEClass, 
		   source, 
		   new String[] {
			 "name", "component-factoryType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getComponentFactoryType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getComponentFactoryType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (componentLifecycleAdapterFactoryTypeEClass, 
		   source, 
		   new String[] {
			 "name", "component-lifecycle-adapter-factoryType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getComponentLifecycleAdapterFactoryType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getComponentLifecycleAdapterFactoryType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (componentPoolFactoryTypeEClass, 
		   source, 
		   new String[] {
			 "name", "component-pool-factoryType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getComponentPoolFactoryType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getComponentPoolFactoryType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getComponentPoolFactoryType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (connectionStrategyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "connection-strategyType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getConnectionStrategyType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getConnectionStrategyType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConnectionStrategyType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (connectorTypeEClass, 
		   source, 
		   new String[] {
			 "name", "connectorType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getConnectorType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getConnectorType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConnectorType_ThreadingProfile(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "threading-profile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConnectorType_ExceptionStrategy(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "exception-strategy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConnectorType_ConnectionStrategy(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "connection-strategy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConnectorType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConnectorType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConnectorType_Ref(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ref",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (constraintTypeEClass, 
		   source, 
		   new String[] {
			 "name", "constraintType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getConstraintType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getConstraintType_LeftFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "left-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConstraintType_RightFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "right-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConstraintType_Filter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConstraintType_BatchSize(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "batchSize",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConstraintType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConstraintType_ExpectedType(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "expectedType",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConstraintType_Expression(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "expression",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConstraintType_Frequency(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "frequency",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConstraintType_Path(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "path",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getConstraintType_Pattern(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "pattern",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (containerContextTypeEClass, 
		   source, 
		   new String[] {
			 "name", "container-contextType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getContainerContextType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getContainerContextType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getContainerContextType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getContainerContextType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (containerEntryTypeEClass, 
		   source, 
		   new String[] {
			 "name", "container-entryType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getContainerEntryType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getContainerEntryType_Reference(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "reference",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getContainerEntryType_Required(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "required",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (containerPropertyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "container-propertyType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getContainerPropertyType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getContainerPropertyType_Container(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "container",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getContainerPropertyType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getContainerPropertyType_Reference(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "reference",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getContainerPropertyType_Required(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "required",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (createConnectorTypeEEnum, 
		   source, 
		   new String[] {
			 "name", "createConnector_._type"
		   });		
		addAnnotation
		  (createConnectorTypeObjectEDataType, 
		   source, 
		   new String[] {
			 "name", "createConnector_._type:Object",
			 "baseType", "createConnector_._type"
		   });		
		addAnnotation
		  (documentRootEClass, 
		   source, 
		   new String[] {
			 "name", "",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getDocumentRoot_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getDocumentRoot_XMLNSPrefixMap(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "xmlns:prefix"
		   });		
		addAnnotation
		  (getDocumentRoot_XSISchemaLocation(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "xsi:schemaLocation"
		   });		
		addAnnotation
		  (getDocumentRoot_MuleConfiguration(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "mule-configuration",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (enableCorrelationTypeEEnum, 
		   source, 
		   new String[] {
			 "name", "enableCorrelation_._type"
		   });		
		addAnnotation
		  (enableCorrelationTypeObjectEDataType, 
		   source, 
		   new String[] {
			 "name", "enableCorrelation_._type:Object",
			 "baseType", "enableCorrelation_._type"
		   });		
		addAnnotation
		  (encryptionStrategyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "encryption-strategyType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getEncryptionStrategyType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getEncryptionStrategyType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEncryptionStrategyType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEncryptionStrategyType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (endpointIdentifiersTypeEClass, 
		   source, 
		   new String[] {
			 "name", "endpoint-identifiersType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getEndpointIdentifiersType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getEndpointIdentifiersType_EndpointIdentifier(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "endpoint-identifier",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (endpointIdentifierTypeEClass, 
		   source, 
		   new String[] {
			 "name", "endpoint-identifierType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getEndpointIdentifierType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getEndpointIdentifierType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointIdentifierType_Value(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "value",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (endpointTypeEClass, 
		   source, 
		   new String[] {
			 "name", "endpointType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getEndpointType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getEndpointType_Transaction(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "transaction",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_Filter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_SecurityFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "security-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_Address(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "address",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_Connector(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "connector",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_CreateConnector(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "createConnector",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_Ref(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ref",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_RemoteSync(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "remoteSync",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_RemoteSyncTimeout(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "remoteSyncTimeout",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_ResponseTransformers(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "responseTransformers",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_Synchronous(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "synchronous",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_Transformers(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "transformers",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getEndpointType_Type(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "type",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (entryPointResolverTypeEClass, 
		   source, 
		   new String[] {
			 "name", "entry-point-resolverType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getEntryPointResolverType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getEntryPointResolverType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (entryTypeEClass, 
		   source, 
		   new String[] {
			 "name", "entryType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getEntryType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getEntryType_Value(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "value",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (environmentPropertiesTypeEClass, 
		   source, 
		   new String[] {
			 "name", "environment-propertiesType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getEnvironmentPropertiesType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getEnvironmentPropertiesType_Group(), 
		   source, 
		   new String[] {
			 "kind", "group",
			 "name", "group:1"
		   });		
		addAnnotation
		  (getEnvironmentPropertiesType_Property(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getEnvironmentPropertiesType_FactoryProperty(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "factory-property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getEnvironmentPropertiesType_SystemProperty(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "system-property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getEnvironmentPropertiesType_Map(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "map",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getEnvironmentPropertiesType_List(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "list",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getEnvironmentPropertiesType_FileProperties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "file-properties",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (exceptionStrategyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "exception-strategyType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getExceptionStrategyType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getExceptionStrategyType_Endpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getExceptionStrategyType_GlobalEndpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "global-endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getExceptionStrategyType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getExceptionStrategyType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (exhaustedActionTypeEEnum, 
		   source, 
		   new String[] {
			 "name", "exhaustedAction_._type"
		   });		
		addAnnotation
		  (exhaustedActionTypeObjectEDataType, 
		   source, 
		   new String[] {
			 "name", "exhaustedAction_._type:Object",
			 "baseType", "exhaustedAction_._type"
		   });		
		addAnnotation
		  (factoryEntryTypeEClass, 
		   source, 
		   new String[] {
			 "name", "factory-entryType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getFactoryEntryType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getFactoryEntryType_Factory(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "factory",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (factoryPropertyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "factory-propertyType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getFactoryPropertyType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getFactoryPropertyType_Factory(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "factory",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFactoryPropertyType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (filePropertiesTypeEClass, 
		   source, 
		   new String[] {
			 "name", "file-propertiesType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getFilePropertiesType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getFilePropertiesType_Location(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "location",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilePropertiesType_Override(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "override",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (filterTypeEClass, 
		   source, 
		   new String[] {
			 "name", "filterType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getFilterType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getFilterType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilterType_Filter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilterType_LeftFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "left-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilterType_RightFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "right-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilterType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilterType_ConfigFile(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "configFile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilterType_ExpectedType(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "expectedType",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilterType_Expression(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "expression",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilterType_Path(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "path",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getFilterType_Pattern(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "pattern",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (globalEndpointsTypeEClass, 
		   source, 
		   new String[] {
			 "name", "global-endpointsType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getGlobalEndpointsType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getGlobalEndpointsType_Endpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (globalEndpointTypeEClass, 
		   source, 
		   new String[] {
			 "name", "global-endpointType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getGlobalEndpointType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getGlobalEndpointType_Transaction(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "transaction",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_Filter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_SecurityFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "security-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_Address(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "address",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_RemoteSync(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "remoteSync",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_RemoteSyncTimeout(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "remoteSyncTimeout",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_ResponseTransformers(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "responseTransformers",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_Synchronous(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "synchronous",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getGlobalEndpointType_Transformers(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "transformers",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (idTypeEEnum, 
		   source, 
		   new String[] {
			 "name", "id_._type"
		   });		
		addAnnotation
		  (idTypeObjectEDataType, 
		   source, 
		   new String[] {
			 "name", "id_._type:Object",
			 "baseType", "id_._type"
		   });		
		addAnnotation
		  (inboundRouterTypeEClass, 
		   source, 
		   new String[] {
			 "name", "inbound-routerType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getInboundRouterType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getInboundRouterType_CatchAllStrategy(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "catch-all-strategy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getInboundRouterType_Endpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getInboundRouterType_GlobalEndpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "global-endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getInboundRouterType_Router(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "router",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getInboundRouterType_MatchAll(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "matchAll",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (initialisationPolicyTypeEEnum, 
		   source, 
		   new String[] {
			 "name", "initialisationPolicy_._type"
		   });		
		addAnnotation
		  (initialisationPolicyTypeObjectEDataType, 
		   source, 
		   new String[] {
			 "name", "initialisationPolicy_._type:Object",
			 "baseType", "initialisationPolicy_._type"
		   });		
		addAnnotation
		  (initialStateTypeEEnum, 
		   source, 
		   new String[] {
			 "name", "initialState_._type"
		   });		
		addAnnotation
		  (initialStateTypeObjectEDataType, 
		   source, 
		   new String[] {
			 "name", "initialState_._type:Object",
			 "baseType", "initialState_._type"
		   });		
		addAnnotation
		  (interceptorStackTypeEClass, 
		   source, 
		   new String[] {
			 "name", "interceptor-stackType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getInterceptorStackType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getInterceptorStackType_Interceptor(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "interceptor",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getInterceptorStackType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (interceptorTypeEClass, 
		   source, 
		   new String[] {
			 "name", "interceptorType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getInterceptorType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getInterceptorType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getInterceptorType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getInterceptorType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (leftFilterTypeEClass, 
		   source, 
		   new String[] {
			 "name", "left-filterType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getLeftFilterType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getLeftFilterType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getLeftFilterType_Filter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getLeftFilterType_LeftFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "left-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getLeftFilterType_RightFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "right-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getLeftFilterType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getLeftFilterType_ConfigFile(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "configFile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getLeftFilterType_ExpectedType(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "expectedType",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getLeftFilterType_Expression(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "expression",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getLeftFilterType_Path(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "path",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getLeftFilterType_Pattern(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "pattern",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (listTypeEClass, 
		   source, 
		   new String[] {
			 "name", "listType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getListType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getListType_Group(), 
		   source, 
		   new String[] {
			 "kind", "group",
			 "name", "group:1"
		   });		
		addAnnotation
		  (getListType_Entry(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "entry",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getListType_FactoryEntry(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "factory-entry",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getListType_SystemEntry(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "system-entry",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getListType_ContainerEntry(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "container-entry",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getListType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (mapTypeEClass, 
		   source, 
		   new String[] {
			 "name", "mapType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getMapType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getMapType_Group(), 
		   source, 
		   new String[] {
			 "kind", "group",
			 "name", "group:1"
		   });		
		addAnnotation
		  (getMapType_Property(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getMapType_FactoryProperty(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "factory-property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getMapType_ContainerProperty(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "container-property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getMapType_SystemProperty(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "system-property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getMapType_Map(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "map",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getMapType_List(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "list",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getMapType_FileProperties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "file-properties",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getMapType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (modelTypeEClass, 
		   source, 
		   new String[] {
			 "name", "modelType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getModelType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getModelType_Description(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "description",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_EntryPointResolver(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "entry-point-resolver",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_ComponentFactory(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "component-factory",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_ComponentLifecycleAdapterFactory(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "component-lifecycle-adapter-factory",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_ComponentPoolFactory(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "component-pool-factory",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_ExceptionStrategy(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "exception-strategy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_MuleDescriptor(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "mule-descriptor",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_Ref(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ref",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getModelType_Type(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "type",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (muleConfigurationTypeEClass, 
		   source, 
		   new String[] {
			 "name", "mule-configurationType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getMuleConfigurationType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getMuleConfigurationType_Description(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "description",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_EnvironmentProperties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "environment-properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_MuleEnvironmentProperties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "mule-environment-properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_ContainerContext(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "container-context",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_SecurityManager(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "security-manager",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_TransactionManager(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "transaction-manager",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_Agents(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "agents",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_Connector(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "connector",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_EndpointIdentifiers(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "endpoint-identifiers",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_Transformers(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "transformers",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_GlobalEndpoints(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "global-endpoints",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_InterceptorStack(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "interceptor-stack",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_Model(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "model",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_MuleDescriptor(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "mule-descriptor",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_Id(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "id",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleConfigurationType_Version(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "version",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (muleDescriptorTypeEClass, 
		   source, 
		   new String[] {
			 "name", "mule-descriptorType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getMuleDescriptorType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getMuleDescriptorType_InboundRouter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "inbound-router",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_OutboundRouter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "outbound-router",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_ResponseRouter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "response-router",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_Interceptor(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "interceptor",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_ThreadingProfile(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "threading-profile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_PoolingProfile(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "pooling-profile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_QueueProfile(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "queue-profile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_ExceptionStrategy(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "exception-strategy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_ContainerManaged(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "containerManaged",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_Implementation(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "implementation",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_InboundEndpoint(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "inboundEndpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_InboundTransformer(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "inboundTransformer",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_InitialState(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "initialState",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_OutboundEndpoint(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "outboundEndpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_OutboundTransformer(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "outboundTransformer",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_Ref(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ref",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_ResponseTransformer(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "responseTransformer",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_Singleton(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "singleton",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleDescriptorType_Version(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "version",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (muleEnvironmentPropertiesTypeEClass, 
		   source, 
		   new String[] {
			 "name", "mule-environment-propertiesType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_ThreadingProfile(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "threading-profile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_PoolingProfile(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "pooling-profile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_QueueProfile(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "queue-profile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_PersistenceStrategy(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "persistence-strategy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_ConnectionStrategy(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "connection-strategy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_ClientMode(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "clientMode",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_Embedded(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "embedded",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_EnableMessageEvents(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "enableMessageEvents",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_Encoding(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "encoding",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_Model(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "model",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_RecoverableMode(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "recoverableMode",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_RemoteSync(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "remoteSync",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_ServerUrl(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "serverUrl",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_Synchronous(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "synchronous",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_SynchronousEventTimeout(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "synchronousEventTimeout",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_TransactionTimeout(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "transactionTimeout",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getMuleEnvironmentPropertiesType_WorkingDirectory(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "workingDirectory",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (outboundRouterTypeEClass, 
		   source, 
		   new String[] {
			 "name", "outbound-routerType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getOutboundRouterType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getOutboundRouterType_CatchAllStrategy(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "catch-all-strategy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getOutboundRouterType_Router(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "router",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getOutboundRouterType_MatchAll(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "matchAll",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (persistenceStrategyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "persistence-strategyType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getPersistenceStrategyType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getPersistenceStrategyType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getPersistenceStrategyType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (poolExhaustedActionTypeEEnum, 
		   source, 
		   new String[] {
			 "name", "poolExhaustedAction_._type"
		   });		
		addAnnotation
		  (poolExhaustedActionTypeObjectEDataType, 
		   source, 
		   new String[] {
			 "name", "poolExhaustedAction_._type:Object",
			 "baseType", "poolExhaustedAction_._type"
		   });		
		addAnnotation
		  (poolingProfileTypeEClass, 
		   source, 
		   new String[] {
			 "name", "pooling-profileType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getPoolingProfileType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getPoolingProfileType_ExhaustedAction(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "exhaustedAction",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getPoolingProfileType_Factory(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "factory",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getPoolingProfileType_InitialisationPolicy(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "initialisationPolicy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getPoolingProfileType_MaxActive(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "maxActive",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getPoolingProfileType_MaxIdle(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "maxIdle",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getPoolingProfileType_MaxWait(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "maxWait",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (propertiesTypeEClass, 
		   source, 
		   new String[] {
			 "name", "propertiesType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getPropertiesType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getPropertiesType_Group(), 
		   source, 
		   new String[] {
			 "kind", "group",
			 "name", "group:1"
		   });		
		addAnnotation
		  (getPropertiesType_Property(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getPropertiesType_FactoryProperty(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "factory-property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getPropertiesType_ContainerProperty(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "container-property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getPropertiesType_SystemProperty(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "system-property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getPropertiesType_Map(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "map",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getPropertiesType_List(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "list",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getPropertiesType_FileProperties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "file-properties",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (getPropertiesType_TextProperty(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "text-property",
			 "namespace", "##targetNamespace",
			 "group", "group:1"
		   });		
		addAnnotation
		  (propertyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "propertyType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getPropertyType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getPropertyType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getPropertyType_Value(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "value",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (queueProfileTypeEClass, 
		   source, 
		   new String[] {
			 "name", "queue-profileType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getQueueProfileType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getQueueProfileType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getQueueProfileType_MaxOutstandingMessages(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "maxOutstandingMessages",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getQueueProfileType_Persistent(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "persistent",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (replyToTypeEClass, 
		   source, 
		   new String[] {
			 "name", "reply-toType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getReplyToType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getReplyToType_Address(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "address",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (responseRouterTypeEClass, 
		   source, 
		   new String[] {
			 "name", "response-routerType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getResponseRouterType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getResponseRouterType_Endpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getResponseRouterType_GlobalEndpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "global-endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getResponseRouterType_Router(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "router",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getResponseRouterType_Timeout(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "timeout",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (rightFilterTypeEClass, 
		   source, 
		   new String[] {
			 "name", "right-filterType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getRightFilterType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getRightFilterType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRightFilterType_Filter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRightFilterType_LeftFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "left-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRightFilterType_RightFilter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "right-filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRightFilterType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRightFilterType_ConfigFile(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "configFile",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRightFilterType_ExpectedType(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "expectedType",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRightFilterType_Expression(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "expression",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRightFilterType_Path(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "path",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRightFilterType_Pattern(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "pattern",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (routerTypeEClass, 
		   source, 
		   new String[] {
			 "name", "routerType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getRouterType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getRouterType_Endpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRouterType_GlobalEndpoint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "global-endpoint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRouterType_ReplyTo(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "reply-to",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRouterType_Transaction(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "transaction",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRouterType_Filter(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "filter",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRouterType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRouterType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRouterType_EnableCorrelation(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "enableCorrelation",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getRouterType_PropertyExtractor(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "propertyExtractor",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (securityFilterTypeEClass, 
		   source, 
		   new String[] {
			 "name", "security-filterType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getSecurityFilterType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getSecurityFilterType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSecurityFilterType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSecurityFilterType_UseProviders(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "useProviders",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (securityManagerTypeEClass, 
		   source, 
		   new String[] {
			 "name", "security-managerType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getSecurityManagerType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getSecurityManagerType_SecurityProvider(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "security-provider",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSecurityManagerType_EncryptionStrategy(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "encryption-strategy",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSecurityManagerType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSecurityManagerType_Ref(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ref",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (securityProviderTypeEClass, 
		   source, 
		   new String[] {
			 "name", "security-providerType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getSecurityProviderType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getSecurityProviderType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSecurityProviderType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSecurityProviderType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSecurityProviderType_Ref(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ref",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (systemEntryTypeEClass, 
		   source, 
		   new String[] {
			 "name", "system-entryType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getSystemEntryType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getSystemEntryType_DefaultValue(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "defaultValue",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSystemEntryType_Key(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "key",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (systemPropertyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "system-propertyType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getSystemPropertyType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getSystemPropertyType_DefaultValue(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "defaultValue",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSystemPropertyType_Key(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "key",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getSystemPropertyType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (textPropertyTypeEClass, 
		   source, 
		   new String[] {
			 "name", "text-propertyType",
			 "kind", "simple"
		   });		
		addAnnotation
		  (getTextPropertyType_Value(), 
		   source, 
		   new String[] {
			 "name", ":0",
			 "kind", "simple"
		   });		
		addAnnotation
		  (getTextPropertyType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (threadingProfileTypeEClass, 
		   source, 
		   new String[] {
			 "name", "threading-profileType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getThreadingProfileType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getThreadingProfileType_DoThreading(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "doThreading",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getThreadingProfileType_Id(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "id",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getThreadingProfileType_MaxBufferSize(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "maxBufferSize",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getThreadingProfileType_MaxThreadsActive(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "maxThreadsActive",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getThreadingProfileType_MaxThreadsIdle(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "maxThreadsIdle",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getThreadingProfileType_PoolExhaustedAction(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "poolExhaustedAction",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getThreadingProfileType_ThreadTTL(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "threadTTL",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getThreadingProfileType_ThreadWaitTimeout(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "threadWaitTimeout",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (transactionManagerTypeEClass, 
		   source, 
		   new String[] {
			 "name", "transaction-managerType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getTransactionManagerType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getTransactionManagerType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransactionManagerType_Factory(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "factory",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransactionManagerType_Ref(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ref",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (transactionTypeEClass, 
		   source, 
		   new String[] {
			 "name", "transactionType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getTransactionType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getTransactionType_Constraint(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "constraint",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransactionType_Action(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "action",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransactionType_Factory(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "factory",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransactionType_Timeout(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "timeout",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (transformersTypeEClass, 
		   source, 
		   new String[] {
			 "name", "transformersType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getTransformersType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getTransformersType_Transformer(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "transformer",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (transformerTypeEClass, 
		   source, 
		   new String[] {
			 "name", "transformerType",
			 "kind", "mixed"
		   });		
		addAnnotation
		  (getTransformerType_Mixed(), 
		   source, 
		   new String[] {
			 "kind", "elementWildcard",
			 "name", ":mixed"
		   });		
		addAnnotation
		  (getTransformerType_Properties(), 
		   source, 
		   new String[] {
			 "kind", "element",
			 "name", "properties",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransformerType_ClassName(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "className",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransformerType_IgnoreBadInput(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ignoreBadInput",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransformerType_Name(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "name",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransformerType_Ref(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "ref",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (getTransformerType_ReturnClass(), 
		   source, 
		   new String[] {
			 "kind", "attribute",
			 "name", "returnClass",
			 "namespace", "##targetNamespace"
		   });		
		addAnnotation
		  (typeTypeEDataType, 
		   source, 
		   new String[] {
			 "name", "type_._type",
			 "baseType", "http://www.eclipse.org/emf/2003/XMLType#NMTOKEN",
			 "enumeration", "seda direct pipeline jms jms-clustered jcyclone custom"
		   });		
		addAnnotation
		  (typeType1EEnum, 
		   source, 
		   new String[] {
			 "name", "type_._1_._type"
		   });		
		addAnnotation
		  (typeTypeObjectEDataType, 
		   source, 
		   new String[] {
			 "name", "type_._1_._type:Object",
			 "baseType", "type_._1_._type"
		   });		
		addAnnotation
		  (versionTypeEDataType, 
		   source, 
		   new String[] {
			 "name", "version_._type",
			 "baseType", "http://www.eclipse.org/emf/2003/XMLType#NMTOKEN",
			 "enumeration", "1.0"
		   });
	}

} //MulePackageImpl
