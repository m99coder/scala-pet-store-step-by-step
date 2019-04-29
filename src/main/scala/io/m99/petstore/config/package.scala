package io.m99.petstore

import io.circe.Decoder
import io.circe.generic.semiauto._

package object config {
  implicit val databaseConnectionsConfigDecoder: Decoder[DatabaseConnectionsConfig] = deriveDecoder
  implicit val databaseConfigDecoder: Decoder[DatabaseConfig]                       = deriveDecoder
  implicit val serverConfigDecoder: Decoder[ServerConfig]                           = deriveDecoder
  implicit val petStoreConfigDecoder: Decoder[PetStoreConfig]                       = deriveDecoder
}
