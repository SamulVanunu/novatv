package dev.anon.novatv.core.network

import java.io.IOException

class SourceAuthException(message: String) : IOException(message)

class SourceNetworkException(message: String, cause: Throwable? = null) : IOException(message, cause)

class SourceProtocolException(message: String, cause: Throwable? = null) : IOException(message, cause)
