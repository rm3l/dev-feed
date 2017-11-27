package org.rm3l.iana.service_names_port_numbers.parsers

import org.rm3l.iana.service_names_port_numbers.domain.Protocol
import org.rm3l.iana.service_names_port_numbers.domain.Record
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import org.w3c.dom.Node
import javax.xml.parsers.DocumentBuilderFactory

@Component(value = "xml")
class ServiceNamePortNumberXmlParser : ServiceNamePortNumberMappingParser {

    private val logger = LoggerFactory.getLogger(ServiceNamePortNumberXmlParser::class.java)

    override fun parse(content: String): List<Record> {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val documentParsed = dBuilder.parse(content.byteInputStream())
        documentParsed.documentElement.normalize()

        val records = mutableListOf<Record>()

        val recordNodeList = documentParsed.getElementsByTagName("record")
        for (i in 0 until recordNodeList.length) {
            val recordNode = recordNodeList.item(i)
            if (logger.isDebugEnabled) {
                logger.debug("Current element: ${recordNode.nodeName}")
            }
            if (recordNode.nodeType == Node.ELEMENT_NODE) {
                val element = recordNode as Element
                val serviceName = element.getElementsByTagName("name")?.item(0)?.textContent
                val protocolLowerCase = element.getElementsByTagName("protocol")?.item(0)?.textContent
                val description = element.getElementsByTagName("description")?.item(0)?.textContent
                val note = element.getElementsByTagName("note")?.item(0)?.textContent
                val portNumber = element.getElementsByTagName("number")?.item(0)?.textContent?.toLongOrNull()
                val record = Record(serviceName = serviceName,
                        portNumber = portNumber,
                        transportProtocol = if (protocolLowerCase != null) Protocol.valueOf(protocolLowerCase.toUpperCase()) else null,
                        description = description,
                        assignmentNotes = note)
                if (note != null && note.contains("should not be used for discovery purposes", ignoreCase = true)) {
                    if (logger.isDebugEnabled) {
                        logger.debug("Ignored record: $record since it should not be used for discovery purposes (per IANA recommendations)")
                    }
                    continue
                }
                records.add(record)
            }
        }

        return records.toList()
    }
}