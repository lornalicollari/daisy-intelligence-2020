package util

import Dimensions
import Rectangle
import Vertex
import XComponent
import YComponent
import bounding
import com.google.cloud.vision.v1.Block
import com.google.cloud.vision.v1.TextAnnotation
import ext.asRectangle
import ext.text
import mu.KotlinLogging
import org.apache.commons.math3.ml.clustering.Cluster
import org.apache.commons.math3.ml.clustering.DBSCANClusterer
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.nield.kotlinstatistics.Centroid
import org.nield.kotlinstatistics.ClusterInput
import org.nield.kotlinstatistics.DoublePoint
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

@JvmName("dbScanClusterForRectangles")
inline fun <T> Collection<T>.dbScanCluster(
        maximumRadius: Double,
        minPoints: Int,
        crossinline selector: (T) -> Rectangle
): List<Centroid<T>> {
    return dbScanCluster(
            maximumRadius,
            minPoints,
            {
                doubleArrayOf(
                        selector(it).topLeft.x.value,
                        selector(it).topLeft.y.value,
                        selector(it).dimensions.x.value,
                        selector(it).dimensions.y.value
                )
            },
            { a, b ->
                val rectA = Rectangle(Vertex(XComponent(a[0]), YComponent(a[1])), Dimensions(XComponent(a[2]), YComponent(a[3])))
                val rectB = Rectangle(Vertex(XComponent(b[0]), YComponent(b[1])), Dimensions(XComponent(b[2]), YComponent(b[3])))

                distance(rectA, rectB)
            }
    )
}

infix fun ClosedRange<Double>.intersects(other: ClosedRange<Double>): Boolean {
    return this.contains(other.start) || this.contains(other.endInclusive) || other.contains(this.start)
}

inline fun <T> Collection<T>.dbScanCluster(
        maximumRadius: Double,
        minPoints: Int,
        crossinline selector: (T) -> DoubleArray,
        crossinline distance: (a: DoubleArray, b: DoubleArray) -> Double
): List<Centroid<T>> {
    return asSequence().map { ClusterInput(it, selector(it)) }
            .toList()
            .let {
                DBSCANClusterer<ClusterInput<T>>(maximumRadius, minPoints, DistanceMeasure { a, b -> distance(a, b) })
                        .cluster(it)
                        .map {
                            Centroid(DoublePoint(-1.0, -1.0), it.points.map { it.item })
                        }
            }
}

fun overlapInX(a: Rectangle, b: Rectangle): Boolean {
    return (a.left.value..a.right.value) intersects (b.left.value..b.right.value)
}

fun overlapInY(a: Rectangle, b: Rectangle): Boolean {
    return (a.top.value..a.bottom.value) intersects (b.top.value..b.bottom.value)
}

fun overlap(a: Rectangle, b: Rectangle): Boolean {
    return overlapInX(a, b) && overlapInY(a, b)
}

fun overlapFully(a: Rectangle, b: Rectangle): Boolean {
    return a > b && a.topLeft < b.topLeft && a.bottomRight > b.bottomRight
}

fun distance(a: XComponent, b: XComponent): Double {
    return abs((a - b).value)
}

fun distance(a: YComponent, b: YComponent): Double {
    return abs((a - b).value)
}

fun distance(a: Rectangle, b: Rectangle): Double {
    val distanceInX = min(distance(a.left, b.right), distance(a.right, b.left))
    val distanceInY = min(distance(a.bottom, b.top), distance(a.top, b.bottom))

    return when {
        overlap(a, b) -> 0.0
        overlapInX(a, b) -> distanceInY
        overlapInY(a, b) -> distanceInX
        else -> sqrt(distanceInX.pow(2.0) + distanceInY.pow(2.0))
    }
}

fun Collection<Block>.cluster(
        maxDistance: Double = 50.0,
        minCount: Int = 1,
        minDimensions: Dimensions = Dimensions(XComponent(0.0), YComponent(0.0))
): Set<Set<Block>> {
    fun neighbors(items: Set<Block>, clusters: Set<Set<Block>>): Set<Block> {
        val neighbors = items.flatMap { a ->
            this.filter { b ->
                if (a === b) return@filter true

                val leftAligned = abs((a.boundingBox.asRectangle().left - b.boundingBox.asRectangle().left).value) < 15
                val rightAligned = abs((a.boundingBox.asRectangle().right - b.boundingBox.asRectangle().right).value) < 15
                val centerAligned = abs((a.boundingBox.asRectangle().centroid.x - b.boundingBox.asRectangle().centroid.x).value) < 40
                val distance = distance(a.boundingBox.asRectangle(), b.boundingBox.asRectangle()) < maxDistance
                val clustered = clusters.any { cluster -> b in cluster }

                (leftAligned || rightAligned || centerAligned) && distance && !clustered
            }
        }.toSet()
        return if (items.containsAll(neighbors) || neighbors.isEmpty()) neighbors
        else neighbors(neighbors, clusters)
    }

    return this.fold(emptySet()) { clusters, block ->
        when {
            clusters.any { cluster -> block in cluster } -> clusters
            else -> {
                val neighbors = neighbors(setOf(block), clusters)
                when {
                    neighbors.size >= minCount && neighbors.bounding().dimensions >= minDimensions &&
                            neighbors.bounding().dimensions.height >= minDimensions.height &&
                            neighbors.bounding().dimensions.width >= minDimensions.width -> {
                        clusters + setOf(neighbors)
                    }
                    else -> clusters
                }
            }
        }
    }
}