package com.flowmoney.app.domain.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankNotificationParserRegistry @Inject constructor() {

    private val parsers = mutableListOf<BankNotificationParser>()
    private val genericParser = GenericBankParser()

    init {
        // Register top Vietnamese bank and fintech parsers
        registerParser(VietcombankParser())
        registerParser(MbBankParser())
        registerParser(TechcombankParser())
        registerParser(BidvParser())
        registerParser(MomoParser())
    }

    fun registerParser(parser: BankNotificationParser) {
        if (!parsers.contains(parser)) {
            parsers.add(parser)
        }
    }

    fun getAllParsers(): List<BankNotificationParser> = parsers.toList()

    fun findParser(packageName: String, title: String, text: String): BankNotificationParser? {
        // First check specific parsers
        val specific = parsers.firstOrNull { it.canParse(packageName, title, text) }
        if (specific != null) return specific

        // Fallback to generic parser if text contains bank transaction indicators
        return if (genericParser.canParse(packageName, title, text)) genericParser else null
    }

    fun parseNotification(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long = System.currentTimeMillis()
    ): ParsedBankNotification? {
        val parser = findParser(packageName, title, text) ?: return null
        return parser.parse(title, text, timestamp)
    }
}
