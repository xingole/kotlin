/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.repl

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

class FirTestReplCompilerExtensionRegistrar(
    private val hostConfiguration: ScriptingHostConfiguration
) : FirExtensionRegistrar() {

    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirTestReplSnippetConfiguratorExtensionImpl.getFactory(hostConfiguration)
        +FirTestReplSnippetResolveExtensionImpl.getFactory(hostConfiguration)
        +Fir2IrReplSnippetConfiguratorExtensionImpl.getFactory(hostConfiguration)
    }
}

@OptIn(ExperimentalCompilerApi::class)
class TestReplCompilerPluginRegistrar(val hostConfiguration: ScriptingHostConfiguration) : CompilerPluginRegistrar() {
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        with(this) {
            FirExtensionRegistrarAdapter.registerExtension(FirTestReplCompilerExtensionRegistrar(hostConfiguration))
        }
    }

    override val supportsK2: Boolean
        get() = true
}
