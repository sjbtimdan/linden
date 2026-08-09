package org.sjbtimdan.linden.imports

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal fun buildIvyZip(json: String, charset: Charset = Charsets.UTF_16BE): ByteArray {
    val jsonBytes = charset.encode(json).let { buffer ->
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        bytes
    }
    val bos = ByteArrayOutputStream()
    ZipOutputStream(bos).use { zos ->
        zos.putNextEntry(ZipEntry("backup.json"))
        zos.write(jsonBytes)
        zos.closeEntry()
    }
    return bos.toByteArray()
}

internal val minimalIvyJson: String
    get() = """
        {
          "accounts": [
            {"id": "00000000-0000-0000-0000-000000000001", "name": "Wallet",      "currency": "USD"},
            {"id": "00000000-0000-0000-0000-000000000002", "name": "Euro Bank",   "currency": "EUR"},
            {"id": "00000000-0000-0000-0000-000000000003", "name": "UK Savings",  "currency": "GBP"}
          ],
          "categories": [
            {"id": "00000000-0000-0000-0000-000000000011", "name": "Food"},
            {"id": "00000000-0000-0000-0000-000000000012", "name": "Salary"},
            {"id": "00000000-0000-0000-0000-000000000013", "name": "General"}
          ],
          "transactions": [
            {
              "id": "00000000-0000-0000-0000-000000000021",
              "type": "EXPENSE",
              "amount": 45.50,
              "accountId": "00000000-0000-0000-0000-000000000001",
              "categoryId": "00000000-0000-0000-0000-000000000011",
              "title": "Grocery run",
              "dateTime": 946684800000
            },
            {
              "id": "00000000-0000-0000-0000-000000000022",
              "type": "INCOME",
              "amount": 3200.0,
              "accountId": "00000000-0000-0000-0000-000000000002",
              "categoryId": "00000000-0000-0000-0000-000000000012",
              "title": "June salary",
              "dateTime": 946771200000
            },
            {
              "id": "00000000-0000-0000-0000-000000000023",
              "type": "TRANSFER",
              "amount": 500.0,
              "accountId": "00000000-0000-0000-0000-000000000001",
              "toAccountId": "00000000-0000-0000-0000-000000000003",
              "toAmount": 500.0,
              "categoryId": "00000000-0000-0000-0000-000000000013",
              "title": "Transfer to savings",
              "dateTime": 946771200000
            },
            {
              "id": "00000000-0000-0000-0000-000000000024",
              "type": "EXPENSE",
              "amount": 5.75,
              "accountId": "00000000-0000-0000-0000-000000000001",
              "categoryId": "00000000-0000-0000-0000-000000000011",
              "title": "Coffee",
              "dateTime": 946684800000
            },
            {
              "id": "00000000-0000-0000-0000-000000000025",
              "type": "INCOME",
              "amount": 150.0,
              "accountId": "00000000-0000-0000-0000-000000000003",
              "categoryId": "00000000-0000-0000-0000-000000000012",
              "title": "Bonus",
              "dateTime": 946771200000
            }
          ]
        }
    """.trimIndent()
