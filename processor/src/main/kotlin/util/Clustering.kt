package util

import Dimensions
import Rectangle
import Vertex
import XComponent
import YComponent
import org.apache.commons.math3.ml.clustering.Cluster
import org.apache.commons.math3.ml.clustering.DBSCANClusterer
import org.apache.commons.math3.ml.distance.DistanceMeasure
import org.nield.kotlinstatistics.Centroid
import org.nield.kotlinstatistics.ClusterInput
import org.nield.kotlinstatistics.DoublePoint
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

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

                val overlapInX = (rectA.left.value..rectA.right.value) intersects (rectB.left.value..rectB.right.value)
                val overlapInY = (rectA.top.value..rectA.bottom.value) intersects (rectB.top.value..rectB.bottom.value)
                val distanceInX = abs((rectA.left - rectB.left).value)
                val distanceInY = abs((rectA.bottom - rectB.bottom).value)

                when {
                    overlapInX && overlapInY -> 0.0
                    overlapInX -> distanceInY
                    overlapInY -> distanceInX
                    else -> sqrt(distanceInX.pow(2.0) + distanceInY.pow(2.0))
                }
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