package ext

fun <T> Collection<T>.repeat(times: Int): Iterable<T> {
    val list = mutableListOf<T>()
    for (i in 0..times) list.addAll(this)
    return list
}