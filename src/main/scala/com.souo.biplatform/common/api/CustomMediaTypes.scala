package com.souo.biplatform.common.api

import akka.http.scaladsl.model.MediaType
import akka.http.scaladsl.model.HttpCharsets.`UTF-8`
/**
 * @author souo
 */
object CustomMediaTypes {
  /**
   * [[http://media-types.ietf.narkive.com/emZX0ly2/proposed-media-type-registration-for-yaml]].
   */
  val `text/vnd.yaml`: MediaType.WithFixedCharset =
    MediaType.customWithFixedCharset("text", "vnd.yaml", `UTF-8`)

  val `text/json`: MediaType.WithFixedCharset =
    MediaType.customWithFixedCharset("text", "json", `UTF-8`)
}
