package com.souo.biplatform.common.api

import java.util.UUID

import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import io.circe._
import io.circe.syntax._
import cats.syntax.either._
import com.souo.biplatform.model._
import com.souo.biplatform.queryrouter.DataCellType.DataCellType
import io.circe.Decoder.Result
import io.circe.generic.auto._

/**
 * Created by souo on 2016/12/20
 */
trait CirceCodec extends CirceEncoders with CirceDecoders {
  override val dateTimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
}

trait CirceEncoders {
  val dateTimeFormat: DateTimeFormatter

  implicit object DateTimeEncoder extends Encoder[DateTime] {
    override def apply(a: DateTime): Json = dateTimeFormat.print(a).asJson
  }

  implicit object UuidEncoder extends Encoder[UUID] {
    override def apply(u: UUID): Json = u.toString.asJson
  }

  implicit object DataCellTypeEncoder extends Encoder[DataCellType] {
    override def apply(a: DataCellType): Json = a.toString.asJson
  }

  implicit object filterEncoders extends Encoder[Filter] {
    override def apply(a: Filter): Json = a match {
      case x: lt ⇒
        Map("lt" → x).asJson
      case x: lte ⇒
        Map("lte" → x).asJson
      case x: gt ⇒
        Map("gt" → x).asJson
      case x: gte ⇒
        Map("gte" → x).asJson
      case x: eql ⇒
        Map("eql" → x).asJson
      case x: in ⇒
        Map("in" → x).asJson
      case x: range ⇒
        Map("range" → x).asJson
    }
  }

  implicit val dataSourceEncoder: Encoder[DataSource] = Encoder.instance {
    case csv: CsvSource     ⇒ csv.asJson
    case mysql: MysqlSource ⇒ mysql.asJson
  }

}

trait CirceDecoders {

  val dateTimeFormat: DateTimeFormatter

  implicit val dateTimeDecoder: Decoder[DateTime] = Decoder.decodeString.emap { str ⇒
    Either.catchNonFatal(dateTimeFormat.parseDateTime(str)).leftMap(t ⇒ t.getMessage)
  }

  implicit val uuidDecoder: Decoder[UUID] = Decoder.decodeString.map { str ⇒
    UUID.fromString(str)
  }

  implicit object FilterDecoder extends Decoder[Filter] {
    override def apply(c: HCursor): Result[Filter] = {
      c.fieldSet.map(_.toList) match {
        case None ⇒
          Left(DecodingFailure("empty filter", c.history))
        case Some("lt" :: Nil) ⇒
          c.get[lt]("lt").map[Filter](identity)
        case Some("lte" :: Nil) ⇒
          c.get[lte]("lte").map[Filter](identity)
        case Some("gt" :: Nil) ⇒
          c.get[gt]("gt").map[Filter](identity)
        case Some("gte" :: Nil) ⇒
          c.get[gte]("gte").map[Filter](identity)
        case Some("eql" :: Nil) ⇒
          c.get[eql]("eql").map[Filter](identity)
        case Some("range" :: Nil) ⇒
          c.get[range]("range").map[Filter](identity)
        case Some("in" :: Nil) ⇒
          c.get[in]("in").map[Filter](identity)
        case _ ⇒
          Left(DecodingFailure("error", c.history))
      }
    }
  }

  implicit val dataSourceDecoder: Decoder[DataSource] = {
    Decoder[CsvSource].map[DataSource](identity) or Decoder[MysqlSource].map[DataSource](identity)
  }

}

object CirceCodec extends CirceCodec