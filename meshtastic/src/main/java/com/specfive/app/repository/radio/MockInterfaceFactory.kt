package com.specfive.app.repository.radio

import dagger.assisted.AssistedFactory

/**
 * Factory for creating `MockInterface` instances.
 */
@AssistedFactory
interface MockInterfaceFactory : InterfaceFactorySpi<MockInterface>