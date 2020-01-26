import com.github.doyaaaaaken.kotlincsv.dsl.csvReader

fun buildDictionary(filename: String): Set<String> {
    return csvReader().open(RESOURCES_DIR.resolve("csv/$filename.csv")) {
        readAllAsSequence().drop(1).map { line -> line.first() }.toSet()
    }
}
