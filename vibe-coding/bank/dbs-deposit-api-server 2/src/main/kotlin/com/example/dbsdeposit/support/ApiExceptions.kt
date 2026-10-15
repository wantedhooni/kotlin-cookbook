package com.example.dbsdeposit.support

class ResourceNotFoundException(message: String) : RuntimeException(message)
class IdempotencyKeyConflictException(message: String) : RuntimeException(message)
class IdempotencyInProgressException(message: String) : RuntimeException(message)
class ConcurrentSyncException(message: String) : RuntimeException(message)
class ProviderCallException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
