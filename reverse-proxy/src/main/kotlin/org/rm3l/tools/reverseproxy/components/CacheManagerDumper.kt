package org.rm3l.tools.reverseproxy.components

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.fasterxml.jackson.databind.ObjectMapper
import org.rm3l.tools.reverseproxy.configuration.CacheManagerWrapper
import org.rm3l.tools.reverseproxy.configuration.auto.cache.keys.CacheKeysExtractorFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

//TODO Change version if format of dumps is changed
const val SER_VER = "1.0.0"

@Component
class CacheManagerDumper(val objectMapper: ObjectMapper, val cacheManagers: List<CacheManagerWrapper>?) {

    private val logger = LoggerFactory.getLogger(CacheManagerDumper::class.java)

    @Value("\${cache.dumpDir}")
    private val dumpDir: String = "/tmp/reverse-proxy/.caches"

    private val kryo = Kryo()

    private fun createAndGetCacheManagerDumpDir(cacheManager: CacheManagerWrapper): File? {
        val cacheManagerDumpDirName = "$dumpDir/$SER_VER/${cacheManager.getCacheManagerName()}"
        val cacheManagerDumpDir =File(cacheManagerDumpDirName)
        if (!(cacheManagerDumpDir.exists() || cacheManagerDumpDir.mkdirs())) {
            logger.warn("Failed to create directory: '$cacheManagerDumpDirName' => no (de-)serialization of caches")
            return null
        }
        return cacheManagerDumpDir
    }

    private fun getDumpFile(cacheManager: CacheManagerWrapper, cacheName: String): File? {
        val cacheManagerDumpDir = createAndGetCacheManagerDumpDir(cacheManager)?:return null
        return File(cacheManagerDumpDir, "$cacheName.bin")
    }

    //Preload the caches from content saved last time
    @PostConstruct
    private fun init() {
        logger.info("Initializing caches")
        cacheManagers?.forEach { cacheManager ->
            //Get cache manager name from dump files created
            val cacheManagerDumpDir = createAndGetCacheManagerDumpDir(cacheManager) ?: return
            cacheManagerDumpDir.listFiles().map { it.nameWithoutExtension }
                    .forEach { cacheName ->
                        var dumpFile: Input? = null
                        try {
                            val file = getDumpFile(cacheManager, cacheName)?:return
                            dumpFile = Input(FileInputStream(file))
                            val cache = cacheManager.getCache(cacheName)
                            val cacheDumpContentString = kryo.readObject(dumpFile, String::class.java)
                            @Suppress("UNCHECKED_CAST")
                            val cacheDumpContentList =
                                    objectMapper.readValue(cacheDumpContentString, List::class.java)
                                            as List<LinkedHashMap<String, Any>>
                            cacheDumpContentList
                                    .map {
                                        //TODO Find a better way to handle this
                                        @Suppress("UNCHECKED_CAST")
                                        objectMapper.readValue(
                                                objectMapper.writeValueAsString(it),
                                                ProxyResponseCacheDumpEntry::class.java)
                                                as ProxyResponseCacheDumpEntry<String>
                                    }
                                    .forEach { cache.put(it.key, ResponseEntity(it.body, it.httpStatus)) }
                        } catch (e: Exception) {
                            if (logger.isDebugEnabled) {
                                logger.debug(e.message, e)
                            }
                        } finally {
                            dumpFile?.close()
                        }
                    }
        }
    }

    //Persists the content of the caches to disk, so as to reload them next time
    @PreDestroy
    private fun destroy() {
        logger.info("Dumping caches to disk")
        cacheManagers?.forEach { cacheManager ->
            cacheManager.cacheNames.forEach { cacheName ->
                var dumpFile: Output? = null
                try {
                    val file = getDumpFile(cacheManager, cacheName)?:return
                    dumpFile = Output(FileOutputStream(file))
                    val cache = cacheManager.getCache(cacheName)
                    val cacheContent = CacheKeysExtractorFactory.getKeyExtractor(cache).extractKeys(cache)
                            .map { it to cache.get(it).get() as ResponseEntity<*> }
                            .map { ProxyResponseCacheDumpEntry(it.first as String, it.second.body.toString(), it.second.statusCode) }
                            .toList()
                    kryo.writeObject(dumpFile, objectMapper.writeValueAsString(cacheContent))
                    dumpFile.close()
                } catch (e: Exception) {
                    if (logger.isDebugEnabled) {
                        logger.debug(e.message, e)
                    }
                } finally {
                    dumpFile?.close()
                }
            }
        }
    }

}

data class ProxyResponseCacheDumpEntry<out T>(val key: String, val body: T, val httpStatus: HttpStatus)
