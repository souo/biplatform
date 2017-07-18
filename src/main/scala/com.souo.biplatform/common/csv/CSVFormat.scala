package com.souo.biplatform.common.csv

/**
 * @author souo
 */
trait CSVFormat {

  val delimiter: Char

  val quoteChar: Char

  val escapeChar: Char

  val lineTerminator: String

  val quoting: Quoting

  val treatEmptyLineAsNil: Boolean

}

trait DefaultCSVFormat extends CSVFormat {

  val delimiter: Char = ','

  val quoteChar: Char = '"'

  val escapeChar: Char = '"'

  val lineTerminator: String = "\r\n"

  val quoting: Quoting = QUOTE_MINIMAL

  val treatEmptyLineAsNil: Boolean = false

}

trait TSVFormat extends CSVFormat {

  val delimiter: Char = '\t'

  val quoteChar: Char = '"'

  val escapeChar: Char = '\\'

  val lineTerminator: String = "\r\n"

  val quoting: Quoting = QUOTE_NONE

  val treatEmptyLineAsNil: Boolean = false

}
