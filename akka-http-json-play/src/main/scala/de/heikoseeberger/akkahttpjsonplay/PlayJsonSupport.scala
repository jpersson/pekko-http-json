/*
 * Copyright 2015 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.akkahttpjsonplay

import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.model.{ ContentTypes, HttpCharsets, MediaTypes }
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import akka.stream.FlowMaterializer
import play.api.libs.json.{ JsResultException, JsValue, Json, Reads, Writes }

/**
 * Automatic to and from JSON marshalling/unmarshalling using an in-scope *play-json* protocol.
 */
object PlayJsonSupport extends PlayJsonSupport

/**
 * Automatic to and from JSON marshalling/unmarshalling using an in-scope *play-json* protocol.
 */
trait PlayJsonSupport {

  implicit def playJsonUnmarshallerConverter[A](reads: Reads[A])(implicit mat: FlowMaterializer): FromEntityUnmarshaller[A] =
    playJsonUnmarshaller(reads, mat)

  implicit def playJsonUnmarshaller[A](implicit reads: Reads[A], mat: FlowMaterializer): FromEntityUnmarshaller[A] = {
    def read(json: JsValue) = reads.reads(json).recoverTotal(error => throw JsResultException(error.errors))
    playJsValueUnmarshaller.map(read)
  }

  implicit def playJsValueUnmarshaller(implicit mat: FlowMaterializer): FromEntityUnmarshaller[JsValue] =
    Unmarshaller.byteStringUnmarshaller
      .forContentTypes(MediaTypes.`application/json`)
      .mapWithCharset { (data, charset) =>
        val input = if (charset == HttpCharsets.`UTF-8`) data.utf8String else data.decodeString(charset.nioCharset.name)
        Json.parse(input)
      }

  implicit def playJsonMarshallerConverter[A](writes: Writes[A])(implicit printer: JsValue => String = Json.prettyPrint): ToEntityMarshaller[A] =
    playJsonMarshaller[A](writes)

  implicit def playJsonMarshaller[A](implicit writes: Writes[A], printer: JsValue => String = Json.prettyPrint): ToEntityMarshaller[A] =
    playJsValueMarshaller.compose(writes.writes)

  implicit def playJsValueMarshaller(implicit printer: JsValue => String = Json.prettyPrint): ToEntityMarshaller[JsValue] =
    Marshaller.StringMarshaller.wrap(ContentTypes.`application/json`)(printer)
}
