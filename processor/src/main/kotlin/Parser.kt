import com.google.cloud.vision.v1.AnnotateImageResponse
import ext.asRectangle
import ext.draw
import ext.fill
import ext.repeat
import org.nield.kotlinstatistics.dbScanCluster
import org.nield.kotlinstatistics.multiKMeansCluster
import util.cluster
import util.dbScanCluster
import java.awt.Color
import java.io.File

fun findAdBlocks(image: File, annotations: AnnotateImageResponse) {

    val clusters = annotations.fullTextAnnotation.pagesList
            .flatMap { it.blocksList }
            .cluster()

    visualize(image) {
        setColor(Color(255, 0, 0, 125))
        annotations.fullTextAnnotation.pagesList.flatMap { it.blocksList }.forEach { block ->
            fill(block.boundingBox.asRectangle())
        }
    }

    visualize(image) {

        val colors = listOf(Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.PINK, Color.MAGENTA, Color.LIGHT_GRAY, Color.CYAN, Color.PINK)
                .repeat(20)
        (clusters zip colors).forEach { (cluster, color) ->
            setColor(color)
            cluster.forEach { point ->
                fill(point.boundingBox.asRectangle())
            }
        }
    }

}
