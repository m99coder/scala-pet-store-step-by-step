package io.m99.petstore.config

final case class ServerConfig(host: String, port: Int)
final case class PetStoreConfig(database: DatabaseConfig, server: ServerConfig)
