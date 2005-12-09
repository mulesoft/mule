/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.mule.schema.util;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

import org.mule.schema.*;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see org.mule.schema.MulePackage
 * @generated
 */
public class MuleAdapterFactory extends AdapterFactoryImpl {
	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static MulePackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public MuleAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = MulePackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject)object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch the delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MuleSwitch modelSwitch =
		new MuleSwitch() {
			public Object caseAgentsType(AgentsType object) {
				return createAgentsTypeAdapter();
			}
			public Object caseAgentType(AgentType object) {
				return createAgentTypeAdapter();
			}
			public Object caseCatchAllStrategyType(CatchAllStrategyType object) {
				return createCatchAllStrategyTypeAdapter();
			}
			public Object caseComponentFactoryType(ComponentFactoryType object) {
				return createComponentFactoryTypeAdapter();
			}
			public Object caseComponentLifecycleAdapterFactoryType(ComponentLifecycleAdapterFactoryType object) {
				return createComponentLifecycleAdapterFactoryTypeAdapter();
			}
			public Object caseComponentPoolFactoryType(ComponentPoolFactoryType object) {
				return createComponentPoolFactoryTypeAdapter();
			}
			public Object caseConnectionStrategyType(ConnectionStrategyType object) {
				return createConnectionStrategyTypeAdapter();
			}
			public Object caseConnectorType(ConnectorType object) {
				return createConnectorTypeAdapter();
			}
			public Object caseConstraintType(ConstraintType object) {
				return createConstraintTypeAdapter();
			}
			public Object caseContainerContextType(ContainerContextType object) {
				return createContainerContextTypeAdapter();
			}
			public Object caseContainerEntryType(ContainerEntryType object) {
				return createContainerEntryTypeAdapter();
			}
			public Object caseContainerPropertyType(ContainerPropertyType object) {
				return createContainerPropertyTypeAdapter();
			}
			public Object caseDocumentRoot(DocumentRoot object) {
				return createDocumentRootAdapter();
			}
			public Object caseEncryptionStrategyType(EncryptionStrategyType object) {
				return createEncryptionStrategyTypeAdapter();
			}
			public Object caseEndpointIdentifiersType(EndpointIdentifiersType object) {
				return createEndpointIdentifiersTypeAdapter();
			}
			public Object caseEndpointIdentifierType(EndpointIdentifierType object) {
				return createEndpointIdentifierTypeAdapter();
			}
			public Object caseEndpointType(EndpointType object) {
				return createEndpointTypeAdapter();
			}
			public Object caseEntryPointResolverType(EntryPointResolverType object) {
				return createEntryPointResolverTypeAdapter();
			}
			public Object caseEntryType(EntryType object) {
				return createEntryTypeAdapter();
			}
			public Object caseEnvironmentPropertiesType(EnvironmentPropertiesType object) {
				return createEnvironmentPropertiesTypeAdapter();
			}
			public Object caseExceptionStrategyType(ExceptionStrategyType object) {
				return createExceptionStrategyTypeAdapter();
			}
			public Object caseFactoryEntryType(FactoryEntryType object) {
				return createFactoryEntryTypeAdapter();
			}
			public Object caseFactoryPropertyType(FactoryPropertyType object) {
				return createFactoryPropertyTypeAdapter();
			}
			public Object caseFilePropertiesType(FilePropertiesType object) {
				return createFilePropertiesTypeAdapter();
			}
			public Object caseFilterType(FilterType object) {
				return createFilterTypeAdapter();
			}
			public Object caseGlobalEndpointsType(GlobalEndpointsType object) {
				return createGlobalEndpointsTypeAdapter();
			}
			public Object caseGlobalEndpointType(GlobalEndpointType object) {
				return createGlobalEndpointTypeAdapter();
			}
			public Object caseInboundRouterType(InboundRouterType object) {
				return createInboundRouterTypeAdapter();
			}
			public Object caseInterceptorStackType(InterceptorStackType object) {
				return createInterceptorStackTypeAdapter();
			}
			public Object caseInterceptorType(InterceptorType object) {
				return createInterceptorTypeAdapter();
			}
			public Object caseLeftFilterType(LeftFilterType object) {
				return createLeftFilterTypeAdapter();
			}
			public Object caseListType(ListType object) {
				return createListTypeAdapter();
			}
			public Object caseMapType(MapType object) {
				return createMapTypeAdapter();
			}
			public Object caseModelType(ModelType object) {
				return createModelTypeAdapter();
			}
			public Object caseMuleConfigurationType(MuleConfigurationType object) {
				return createMuleConfigurationTypeAdapter();
			}
			public Object caseMuleDescriptorType(MuleDescriptorType object) {
				return createMuleDescriptorTypeAdapter();
			}
			public Object caseMuleEnvironmentPropertiesType(MuleEnvironmentPropertiesType object) {
				return createMuleEnvironmentPropertiesTypeAdapter();
			}
			public Object caseOutboundRouterType(OutboundRouterType object) {
				return createOutboundRouterTypeAdapter();
			}
			public Object casePersistenceStrategyType(PersistenceStrategyType object) {
				return createPersistenceStrategyTypeAdapter();
			}
			public Object casePoolingProfileType(PoolingProfileType object) {
				return createPoolingProfileTypeAdapter();
			}
			public Object casePropertiesType(PropertiesType object) {
				return createPropertiesTypeAdapter();
			}
			public Object casePropertyType(PropertyType object) {
				return createPropertyTypeAdapter();
			}
			public Object caseQueueProfileType(QueueProfileType object) {
				return createQueueProfileTypeAdapter();
			}
			public Object caseReplyToType(ReplyToType object) {
				return createReplyToTypeAdapter();
			}
			public Object caseResponseRouterType(ResponseRouterType object) {
				return createResponseRouterTypeAdapter();
			}
			public Object caseRightFilterType(RightFilterType object) {
				return createRightFilterTypeAdapter();
			}
			public Object caseRouterType(RouterType object) {
				return createRouterTypeAdapter();
			}
			public Object caseSecurityFilterType(SecurityFilterType object) {
				return createSecurityFilterTypeAdapter();
			}
			public Object caseSecurityManagerType(SecurityManagerType object) {
				return createSecurityManagerTypeAdapter();
			}
			public Object caseSecurityProviderType(SecurityProviderType object) {
				return createSecurityProviderTypeAdapter();
			}
			public Object caseSystemEntryType(SystemEntryType object) {
				return createSystemEntryTypeAdapter();
			}
			public Object caseSystemPropertyType(SystemPropertyType object) {
				return createSystemPropertyTypeAdapter();
			}
			public Object caseTextPropertyType(TextPropertyType object) {
				return createTextPropertyTypeAdapter();
			}
			public Object caseThreadingProfileType(ThreadingProfileType object) {
				return createThreadingProfileTypeAdapter();
			}
			public Object caseTransactionManagerType(TransactionManagerType object) {
				return createTransactionManagerTypeAdapter();
			}
			public Object caseTransactionType(TransactionType object) {
				return createTransactionTypeAdapter();
			}
			public Object caseTransformersType(TransformersType object) {
				return createTransformersTypeAdapter();
			}
			public Object caseTransformerType(TransformerType object) {
				return createTransformerTypeAdapter();
			}
			public Object defaultCase(EObject object) {
				return createEObjectAdapter();
			}
		};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	public Adapter createAdapter(Notifier target) {
		return (Adapter)modelSwitch.doSwitch((EObject)target);
	}


	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.AgentsType <em>Agents Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.AgentsType
	 * @generated
	 */
	public Adapter createAgentsTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.AgentType <em>Agent Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.AgentType
	 * @generated
	 */
	public Adapter createAgentTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.CatchAllStrategyType <em>Catch All Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.CatchAllStrategyType
	 * @generated
	 */
	public Adapter createCatchAllStrategyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ComponentFactoryType <em>Component Factory Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ComponentFactoryType
	 * @generated
	 */
	public Adapter createComponentFactoryTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ComponentLifecycleAdapterFactoryType <em>Component Lifecycle Adapter Factory Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ComponentLifecycleAdapterFactoryType
	 * @generated
	 */
	public Adapter createComponentLifecycleAdapterFactoryTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ComponentPoolFactoryType <em>Component Pool Factory Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ComponentPoolFactoryType
	 * @generated
	 */
	public Adapter createComponentPoolFactoryTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ConnectionStrategyType <em>Connection Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ConnectionStrategyType
	 * @generated
	 */
	public Adapter createConnectionStrategyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ConnectorType <em>Connector Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ConnectorType
	 * @generated
	 */
	public Adapter createConnectorTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ConstraintType <em>Constraint Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ConstraintType
	 * @generated
	 */
	public Adapter createConstraintTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ContainerContextType <em>Container Context Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ContainerContextType
	 * @generated
	 */
	public Adapter createContainerContextTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ContainerEntryType <em>Container Entry Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ContainerEntryType
	 * @generated
	 */
	public Adapter createContainerEntryTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ContainerPropertyType <em>Container Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ContainerPropertyType
	 * @generated
	 */
	public Adapter createContainerPropertyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.DocumentRoot <em>Document Root</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.DocumentRoot
	 * @generated
	 */
	public Adapter createDocumentRootAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.EncryptionStrategyType <em>Encryption Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.EncryptionStrategyType
	 * @generated
	 */
	public Adapter createEncryptionStrategyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.EndpointIdentifiersType <em>Endpoint Identifiers Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.EndpointIdentifiersType
	 * @generated
	 */
	public Adapter createEndpointIdentifiersTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.EndpointIdentifierType <em>Endpoint Identifier Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.EndpointIdentifierType
	 * @generated
	 */
	public Adapter createEndpointIdentifierTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.EndpointType <em>Endpoint Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.EndpointType
	 * @generated
	 */
	public Adapter createEndpointTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.EntryPointResolverType <em>Entry Point Resolver Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.EntryPointResolverType
	 * @generated
	 */
	public Adapter createEntryPointResolverTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.EntryType <em>Entry Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.EntryType
	 * @generated
	 */
	public Adapter createEntryTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.EnvironmentPropertiesType <em>Environment Properties Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.EnvironmentPropertiesType
	 * @generated
	 */
	public Adapter createEnvironmentPropertiesTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ExceptionStrategyType <em>Exception Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ExceptionStrategyType
	 * @generated
	 */
	public Adapter createExceptionStrategyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.FactoryEntryType <em>Factory Entry Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.FactoryEntryType
	 * @generated
	 */
	public Adapter createFactoryEntryTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.FactoryPropertyType <em>Factory Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.FactoryPropertyType
	 * @generated
	 */
	public Adapter createFactoryPropertyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.FilePropertiesType <em>File Properties Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.FilePropertiesType
	 * @generated
	 */
	public Adapter createFilePropertiesTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.FilterType <em>Filter Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.FilterType
	 * @generated
	 */
	public Adapter createFilterTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.GlobalEndpointsType <em>Global Endpoints Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.GlobalEndpointsType
	 * @generated
	 */
	public Adapter createGlobalEndpointsTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.GlobalEndpointType <em>Global Endpoint Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.GlobalEndpointType
	 * @generated
	 */
	public Adapter createGlobalEndpointTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.InboundRouterType <em>Inbound Router Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.InboundRouterType
	 * @generated
	 */
	public Adapter createInboundRouterTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.InterceptorStackType <em>Interceptor Stack Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.InterceptorStackType
	 * @generated
	 */
	public Adapter createInterceptorStackTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.InterceptorType <em>Interceptor Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.InterceptorType
	 * @generated
	 */
	public Adapter createInterceptorTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.LeftFilterType <em>Left Filter Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.LeftFilterType
	 * @generated
	 */
	public Adapter createLeftFilterTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ListType <em>List Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ListType
	 * @generated
	 */
	public Adapter createListTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.MapType <em>Map Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.MapType
	 * @generated
	 */
	public Adapter createMapTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ModelType <em>Model Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ModelType
	 * @generated
	 */
	public Adapter createModelTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.MuleConfigurationType <em>Configuration Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.MuleConfigurationType
	 * @generated
	 */
	public Adapter createMuleConfigurationTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.MuleDescriptorType <em>Descriptor Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.MuleDescriptorType
	 * @generated
	 */
	public Adapter createMuleDescriptorTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.MuleEnvironmentPropertiesType <em>Environment Properties Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.MuleEnvironmentPropertiesType
	 * @generated
	 */
	public Adapter createMuleEnvironmentPropertiesTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.OutboundRouterType <em>Outbound Router Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.OutboundRouterType
	 * @generated
	 */
	public Adapter createOutboundRouterTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.PersistenceStrategyType <em>Persistence Strategy Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.PersistenceStrategyType
	 * @generated
	 */
	public Adapter createPersistenceStrategyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.PoolingProfileType <em>Pooling Profile Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.PoolingProfileType
	 * @generated
	 */
	public Adapter createPoolingProfileTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.PropertiesType <em>Properties Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.PropertiesType
	 * @generated
	 */
	public Adapter createPropertiesTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.PropertyType <em>Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.PropertyType
	 * @generated
	 */
	public Adapter createPropertyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.QueueProfileType <em>Queue Profile Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.QueueProfileType
	 * @generated
	 */
	public Adapter createQueueProfileTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ReplyToType <em>Reply To Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ReplyToType
	 * @generated
	 */
	public Adapter createReplyToTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ResponseRouterType <em>Response Router Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ResponseRouterType
	 * @generated
	 */
	public Adapter createResponseRouterTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.RightFilterType <em>Right Filter Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.RightFilterType
	 * @generated
	 */
	public Adapter createRightFilterTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.RouterType <em>Router Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.RouterType
	 * @generated
	 */
	public Adapter createRouterTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.SecurityFilterType <em>Security Filter Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.SecurityFilterType
	 * @generated
	 */
	public Adapter createSecurityFilterTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.SecurityManagerType <em>Security Manager Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.SecurityManagerType
	 * @generated
	 */
	public Adapter createSecurityManagerTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.SecurityProviderType <em>Security Provider Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.SecurityProviderType
	 * @generated
	 */
	public Adapter createSecurityProviderTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.SystemEntryType <em>System Entry Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.SystemEntryType
	 * @generated
	 */
	public Adapter createSystemEntryTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.SystemPropertyType <em>System Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.SystemPropertyType
	 * @generated
	 */
	public Adapter createSystemPropertyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.TextPropertyType <em>Text Property Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.TextPropertyType
	 * @generated
	 */
	public Adapter createTextPropertyTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.ThreadingProfileType <em>Threading Profile Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.ThreadingProfileType
	 * @generated
	 */
	public Adapter createThreadingProfileTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.TransactionManagerType <em>Transaction Manager Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.TransactionManagerType
	 * @generated
	 */
	public Adapter createTransactionManagerTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.TransactionType <em>Transaction Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.TransactionType
	 * @generated
	 */
	public Adapter createTransactionTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.TransformersType <em>Transformers Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.TransformersType
	 * @generated
	 */
	public Adapter createTransformersTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link org.mule.schema.TransformerType <em>Transformer Type</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see org.mule.schema.TransformerType
	 * @generated
	 */
	public Adapter createTransformerTypeAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} //MuleAdapterFactory
