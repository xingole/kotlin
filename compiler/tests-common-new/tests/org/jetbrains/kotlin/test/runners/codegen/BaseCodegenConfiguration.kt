/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners.codegen

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.test.Constructor
import org.jetbrains.kotlin.test.HandlersStepBuilder
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.backend.handlers.*
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.*
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.DUMP_SMAP
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives.RUN_DEX_CHECKER
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.*
import org.jetbrains.kotlin.test.services.configuration.CommonEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.configuration.JvmForeignAnnotationsConfigurator
import org.jetbrains.kotlin.test.services.configuration.ScriptingEnvironmentConfigurator
import org.jetbrains.kotlin.test.services.sourceProviders.*

fun <F : ResultingArtifact.FrontendOutput<F>, B : ResultingArtifact.BackendInput<B>> TestConfigurationBuilder.commonConfigurationForTest(
    targetFrontend: FrontendKind<F>,
    frontendFacade: Constructor<FrontendFacade<F>>,
    frontendToBackendConverter: Constructor<Frontend2BackendConverter<F, B>>,
    commonServicesConfiguration: (FrontendKind<*>) -> Unit = { commonServicesConfigurationForCodegenTest(it) }
) {
    commonServicesConfiguration(targetFrontend)
    facadeStep(frontendFacade)
    classicFrontendHandlersStep()
    firHandlersStep()
    facadeStep(frontendToBackendConverter)
    irHandlersStep(init = {})
    facadeStep(::JvmIrBackendFacade)
    jvmArtifactsHandlersStep(init = {})
}

fun TestConfigurationBuilder.commonServicesConfigurationForCodegenAndDebugTest(targetFrontend: FrontendKind<*>) {
    globalDefaults {
        frontend = targetFrontend
        targetPlatform = JvmPlatforms.defaultJvmPlatform
        dependencyKind = DependencyKind.Binary
    }

    defaultDirectives {
        +RUN_DEX_CHECKER
    }

    useConfigurators(
        ::CommonEnvironmentConfigurator,
        ::JvmEnvironmentConfigurator,
        ::ScriptingEnvironmentConfigurator,
        ::JvmForeignAnnotationsConfigurator,
    )

    useAdditionalSourceProviders(
        ::AdditionalDiagnosticsSourceFilesProvider,
        ::CoroutineHelpersSourceFilesProvider,
        ::CodegenHelpersSourceFilesProvider
    )
}

fun TestConfigurationBuilder.commonServicesConfigurationForCodegenTest(targetFrontend: FrontendKind<*>) {
    commonServicesConfigurationForCodegenAndDebugTest(targetFrontend)
    useAdditionalSourceProviders(
        ::MainFunctionForBlackBoxTestsSourceProvider
    )
}

fun TestConfigurationBuilder.commonServicesConfigurationForDebugTest(targetFrontend: FrontendKind<*>) {
    commonServicesConfigurationForCodegenAndDebugTest(targetFrontend)
    useAdditionalSourceProviders(
        ::MainFunctionForDebugTestsSourceProvider
    )
}

fun TestConfigurationBuilder.useInlineHandlers() {
    configureJvmArtifactsHandlersStep {
        useHandlers(
            ::BytecodeInliningHandler,
            ::SMAPDumpHandler
        )
    }

    forTestsMatching("compiler/testData/codegen/boxInline/smap/*") {
        defaultDirectives {
            +DUMP_SMAP
        }
    }
}

fun TestConfigurationBuilder.useIrInliner() {
    defaultDirectives {
        +LanguageSettingsDirectives.ENABLE_JVM_IR_INLINER
    }
}

fun TestConfigurationBuilder.useInlineScopesNumbers() {
    defaultDirectives {
        +LanguageSettingsDirectives.USE_INLINE_SCOPES_NUMBERS
    }
}

fun TestConfigurationBuilder.configureDumpHandlersForCodegenTest() {
    configureIrHandlersStep {
        useHandlers(
            ::IrTreeVerifierHandler,
            ::IrTextDumpHandler,
            ::IrMangledNameAndSignatureDumpHandler,
        )
    }
    configureJvmArtifactsHandlersStep {
        useHandlers(::BytecodeListingHandler)
    }
}

fun TestConfigurationBuilder.configureCommonHandlersForBoxTest() {
    commonHandlersForCodegenTest()
    configureJvmArtifactsHandlersStep {
        useHandlers(::JvmBoxRunner)
    }
}

fun TestConfigurationBuilder.commonHandlersForCodegenTest() {
    configureClassicFrontendHandlersStep {
        commonClassicFrontendHandlersForCodegenTest()
    }

    configureFirHandlersStep {
        commonFirHandlersForCodegenTest()
    }
    configureJvmArtifactsHandlersStep {
        commonBackendHandlersForCodegenTest()
    }
}

fun HandlersStepBuilder<ClassicFrontendOutputArtifact, FrontendKinds.ClassicFrontend>.commonClassicFrontendHandlersForCodegenTest() {
    useHandlers(
        ::NoCompilationErrorsHandler,
    )
}

fun HandlersStepBuilder<FirOutputArtifact, FrontendKinds.FIR>.commonFirHandlersForCodegenTest() {
    useHandlers(
        ::NoFirCompilationErrorsHandler,
    )
}

fun HandlersStepBuilder<BinaryArtifacts.Jvm, ArtifactKinds.Jvm>.commonBackendHandlersForCodegenTest() {
    useHandlers(
        ::JvmBackendDiagnosticsHandler,
        ::NoJvmSpecificCompilationErrorsHandler,
        ::DxCheckerHandler,
    )
}

fun TestConfigurationBuilder.configureModernJavaTest(jdkKind: TestJdkKind, jvmTarget: JvmTarget) {
    defaultDirectives {
        JvmEnvironmentConfigurationDirectives.JDK_KIND with jdkKind
        JvmEnvironmentConfigurationDirectives.JVM_TARGET with jvmTarget
        +ConfigurationDirectives.WITH_STDLIB
        +CodegenTestDirectives.IGNORE_DEXING
    }
}
