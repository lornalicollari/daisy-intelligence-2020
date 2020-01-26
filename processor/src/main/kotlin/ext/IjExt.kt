package ext

import Rectangle
import ij.process.ImageProcessor

fun ImageProcessor.draw(rectangle: Rectangle) {
    drawRect(
            rectangle.topLeft.x.value.toInt(), rectangle.topLeft.y.value.toInt(),
            rectangle.dimensions.width.value.toInt(), rectangle.dimensions.height.value.toInt()
    )
}

fun ImageProcessor.fill(rectangle: Rectangle) {
    fillRect(
            rectangle.topLeft.x.value.toInt(), rectangle.topLeft.y.value.toInt(),
            rectangle.dimensions.width.value.toInt(), rectangle.dimensions.height.value.toInt()
    )
}