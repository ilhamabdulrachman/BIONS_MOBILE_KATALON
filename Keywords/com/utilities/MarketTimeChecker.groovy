package com.utilities

import com.kms.katalon.core.util.KeywordUtil
import java.net.Socket
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Matcher
import java.util.regex.Pattern


class MarketTimeChecker {

    /**
     * servers = list of maps: [name: "...", host: "...", port: 62229]
     * timeoutSeconds = how long to listen after connect
     */
    def checkServers(List<Map> servers, int timeoutSeconds = 6) {
        File outFile = new File("Reports/market_time_log.txt")
        if (!outFile.parentFile.exists()) outFile.parentFile.mkdirs()
        outFile.append("=== Market time check run: ${ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))} ===\n\n")

        servers.each { srv ->
            outFile.append("Checking ${srv.name} (${srv.host}:${srv.port})\n")
            KeywordUtil.logInfo("🔍 Checking ${srv.name} (${srv.host}:${srv.port})")
            try {
                Socket socket = new Socket(srv.host, srv.port)
                socket.soTimeout = 3000  // 3s for per read
                KeywordUtil.logInfo("✅ Connected to ${srv.host}:${srv.port}")
                outFile.append("Connected\n")

                InputStream is = socket.getInputStream()
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
                long endAt = System.currentTimeMillis() + (timeoutSeconds * 1000)
                byte[] buffer = new byte[4096]
                boolean gotAny = false

                while (System.currentTimeMillis() < endAt) {
                    try {
                        int avail = is.available()
                        int read = is.read(buffer, 0, buffer.length)
                        if (read > 0) {
                            gotAny = true
                            baos.write(buffer, 0, read)
                            // convert chunk to text (best-effort)
                            String chunk = null
                            try {
                                chunk = new String(buffer, 0, read, "UTF-8")
                            } catch (Exception e) {
                                chunk = null
                            }
                            String hex = bytesToHex(buffer, 0, read)
                            KeywordUtil.logInfo("📩 Raw chunk (text): " + (chunk?:"<non-text>"))
                            KeywordUtil.logInfo("📩 Raw chunk (hex): " + hex)
                            outFile.append("CHUNK TEXT: " + (chunk?:"<non-text>") + "\n")
                            outFile.append("CHUNK HEX: " + hex + "\n")
                        } else {
                            // no data that moment
                            Thread.sleep(200)
                        }
                    } catch (java.net.SocketTimeoutException ste) {
                        // just continue waiting until overall timeout
                    }
                }

                byte[] all = baos.toByteArray()
                if (all.length == 0) {
                    KeywordUtil.logInfo("⚠️ No data received from ${srv.name}")
                    outFile.append("NO DATA RECEIVED\n\n")
                } else {
                    String allText = null
                    try { allText = new String(all, "UTF-8") } catch (Exception e) { allText = null }
                    outFile.append("FULL TEXT: " + (allText?:"<non-text>") + "\n")
                    KeywordUtil.logInfo("🔎 Parsing received data for timestamps...")

                    // try find ISO datetimes
                    def iso = findIsoDateTime(allText)
                    if (iso) {
                        outFile.append("FOUND ISO DATETIME: ${iso}\n")
                        KeywordUtil.logInfo("FOUND ISO DATETIME: ${iso}")
                    }

                    // try find epoch numbers (10 or 13 digits)
                    def epoch = findEpoch(all)
                    if (epoch) {
                        outFile.append("FOUND EPOCH: ${epoch} -> " + epochToJakarta(epoch) + "\n")
                        KeywordUtil.logInfo("FOUND EPOCH: ${epoch} -> " + epochToJakarta(epoch))
                    }

                    // also try to extract any keywords for market status
                    def status = findStatusKeyword(allText)
                    if (status) {
                        outFile.append("POSSIBLE STATUS KEYWORDS FOUND: ${status}\n")
                        KeywordUtil.logInfo("POSSIBLE STATUS KEYWORDS: ${status}")
                    }

                    outFile.append("\n")
                }

                socket.close()
            } catch (Exception e) {
                KeywordUtil.logWarning("❌ Cannot connect/read ${srv.name}: " + e.message)
                outFile.append("ERROR: " + e.message + "\n\n")
            }
        }

        KeywordUtil.logInfo("📁 Market time check finished. See Reports/market_time_log.txt")
    }

    // --- helpers ---

    private static String bytesToHex(byte[] bytes, int off, int len) {
        StringBuilder sb = new StringBuilder(len * 2)
        for (int i = off; i < off + len; i++) {
            sb.append(String.format("%02x", bytes[i] & 0xff))
        }
        return sb.toString()
    }

    private static String findIsoDateTime(String text) {
        if (!text) return null
        // match common ISO patterns e.g. 2025-09-19T15:04:10Z or 2025-09-19 15:04:10
        Pattern p = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:?\\d{2})?")
        Matcher m = p.matcher(text)
        if (m.find()) return m.group(0)
        return null
    }

    private static Long findEpoch(byte[] allBytes) {
        // try to find ascii digits sequences of length 10 or 13 in text
        String asText = null
        try { asText = new String(allBytes, "UTF-8") } catch (Exception e) { asText = null }
        if (asText) {
            Pattern p10 = Pattern.compile("\\b(\\d{10})\\b")
            Matcher m10 = p10.matcher(asText)
            if (m10.find()) {
                return Long.parseLong(m10.group(1))
            }
            Pattern p13 = Pattern.compile("\\b(\\d{13})\\b")
            Matcher m13 = p13.matcher(asText)
            if (m13.find()) {
                return Long.parseLong(m13.group(1))
            }
        }
        // also try to scan raw bytes for 4- or 8-byte big-endian ints that look like unix time
        // read as unsigned 4-byte ints and check plausible range (from 2000-01-01 to 2050)
        for (int i = 0; i + 4 <= allBytes.length; i++) {
            long v = ((allBytes[i] & 0xffL) << 24) | ((allBytes[i+1] & 0xffL) << 16) | ((allBytes[i+2] & 0xffL) << 8) | (allBytes[i+3] & 0xffL)
            if (v > 946684800L && v < 2556144000L) { // between 2000 and ~2050 in seconds
                return v
            }
        }
        // scan for 8-byte (ms) values (big-endian)
        for (int i = 0; i + 8 <= allBytes.length; i++) {
            long v = 0L
            for (int j = 0; j < 8; j++) {
                v = (v << 8) | (allBytes[i+j] & 0xffL)
            }
            if (v > 946684800000L && v < 2556144000000L) { // plausible millis
                return v
            }
        }
        return null
    }

    private static String epochToJakarta(Long epoch) {
        if (!epoch) return null
        // decide if epoch in seconds (10 digits) or milliseconds (13 digits)
        Instant inst = (epoch.toString().length() >= 13) ? Instant.ofEpochMilli(epoch) : Instant.ofEpochSecond(epoch)
        ZonedDateTime z = ZonedDateTime.ofInstant(inst, ZoneId.of("Asia/Jakarta"))
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return z.format(fmt)
    }

    private static String findStatusKeyword(String text) {
        if (!text) return null
        def keywords = ["market", "status", "open", "close", "open_time", "close_time", "trading", "serverTime", "time", "bursa"]
        def found = []
        def lower = text.toLowerCase()
        keywords.each { k -> if (lower.contains(k.toLowerCase())) found << k }
        return found ? found.join(", ") : null
    }
}
