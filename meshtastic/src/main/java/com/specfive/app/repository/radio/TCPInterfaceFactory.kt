package com.specfive.app.repository.radio

import dagger.assisted.AssistedFactory

/**
 * Factory for creating `TCPInterface` instances.
 */
@AssistedFactory
interface TCPInterfaceFactory : InterfaceFactorySpi<TCPInterface>