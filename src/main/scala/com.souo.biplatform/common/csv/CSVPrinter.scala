package com.souo.biplatform.common.csv

/**
 * @author souo
 */
class CSVPrinter(implicit val format: CSVFormat) {

  private[this] val quoteMinimalSpecs = Array('\r', '\n', format.quoteChar, format.delimiter)

  def printRow(fields: Seq[Any]): String = {
    def shouldQuote(field: String, quoting: Quoting): Boolean =
      quoting match {
        case QUOTE_ALL ⇒ true
        case QUOTE_MINIMAL ⇒
          var i = 0
          while (i < field.length) {
            val char = field(i)
            var j = 0
            while (j < quoteMinimalSpecs.length) {
              val quoteSpec = quoteMinimalSpecs(j)
              if (quoteSpec == char) {
                return true
              }
              j += 1
            }
            i += 1
          }
          false
        case QUOTE_NONE ⇒ false
        case QUOTE_NONNUMERIC ⇒
          var foundDot = false
          var i = 0
          while (i < field.length) {
            val char = field(i)
            if (char == '.') {
              if (foundDot) {
                return true
              }
              else {
                foundDot = true
              }
            }
            else if (char < '0' || char > '9') {
              return true
            }
            i += 1
          }
          false
      }
    val sb = new StringBuilder()
    def printField(field: String): Unit = {
      if (shouldQuote(field, format.quoting)) {
        sb.append(format.quoteChar)
        var i = 0
        while (i < field.length) {
          val char = field(i)
          if (char == format.quoteChar || (format.quoting == QUOTE_NONE && char == format.delimiter)) {
            sb.append(format.quoteChar)
          }
          sb.append(char)
          i += 1
        }
        sb.append(format.quoteChar)
      }
      else {
        sb.append(field)
      }
    }

    val iterator = fields.iterator
    var hasNext = iterator.hasNext
    while (hasNext) {
      val next = iterator.next()
      if (next != null) {
        printField(next.toString)
      }
      hasNext = iterator.hasNext
      if (hasNext) {
        sb.append(format.delimiter)
      }
    }
    sb.toString()
  }
}