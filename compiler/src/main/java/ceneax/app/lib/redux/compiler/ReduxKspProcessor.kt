package ceneax.app.lib.redux.compiler

import ceneax.app.lib.redux.annotation.PageRoute
import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import java.io.File

/**
 * KSP 版路由注册表生成器（与 kapt 版 [ReduxCompiler] 语义对等）：
 * 扫描 @PageRoute 注解 → 生成 Redux{模块名}Module 注册表类。
 *
 * 同一 artifact 内双注册（kapt 走 @AutoService(Processor)，ksp 走 @AutoService(SymbolProcessorProvider)），
 * 接入方 kapt / ksp 二选一引用本 artifact 即可，生成产物一致，
 * 运行期注册（ASM 插件扫 ReduxModule 接口注入）不受处理器方式影响。
 */
@AutoService(SymbolProcessorProvider::class)
class ReduxKspProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ReduxKspProcessor(environment)
    }
}

class ReduxKspProcessor(private val mEnvironment: SymbolProcessorEnvironment) : SymbolProcessor {

    private val mPageRouteMap = mutableMapOf<String, String>()
    private var mGenerated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(PageRoute::class.qualifiedName!!)
            .filterIsInstance<KSClassDeclaration>()
            .forEach {
                if (it.classKind != ClassKind.CLASS) {
                    mEnvironment.logger.warn("PageRoute 注解必须在 Class 上使用")
                } else {
                    val path = it.findPageRoutePath() ?: return@forEach
                    // 与 kapt 版对齐：map 值为类的 fully qualified name（生成 X::class.java 引用）
                    mPageRouteMap[path] = it.qualifiedName!!.asString()
                }
            }

        if (mPageRouteMap.isNotEmpty() && !mGenerated) {
            val moduleName = resolveModuleName(resolver)
            val generator = ReduxGenerator(DEFAULT_PACKAGE, moduleName, mPageRouteMap)
            val codeGenerator: CodeGenerator = mEnvironment.codeGenerator
            codeGenerator.createNewFile(Dependencies(false), DEFAULT_PACKAGE, moduleName).use { output ->
                output.write(generator.generate().toString().toByteArray(Charsets.UTF_8))
            }
            mGenerated = true
        }

        return emptyList()
    }

    /**
     * 提取 @PageRoute 的 path 参数（对齐 kapt 版 annotation.path）
     */
    private fun KSClassDeclaration.findPageRoutePath(): String? {
        return annotations
            .first { it.shortName.getShortName() == PageRoute::class.simpleName }
            .arguments
            .firstOrNull { it.name?.getShortName() == "path" }
            ?.value as? String
    }

    /**
     * 模块名推断，优先级：
     * 1. 显式 option redux.module.name（接入方 ksp { arg("redux.module.name", "xxx") }）
     * 2. 从本模块源文件路径推断（.../app/src/main/java/... 或 .../app/src/...，
     *    取 "/src/" 锚点前的最后一段 = 模块目录名，对齐 kapt 版从生成目录取模块名的行为）
     * 3. 兜底 ReduxDefaultModule（与 kapt 版 getModuleName 异常分支一致）
     */
    private fun resolveModuleName(resolver: Resolver): String {
        mEnvironment.options["redux.module.name"]?.let { return "Redux${it.camelCase()}Module" }
        resolver.getAllFiles().firstOrNull()?.filePath?.let { rawPath ->
            // KSP 的 filePath 为 URI 风格正斜杠（Windows 亦然），统一归一后推断
            val path = rawPath.replace('\\', '/')
            val srcIndex = path.lastIndexOf("/src/")
            if (srcIndex > 0) {
                val moduleDir = path.substring(0, srcIndex).substringAfterLast('/')
                if (moduleDir.isNotEmpty()) {
                    return "Redux${moduleDir.camelCase()}Module"
                }
            }
        }
        mEnvironment.logger.info("Redux KSP: 未能从源文件路径推断模块名，兜底为 ReduxDefaultModule")
        return "ReduxDefaultModule"
    }
}
