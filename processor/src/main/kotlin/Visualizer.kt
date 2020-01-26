import ij.IJ
import ij.process.ImageProcessor
import java.io.File

fun visualize(image: File, visualizer: ImageProcessor.() -> Unit) {
    IJ.openImage(image.path).also { visualizer(it.processor) }.show()
}