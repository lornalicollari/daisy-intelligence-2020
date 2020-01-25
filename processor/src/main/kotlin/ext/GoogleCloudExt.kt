package ext

import Dimensions
import Rectangle
import Vertex
import XComponent
import YComponent
import com.google.cloud.vision.v1.BoundingPoly

fun BoundingPoly.asRectangle(): Rectangle {
    check(verticesList.isNotEmpty()) { "BoundingPoly has no vertices." }

    val (minX, maxX) = verticesList.map { XComponent(it.x.toDouble()) }.let { it.min()!! to it.max()!! }
    val (minY, maxY) = verticesList.map { YComponent(it.y.toDouble()) }.let { it.min()!! to it.max()!! }

    return Rectangle(Vertex(minX, minY), Dimensions(maxX - minX, maxY - minY))
}