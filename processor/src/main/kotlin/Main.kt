import com.google.cloud.vision.v1.AnnotateImageResponse
import com.google.cloud.vision.v1.BoundingPoly
import ij.IJ
import mu.KotlinLogging
import java.awt.Color
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

val RESOURCES_DIR: File = Paths.get("").toAbsolutePath().resolve("res/").toFile()
val DIRECTORY: File = RESOURCES_DIR.resolve("full/ad-pages")

fun main() {
    val productDictionary = buildDictionary("product_dictionary")
    val unitDictionary = buildDictionary("units_dictionary")
    val productNames = getAnnotatedImages(DIRECTORY).take(1)
            .map { (image, annotations) ->
                findAdBlocks(image, annotations)
                        .asSequence()
                        .map { adBlock ->
                            (getProductName(adBlock, productDictionary ) to getProductUnits(adBlock, unitDictionary) )
                        }
                        .filter { it.first!!.second > 80 }
                        .forEach { logger.info { "Product Name: ${it.first}  Units: ${it.second}" } }
            }
            .toList()
}

fun annotateImage(image: File, response: AnnotateImageResponse) {
    val imageJ = IJ.openImage(image.path)
    imageJ.processor.let { p ->
        p.lineWidth = 2

        response.textAnnotationsList.forEach { annotation ->
            p.setColor(Color.RED)
            val (x, y, width, height) = annotation.boundingPoly.rect()
            p.drawRect(x, y, width, height)
        }

        response.fullTextAnnotation.pagesList[0].blocksList.forEach { block ->
                p.setColor(Color.BLUE)
            val (x, y, width, height) = block.boundingBox.rect()
            p.drawRect(x, y, width, height)
        }
    }
    imageJ.show()
}

fun BoundingPoly.rect(): List<Int> {
    val minX = verticesList.map { it.x }.min()!!
    val maxX = verticesList.map { it.x }.max()!!
    val minY = verticesList.map { it.y }.min()!!
    val maxY = verticesList.map { it.y }.max()!!
    return listOf(minX, minY, maxX - minX, maxY - minY)
}