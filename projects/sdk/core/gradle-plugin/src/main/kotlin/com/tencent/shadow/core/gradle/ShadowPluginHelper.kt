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
import com.tencent.shadow.core.gradle.extensions.PluginApkConfig
import com.tencent.shadow.core.gradle.extensions.PluginBuildType
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.experimental.and

open class ShadowPluginHelper {
    companion object {
        fun getFileMD5(file: File): String? {
            if (!file.isFile) {
                return null
            }

            val buffer = ByteArray(1024)
            var len: Int
            var inStream: FileInputStream? = null
            val digest = MessageDigest.getInstance("MD5")
            try {
                inStream = FileInputStream(file)
                do {
                    len = inStream.read(buffer, 0, 1024)
                    if (len != -1) {
                        digest.update(buffer, 0, len)
                    }
                } while (len != -1)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            } finally {
                inStream?.close()
            }
            return bytes2HexStr(digest.digest())
        }

        private fun bytes2HexStr(bytes: ByteArray?): String {
            val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
            if (bytes == null || bytes.isEmpty()) {
                return ""
            }

            val buf = CharArray(2 * bytes.size)
            try {
                for (i in bytes.indices) {
                    var b = bytes[i]
                    buf[2 * i + 1] = HEX_ARRAY[(b and 0xF).toInt()]
                    b = b.toInt().ushr(4).toByte()
                    buf[2 * i + 0] = HEX_ARRAY[(b and 0xF).toInt()]
                }
            } catch (e: Exception) {
                return ""
            }

            return String(buf)
        }

        /**
         * 处理manager apk 文件复制 Manager Apk 不需要获取方法 但是Manager Apk 文件的名称 需要外部设定
         */
        fun copyManagerApkFile(project: Project, buildType: PluginBuildType){

            val packagePlugin = project.extensions.findByName("packagePlugin")
            val extension = packagePlugin as PackagePluginExtension
            val managerApkName :String = buildType.managerApkConfig.first
            println("build Type Config ${buildType.managerApkConfig} ")
            val splitList = buildType.managerApkConfig.second.split(":")
            val modelName = splitList[1]
            val managerFileParent =
                splitList[splitList.lastIndex].replace("assemble", "").toLowerCase()
            val managerFile = File(
                "${project.rootDir}" +
                        "/${extension.managerApkProjectPath}/build/outputs/apk/$managerFileParent/$managerApkName"
            )

            val file = File("${project.rootDir}/${extension.hostManagerApkPath}/$modelName/$managerFileParent")

            if (!file.exists() && !managerFile.exists()) {
                throw IllegalArgumentException(managerFile.absolutePath + " , manager file not exist... --拷贝 manager 文件失败 请先打包插件后操作")
            }

            if (!file.exists())
                file.mkdirs()

            val copyFile = File(
                "${project.rootDir}/${extension.hostManagerApkPath}/$modelName/$managerFileParent/$managerApkName"

            )
            if (managerFile.exists()) { //如果 loader 文件存在 存在 就替换它
                println("更新 manager copy file  ")
                if (copyFile.exists())
                    copyFile.delete()
                copyFile.createNewFile()//重建新的文件
                FileUtils.copyInputStreamToFile(managerFile.inputStream(), copyFile) //拷贝文件到 复制文件内
            } else {
                println("没有找到 manager 文件更新包 本次构建 manager copy file 插件未更新")

            }
            println("managerFile copy done file path = $copyFile")

        }

        /** 因为 打包发布apk 文件 会删除 build output apk 文件夹，
         * 导致无法 生成 json 配置文件。
         * 所以 拷贝 打包的apk 文件到 output文件夹，
         * 所有文件从 output 文件夹 拿
         * 注意此方法可能 无法拿到loader 的文件 因为 发布打包时 as 会删除apk文件
         */
        fun copyLoaderApkFile(project: Project, buildType: PluginBuildType) {
            val packagePlugin = project.extensions.findByName("packagePlugin")
            val extension = packagePlugin as PackagePluginExtension
            val loaderApkName: String = buildType.loaderApkConfig.first
            val splitList = buildType.loaderApkConfig.second.split(":")
            val loaderFileParent =
                splitList[splitList.lastIndex].replace("assemble", "").toLowerCase()
            val loaderFile = File(
                "${project.rootDir}" +
                        "/${extension.loaderApkProjectPath}/build/outputs/apk/$loaderFileParent/$loaderApkName"
            )

            //android studio 创建的 loader 文件存在 开始更新拷贝 loader 文件
            val file = File(
                "${project.rootDir}" +
                        "/${extension.loaderApkProjectPath}/build/outputs/$loaderFileParent"
            )
            if (!file.exists() && !loaderFile.exists()) {
                throw IllegalArgumentException(loaderFile.absolutePath + " , loader file not exist... --拷贝 loader 文件失败 请先打包插件后操作")
            }

            if (!file.exists())
                file.mkdir()

            val copyFile = File(
                "${project.rootDir}" +
                        "/${extension.loaderApkProjectPath}/build/outputs/$loaderFileParent/$loaderApkName"
            )
            if (loaderFile.exists()) { //如果 loader 文件存在 存在 就替换它
                println("更新 loader copy file  ")
                if (copyFile.exists())
                    copyFile.delete()
                copyFile.createNewFile()//重建新的文件
                FileUtils.copyInputStreamToFile(loaderFile.inputStream(), copyFile) //拷贝文件到 复制文件内
            } else {
                println("没有找到 loader 文件更新包 本次构建 loader copy file 插件未更新")

            }
            println("loaderFile copy done file path =  $copyFile")

        }

        fun copyRuntimeApkFile(project: Project, buildType: PluginBuildType) {
            val packagePlugin = project.extensions.findByName("packagePlugin")
            val extension = packagePlugin as PackagePluginExtension
            val runtimeApkName: String = buildType.runtimeApkConfig.first
            val splitList = buildType.runtimeApkConfig.second.split(":")
            val runtimeFileParent =
                splitList[splitList.lastIndex].replace("assemble", "").toLowerCase()
            val runtimeFile = File(
                "${project.rootDir}" +
                        "/${extension.runtimeApkProjectPath}/build/outputs/apk/$runtimeFileParent/$runtimeApkName"
            )

            //android studio 创建的 loader 文件存在 开始更新拷贝 loader 文件
            val file = File(
                "${project.rootDir}" +
                        "/${extension.runtimeApkProjectPath}/build/outputs/$runtimeFileParent"
            )
            if (!file.exists() && !runtimeFile.exists()) {
                throw IllegalArgumentException(runtimeFile.absolutePath + " , runtime file not exist... --拷贝 loader 文件失败 请先打包插件后操作")
            }

            if (!file.exists())
                file.mkdir()

            val copyFile = File(
                "${project.rootDir}" +
                        "/${extension.runtimeApkProjectPath}/build/outputs/$runtimeFileParent/$runtimeApkName"
            )
            if (runtimeFile.exists()) { //如果 loader 文件存在 并且拷贝文件存在 就替换它
                println("更新 runtime copy file  ")
                if (copyFile.exists())
                    copyFile.delete()
                copyFile.createNewFile()//重建新的文件
                FileUtils.copyInputStreamToFile(runtimeFile.inputStream(), copyFile) //拷贝文件到 复制文件内
            } else {
                println("没有找到 runtime 文件更新包 本次构建 runtime copy file 插件未更新")

            }
            println("runtimeFile copy done file path = $copyFile")

        }

        /**
         * Android studio  会删除build 文件，所以plugin apk 也需要 拷贝到 output 文件夹下
         */
        fun copyPluginFile(
            project: Project,
            pluginConfig: PluginApkConfig,
        ) {

            val pluginFile = File(project.rootDir, pluginConfig.apkPath)
            val file = File( "${project.rootDir.absolutePath}/${pluginConfig.copyPath}")
            if (!pluginFile.exists() && !file.exists()) {
                throw IllegalArgumentException(pluginFile.absolutePath + " , plugin file not exist...--拷贝 plugin 文件失败 请先打包插件后操作")
            }

            if (!file.exists())
                file.mkdir()

            val copyFile = File("${project.rootDir.absolutePath}/${pluginConfig.copyPath}/${pluginConfig.apkName}")
            if (pluginFile.exists()) { //有新的 插件apk 文件更新插件 拷贝文件
                println("更新 plugin copy file  $copyFile")
                if (copyFile.exists())
                    copyFile.delete()
                copyFile.createNewFile()
                FileUtils.copyInputStreamToFile(pluginFile.inputStream(), copyFile)
            } else {
                println("没有找到 plugin 文件更新包 本次构建 plugin copy file 插件未更新")
            }

            println("copy plugin file $pluginFile to = $copyFile")

        }


        // 从配置文件中 获取runtime apk 文件
        fun getRuntimeApkFile(project: Project, buildType: PluginBuildType, checkExist: Boolean): File {
            val packagePlugin = project.extensions.findByName("packagePlugin")
            val extension = packagePlugin as PackagePluginExtension

            val splitList = buildType.runtimeApkConfig.second.split(":")
            val runtimeFileParent = splitList[splitList.lastIndex].replace("assemble", "").toLowerCase()
            val runtimeApkName: String = buildType.runtimeApkConfig.first
            val runtimeFile = File("${project.rootDir}" +
                    "/${extension.runtimeApkProjectPath}/build/outputs/$runtimeFileParent/$runtimeApkName")
            if (checkExist && !runtimeFile.exists()) {
                throw IllegalArgumentException(runtimeFile.absolutePath + " , runtime file not exist...")
            }
            println("runtimeFile path = $runtimeFile")
            return runtimeFile
        }

        fun getLoaderApkFile(project: Project, buildType: PluginBuildType, checkExist: Boolean): File {
            val packagePlugin = project.extensions.findByName("packagePlugin")
            val extension = packagePlugin as PackagePluginExtension

            val loaderApkName: String = buildType.loaderApkConfig.first
            val splitList = buildType.loaderApkConfig.second.split(":")
            val loaderFileParent = splitList[splitList.lastIndex].replace("assemble", "").toLowerCase()
            val loaderFile = File("${project.rootDir}" +
                    "/${extension.loaderApkProjectPath}/build/outputs/$loaderFileParent/$loaderApkName")
            if (checkExist && !loaderFile.exists()) {
                throw IllegalArgumentException(loaderFile.absolutePath + " , loader file not exist...")
            }
            println("loaderFile path = $loaderFile")
            return loaderFile

        }

        fun getPluginFile(project: Project, pluginConfig: PluginApkConfig, checkExist: Boolean): File {
            val pluginFile = File(project.rootDir, pluginConfig.apkPath)
            if (checkExist && !pluginFile.exists()) {
                throw IllegalArgumentException(pluginFile.absolutePath + " , plugin file not exist...")
            }
            println("pluginFile path = $pluginFile")
            return pluginFile
        }
    }
}