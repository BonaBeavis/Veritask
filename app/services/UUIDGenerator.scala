package services

import java.util.UUID
import javax.inject.Singleton

import com.google.inject.ImplementedBy

/**
 * A type declaring the interface that will be injectable.
 */
@ImplementedBy(classOf[SimpleUUIDGenerator])
abstract class UUIDGenerator() {
  def generate: UUID
}

/**
 * A simple implementation of UUIDGenerator that we will inject.
 */
@Singleton
class SimpleUUIDGenerator extends UUIDGenerator {
  def generate: UUID = UUID.randomUUID()
}