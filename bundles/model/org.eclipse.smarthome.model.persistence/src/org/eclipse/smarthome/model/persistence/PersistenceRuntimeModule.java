/**
 * Copyright (c) 2014-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
/*
 * generated by Xtext
 */
package org.eclipse.smarthome.model.persistence;

import org.eclipse.smarthome.model.persistence.scoping.PersistenceGlobalScopeProvider;
import org.eclipse.xtext.generator.IGenerator;
import org.eclipse.xtext.generator.IGenerator.NullGenerator;
import org.eclipse.xtext.linking.lazy.LazyURIEncoder;
import org.eclipse.xtext.scoping.IGlobalScopeProvider;

import com.google.inject.Binder;
import com.google.inject.name.Names;

/**
 * Use this class to register components to be used at runtime / without the Equinox extension registry.
 */
public class PersistenceRuntimeModule extends org.eclipse.smarthome.model.persistence.AbstractPersistenceRuntimeModule {

    @Override
    public Class<? extends IGlobalScopeProvider> bindIGlobalScopeProvider() {
        return PersistenceGlobalScopeProvider.class;
    }

    @Override
    public Class<? extends IGenerator> bindIGenerator() {
        return NullGenerator.class;
    }

    @Override
    public void configureUseIndexFragmentsForLazyLinking(Binder binder) {
        binder.bind(Boolean.TYPE).annotatedWith(Names.named(LazyURIEncoder.USE_INDEXED_FRAGMENTS_BINDING))
                .toInstance(Boolean.FALSE);
    }
}
