package me.sgeb.gradle.nativeartifacts

import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.internal.reflect.Instantiator

class NativeComponentContainer extends AbstractNamedDomainObjectContainer<NativeComponent> {

    NativeComponentContainer(Instantiator instantiator) {
        super(NativeComponent, instantiator)
    }

    @Override
    protected NativeComponent doCreate(String name) {
        return instantiator.newInstance(NativeComponent, name)
    }
}
