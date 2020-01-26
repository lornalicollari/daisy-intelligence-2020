import com.google.cloud.vision.v1.AnnotateImageResponse
import com.google.cloud.vision.v1.Block
import ext.asRectangle
import ext.draw
import ext.fill
import ext.repeat
import mu.KotlinLogging
import util.cluster
import util.distance
import util.overlap
import java.awt.Color
import java.io.File

private val logger = KotlinLogging.logger {}

fun findAdBlocks(image: File, annotations: AnnotateImageResponse): Set<Set<Block>> {

    val clusters: Set<Set<Block>> = annotations.fullTextAnnotation.pagesList
            .flatMap { it.blocksList }
            .cluster()
            .eatContained()
            .eatNearby()
            .eatContained()
            .filter { cluster -> cluster.size > 2 && cluster.bounding().dimensions.width.value > 150 && cluster.bounding().dimensions.height.value > 150 }
            .toSet()

    visualize(image) {

        val colors = listOf(Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.ORANGE, Color.PINK, Color.MAGENTA, Color.LIGHT_GRAY, Color.CYAN, Color.PINK)
                .repeat(20)
        (clusters zip colors).forEach { (cluster, color) ->
            setColor(color)
            cluster.forEach { point ->
                fill(point.boundingBox.asRectangle())
            }
            lineWidth = 5
            draw(cluster.bounding())
        }
    }

    return clusters
}

fun Set<Block>.bounding(): Rectangle {
    return bounding(this.map { it.boundingBox.asRectangle() })
}

fun Set<Set<Block>>.eatContained(): Set<Set<Block>> {
    return this.filter { a -> this.none { b -> overlap(a.bounding(), b.bounding()) && b.bounding() > a.bounding() } }
            .map { a ->
                a + this.filter { b -> overlap(a.bounding(), b.bounding()) && b.bounding() < a.bounding() }.flatten()
            }.toSet()
}

fun Set<Set<Block>>.eatNearby(): Set<Set<Block>> {

    fun isNearbyAndSmaller(base: Rectangle, target: Rectangle): Boolean {
        return target.dimensions < base.dimensions * 0.7 && distance(base, target) < 40
    }

    fun isNearbyAndSmaller(base: Set<Block>, target: Set<Block>): Boolean {
        return isNearbyAndSmaller(base.bounding(), target.bounding())
    }

    return this
            .filter { base ->
                this.none { target -> isNearbyAndSmaller(target, base) }
            }
            .map { base ->
                base + this.filter { target -> isNearbyAndSmaller(base, target) }.flatten().toSet()
            }.toSet()
}
