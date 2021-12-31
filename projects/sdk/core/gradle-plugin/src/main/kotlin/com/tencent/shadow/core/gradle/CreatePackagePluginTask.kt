/*
 * Tencent is pleased to support the open source community by making Tencent Shadow available.
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tencent.shadow.core.gradle

import com.tencent.shadow.core.gradle.extensions.PackagePluginExtension
import com.tencent.shadow.core.gradle.extensions.PluginBuildType
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Zip
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

internal fun createPackagePluginTask(project: Project, buildType: PluginBuildType): Task {
    return project.tasks.create("package${buildType.name.capitalize()}Plugin", Zip::class.java) {
        println("PackagePluginTask task run")

        //runtime apk file
        val runtimeApkName: String = buildType.runtimeApkConfig.first
        var runtimeFile: File? = null
        if (runtimeApkName.isNotEmpty()) {
            runtimeFile = ShadowPluginHelper.getRuntimeApkFile(project, buildType, false)
        }


        //loader apk file
        val loaderApkName: String = buildType.loaderApkConfig.first
        var loaderFile: File? = null
        if (loaderApkName.isNotEmpty()) {
            loaderFile = ShadowPluginHelper.getLoaderApkFile(project, buildType, false)
        }


        //config file
        val targetConfigFile =
            File(project.buildDir.absolutePath + "/intermediates/generatePluginConfig/${buildType.name}/config.json")
        targetConfigFile.parentFile.mkdirs()

        //all plugin apks
        val pluginFiles: MutableList<File> = mutableListOf()
        for (i in buildType.pluginApks) {
            pluginFiles.add(ShadowPluginHelper.getPluginFile(project, i, false))
        }


        it.group = "plugin"
        it.description = "打包插件"
        it.outputs.upToDateWhen { false }
        if (runtimeFile != null) {
            pluginFiles.add(runtimeFile)
        }
        if (loaderFile != null) {
            pluginFiles.add(loaderFile)
        }
        it.from(pluginFiles, targetConfigFile)

        val packagePlugin = project.extensions.findByName("packagePlugin")
        val extension = packagePlugin as PackagePluginExtension

        val prefix = if (extension.archivePrefix.isEmpty()) "plugin" else extension.archivePrefix
        it.archiveName = "$prefix-${buildType.name}.zip"
        it.destinationDir =
            File(if (extension.destinationDir.isEmpty()) "${project.rootDir}/build" else extension.destinationDir)
    }.dependsOn(createGenerateConfigTask(project, buildType))
}

private fun createGenerateConfigTask(project: Project, buildType: PluginBuildType): Task {
    println("GenerateConfigTask task run")
    val packagePlugin = project.extensions.findByName("packagePlugin")
    val extension = packagePlugin as PackagePluginExtension

    /**
     * 因为Android studio 发布打包会 删除 build apk 文件所以 只有在 统一 打包脚本内 拷贝文件到 build output 内
     */
    //manager apk build task
    val managerApkName = buildType.runtimeApkConfig.first
    var managerTask = ""
    if (managerApkName.isNotEmpty()) {
        managerTask = buildType.managerApkConfig.second
        println("manager task = $managerTask")
    } else {
        throw RuntimeException("managerApkConfig is Null 没有设置manager")
    }


    //runtime apk build task
    val runtimeApkName = buildType.runtimeApkConfig.first
    var runtimeTask = ""
    if (runtimeApkName.isNotEmpty()) {
        runtimeTask = buildType.runtimeApkConfig.second
        println("runtime task = $runtimeTask")
    } else {
        throw RuntimeException("runTimeApkConfig is null 请配置runtime ")
    }


    //loader apk build task
    val loaderApkName = buildType.loaderApkConfig.first
    var loaderTask = ""
    if (loaderApkName.isNotEmpty()) {
        loaderTask = buildType.loaderApkConfig.second
        println("loader task = $loaderTask")
    } else {
        throw RuntimeException("loaderApkConfig is null 请配置loader")
    }


    val targetConfigFile =
        File(project.buildDir.absolutePath + "/intermediates/generatePluginConfig/${buildType.name}/config.json")


    val pluginApkTasks: MutableList<String> = mutableListOf()
    for (i in buildType.pluginApks) {
        val task = i.buildTask
        println("pluginApkProjects task = $task")
        pluginApkTasks.add(task)
    }

    val task = project.tasks.create("generate${buildType.name.capitalize()}Config") {
        it.group = "plugin"
        it.description = "生成插件配置文件"
        it.outputs.file(targetConfigFile)
        it.outputs.upToDateWhen { false }
    }
        .dependsOn(pluginApkTasks)
        .doFirst {
            println("copy Apk task begin")

            ShadowPluginHelper.copyManagerApkFile(project, buildType)

            ShadowPluginHelper.copyLoaderApkFile(project, buildType)

            ShadowPluginHelper.copyRuntimeApkFile(project, buildType)

            for (i in buildType.pluginApks) { //复制 插件apk 文件 可以为多个
                ShadowPluginHelper.copyPluginFile(project, i)
            }

            println("copy Apk task done")
        }
        .doLast {

            println("generateConfig task begin")
            val json = extension.toJson(project, loaderApkName, runtimeApkName, buildType)

            val bizWriter = BufferedWriter(FileWriter(targetConfigFile))
            bizWriter.write(json.toJSONString())
            bizWriter.newLine()
            bizWriter.flush()
            bizWriter.close()

            println("generateConfig task done")
        }

    if (managerTask.isNotEmpty()) {
        task.dependsOn(managerTask)
    }
    if (loaderTask.isNotEmpty()) {
        task.dependsOn(loaderTask)
    }
    if (runtimeTask.isNotEmpty()) {
        task.dependsOn(runtimeTask)
    }
    return task
}