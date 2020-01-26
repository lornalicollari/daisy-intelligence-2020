import com.google.cloud.vision.v1.AnnotateImageResponse
import com.google.cloud.vision.v1.BoundingPoly
import ij.IJ
import mu.KotlinLogging
import java.awt.Color
import java.io.File
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

val DIRECTORY: File = Paths.get("").toAbsolutePath().resolve("res/full/ad-pages").toFile()

fun main() {
    getAnnotatedImages(DIRECTORY).take(5)
            .forEach { (image, annotations) -> findAdBlocks(image, annotations) }
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