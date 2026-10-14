package ceneax.app.lib.redux.compiler

import ceneax.app.lib.redux.annotation.PageRoute
import com.google.auto.service.AutoService
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
class ReduxCompiler : AbstractProcessor() {
    private var mPackageName = ""
    private var mClassName = ""

    private companion object {
        // kapt 生成目录锚点(路径形如 .../app/build/generated/source/kaptKotlin/<variant>)
        val KAPT_GENERATED_DIR_ANCHOR =
            "${File.separatorChar}build${File.separatorChar}generated${File.separatorChar}source${File.separatorChar}kaptKotlin"
    }

    private val mPageRouteMap = mutableMapOf<String, String>()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        processingEnv.options["kapt.kotlin.generated"]?.let { kaptKotlinGeneratedDir ->
            mPackageName = DEFAULT_PACKAGE
            mClassName = getModuleName(kaptKotlinGeneratedDir, KAPT_GENERATED_DIR_ANCHOR)
        }
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return linkedSetOf(
            PageRoute::class.java.canonicalName
        )
    }

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(PageRoute::class.java).forEach {
            if (it.kind != ElementKind.CLASS) {
                println("PageRoute 注解必须在 Class 上使用")
            } else {
                val annotation = it.getAnnotation(PageRoute::class.java)
                mPageRouteMap[annotation.path] = it.toString()
            }
        }

        processingEnv.options["kapt.kotlin.generated"]?.let { kaptKotlinGeneratedDir ->
            val generator = ReduxGenerator(mPackageName, mClassName, mPageRouteMap)
            generator.generate().writeTo(File(kaptKotlinGeneratedDir))
        }

        return false
    }
}