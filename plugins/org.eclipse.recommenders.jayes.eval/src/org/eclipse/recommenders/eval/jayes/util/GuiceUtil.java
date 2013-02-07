/*******************************************************************************
 * Copyright (c) 2013 Michael Kutschke.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Michael Kutschke - initial API and implementation
 ******************************************************************************/
package org.eclipse.recommenders.eval.jayes.util;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

public class GuiceUtil {

    @SuppressWarnings("unchecked")
    public static List<Module> loadModules(Iterable<String> moduleNames) throws ClassNotFoundException,
            InstantiationException,
            IllegalAccessException {
        List<Module> guiceModules = newArrayList();
        for (String moduleName : moduleNames) {
            String fullname = "org.eclipse.recommenders.internal.jayes.eval.wiring." + moduleName + "Module";
            Class<? extends Module> clazz = (Class<? extends Module>) Class.forName(fullname);
            guiceModules.add(clazz.newInstance());
        }
        return guiceModules;
    }

    public static <T> T getNamedInstance(Module module, Class<T> clazz, String name) {
        Injector inject = Guice.createInjector(module);
        return inject.getInstance(Key.get(clazz, Names.named(name)));
    }

}
