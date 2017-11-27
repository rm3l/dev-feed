package org.rm3l.iana.service_names_port_numbers.domain

data class Record (
        val serviceName: String? = null /*Service Name*/,
        val portNumber: Long? = null /*Port Number*/,
        val transportProtocol: Protocol? = null /*Transport Protocol*/,
        val description: String? = null /*Description*/,
        val assignee: Person? = null /*Assignee*/,
        val contact: Person? = null /*Contact*/,
        val registrationDate: String? = null /*Registration Date*/,
        val modificationDate: String? = null /*Modification Date*/,
        val reference: String? = null /*Reference*/,
        val serviceCode: String? = null /*Service Code*/,
        val knownUnauthorizedUses: String? = null /*Unauthorized Uses*/,
        val assignmentNotes: String? = null /*Assignment Notes*/)

data class RecordFilter (
        val services: List<String>? = null,
        val protocols: List<Protocol>? = null,
        val ports: List<Long>? = null)

data class Person (
        val id: String,
        val name: String,
        val org: String? = null,
        val uri: String? = null,
        val updated: String? = null)

enum class Protocol {
    TCP,
    UDP,
    DCCP,
    SCTP
}