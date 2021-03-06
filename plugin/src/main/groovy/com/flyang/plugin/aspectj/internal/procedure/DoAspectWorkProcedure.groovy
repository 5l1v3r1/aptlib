/*
 * Copyright 2018 firefly1126, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.gradle_plugin_android_aspectjx
 */
package com.flyang.plugin.aspectj.internal.procedure

import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.flyang.plugin.aspectj.internal.AspectjTask
import com.flyang.plugin.aspectj.internal.AspectjTaskManager
import com.flyang.plugin.aspectj.internal.cache.VariantCache
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

/**
 * class description here
 * @author simon
 * @version 1.0.0
 * @since 2018-04-23
 */
class DoAspectWorkProcedure extends AbsProcedure {
    AspectjTaskManager aspectJTaskManager

    DoAspectWorkProcedure(Project project, VariantCache variantCache, TransformInvocation transformInvocation) {
        super(project, variantCache, transformInvocation)
        aspectJTaskManager = new AspectjTaskManager(encoding: aspectJCache.encoding, ajcArgs: aspectJCache.ajcArgs, bootClassPath: aspectJCache.bootClassPath,
                sourceCompatibility: aspectJCache.sourceCompatibility, targetCompatibility: aspectJCache.targetCompatibility)
    }

    @Override
    boolean doWorkContinuously() {
        //do aspectj real work
        project.logger.debug("~~~~~~~~~~~~~~~~~~~~do aspectj real work")
        aspectJTaskManager.aspectPath << variantCache.aspectDir
        aspectJTaskManager.classPath << variantCache.includeFileDir
        aspectJTaskManager.classPath << variantCache.excludeFileDir

        //process class files
        AspectjTask aspectJTask = new AspectjTask(project)
        File includeJar = transformInvocation.getOutputProvider().getContentLocation("include", variantCache.contentTypes,
                variantCache.scopes, Format.JAR)

        if (!includeJar.parentFile.exists()) {
            FileUtils.forceMkdir(includeJar.getParentFile())
        }

        FileUtils.deleteQuietly(includeJar)

        aspectJTask.outputJar = includeJar.absolutePath
        aspectJTask.inPath << variantCache.includeFileDir
        aspectJTaskManager.addTask(aspectJTask)

        //process jar files
        transformInvocation.inputs.each { TransformInput input ->
            input.jarInputs.each { JarInput jarInput ->
                aspectJTaskManager.classPath << jarInput.file

                if (variantCache.isIncludeJar(jarInput.file.absolutePath)) {
                    AspectjTask aspectJTask1 = new AspectjTask(project)
                    aspectJTask1.inPath << jarInput.file

                    File outputJar = transformInvocation.getOutputProvider().getContentLocation(jarInput.name, jarInput.getContentTypes(),
                            jarInput.getScopes(), Format.JAR)
                    if (!outputJar.getParentFile()?.exists()) {
                        outputJar.getParentFile()?.mkdirs()
                    }

                    aspectJTask1.outputJar = outputJar.absolutePath

                    aspectJTaskManager.addTask(aspectJTask1)
                }
            }
        }

        aspectJTaskManager.batchExecute()

        return true
    }
}
