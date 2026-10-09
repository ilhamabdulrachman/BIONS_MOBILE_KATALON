package com.utilities

import com.kms.katalon.core.util.KeywordUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.zip.Deflater
import java.util.zip.Inflater

import javax.crypto.Cipher

/**
* Minimal BIONS Feed/Trading socket client for Katalon.
*
* Create one instance per channel. Feed and Trading have independent TCP
* connections, session keys, server session IDs, and response decoding rules.
*/
class BionsSocketClient {

	static final String SOCKET_PUBLIC_KEY_BASE64 =
			'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDVd/gb2ORdLI7nTRHJR8C5EHs4RkRBcQuQdHkZ6eq0xnV2f0hkWC8h0mYH/bmelb5ribwulMwzFkuktXoufqzoft6Q6jLQRnkNJGRP6yA4bXqXfKYj1yeMusIPyIb3CTJT/gfZ40oli6szwu4DoFs66IZpJLv4qxU9hqu6NtJ+8QIDAQAB'

	static final String RSA_CIPHER_TRANSFORM = 'RSA/ECB/PKCS1Padding'
	static final int MAX_FRAME_BYTES = 10 * 1024 * 1024

	private Socket socket
	private InputStream inputStream
	private OutputStream outputStream
	private String channelType
	private String rc4Key
	private String serverSessionId
	private String lastFeedLoginTopic
	private String lastTradingLoginTopic
	private int orderSequence = 0

	final List<List> receivedMessages = []

	void connectSocket(String host, int port, int timeoutMs = 5000) {
		closeSocketInternal(false)
		resetSessionState()
		try {
			socket = new Socket()
			socket.connect(new InetSocketAddress(host, port), timeoutMs)
			socket.setTcpNoDelay(true)
			inputStream = socket.getInputStream()
			outputStream = socket.getOutputStream()
			KeywordUtil.logInfo("TCP connected to ${host}:${port}")
		} catch (Exception e) {
			closeSocketInternal(false)
			failAndStop("Gagal connect TCP socket ${host}:${port}: ${e.message}", e)
		}
	}

	void closeSocket() {
		closeSocketInternal(true)
	}

	private void closeSocketInternal(boolean writeLog) {
		try {
			socket?.close()
			if (writeLog) {
				KeywordUtil.logInfo('Socket ditutup')
			}
		} catch (Exception e) {
			KeywordUtil.markWarning('Error saat menutup socket: ' + e.message)
		} finally {
			socket = null
			inputStream = null
			outputStream = null
		}
	}

	private void resetSessionState() {
		channelType = null
		rc4Key = null
		serverSessionId = null
		lastFeedLoginTopic = null
		lastTradingLoginTopic = null
		receivedMessages.clear()
	}

	static String md5Hex(String input) {
		if (input == null) {
			throw new IllegalArgumentException('MD5 input tidak boleh null')
		}
		byte[] hashBytes = MessageDigest.getInstance('MD5').digest(input.getBytes('UTF-8'))
		StringBuilder result = new StringBuilder(hashBytes.length * 2)
		for (byte value : hashBytes) {
			result.append(String.format('%02x', value & 0xFF))
		}
		return result.toString()
	}

	static byte[] rc4(String key, byte[] data) {
		if (!key) {
			throw new IllegalStateException('RC4 session key belum tersedia')
		}
		byte[] keyBytes = key.getBytes('UTF-8')
		int[] state = new int[256]
		for (int index = 0; index < state.length; index++) {
			state[index] = index
		}
		int j = 0
		for (int index = 0; index < state.length; index++) {
			j = (j + state[index] + (keyBytes[index % keyBytes.length] & 0xFF)) & 0xFF
			int swap = state[index]
			state[index] = state[j]
			state[j] = swap
		}
		byte[] output = new byte[data.length]
		int i = 0
		j = 0
		for (int index = 0; index < data.length; index++) {
			i = (i + 1) & 0xFF
			j = (j + state[i]) & 0xFF
			int swap = state[i]
			state[i] = state[j]
			state[j] = swap
			int keyByte = state[(state[i] + state[j]) & 0xFF]
			output[index] = (byte) ((data[index] & 0xFF) ^ keyByte)
		}
		return output
	}

	static byte[] zlibCompress(byte[] data) {
		Deflater deflater = new Deflater()
		try {
			deflater.setInput(data)
			deflater.finish()
			ByteArrayOutputStream output = new ByteArrayOutputStream(data.length)
			byte[] buffer = new byte[4096]
			while (!deflater.finished()) {
				int count = deflater.deflate(buffer)
				output.write(buffer, 0, count)
			}
			return output.toByteArray()
		} finally {
			deflater.end()
		}
	}

	static byte[] zlibDecompress(byte[] data) {
		Inflater inflater = new Inflater()
		try {
			inflater.setInput(data)
			ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(data.length, 256))
			byte[] buffer = new byte[4096]
			while (!inflater.finished()) {
				int count = inflater.inflate(buffer)
				if (count > 0) {
					output.write(buffer, 0, count)
					continue
				}
				if (inflater.needsDictionary()) {
					throw new IOException('Response zlib membutuhkan dictionary yang tidak tersedia')
				}
				if (inflater.needsInput()) {
					throw new EOFException('Response zlib terpotong')
				}
				throw new IOException('Response zlib tidak dapat diproses')
			}
			return output.toByteArray()
		} finally {
			inflater.end()
		}
	}

	static byte[] rsaEncrypt(byte[] data) {
		byte[] keyBytes = Base64.getDecoder().decode(SOCKET_PUBLIC_KEY_BASE64)
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes)
		PublicKey publicKey = KeyFactory.getInstance('RSA').generatePublic(spec)
		Cipher cipher = Cipher.getInstance(RSA_CIPHER_TRANSFORM)
		cipher.init(Cipher.ENCRYPT_MODE, publicKey)
		return cipher.doFinal(data)
	}

	void sendConnect(String userId, String plainPassword, String channel, String sessionKeyUuid) {
		requireConnected()
		requireValue(userId, 'userId')
		requireValue(plainPassword, 'plainPassword')
		requireValue(sessionKeyUuid, 'sessionKeyUuid')
		channelType = channel?.toUpperCase()
		if (!(channelType in ['FEED', 'TRADING'])) {
			failAndStop("channelType harus FEED atau TRADING, diterima: ${channel}")
		}
		rc4Key = sessionKeyUuid
		int connectType = channelType == 'FEED' ? 2 : 1
		String passwordValue = md5Hex(plainPassword)
		if (channelType == 'FEED') {
			passwordValue += '|zaisan'
		}
		List connectPayload = [0, userId.toUpperCase(), passwordValue, connectType, sessionKeyUuid, 0, 2]
		try {
			byte[] plainBytes = JsonOutput.toJson(connectPayload).getBytes('UTF-8')
			writeFrame(rsaEncrypt(plainBytes))
			KeywordUtil.logInfo("${channelType} Connect terkirim untuk user ${userId.toUpperCase()}")
		} catch (Exception e) {
			failAndStop("Gagal mengirim ${channelType} Connect: ${e.message}", e)
		}
	}

	String receiveConnectResponse(int timeoutMs = 5000) {
		byte[] frame = readFrame(timeoutMs)
		if (frame == null) {
			failAndStop("Tidak ada response ${channelType} Connect dalam ${timeoutMs} ms")
		}
		try {
			byte[] decompressed = zlibDecompress(frame)
			byte[] plainBytes = channelType == 'FEED' ? rc4(rc4Key, decompressed) : decompressed
			List response = parseListJson(plainBytes, 'Connect response')
			if (response.size() < 2 || asInt(response[0]) != 1) {
				failAndStop("${channelType} Connect ditolak: ${safeJson(response)}")
			}
			serverSessionId = response[1]?.toString()
			requireValue(serverSessionId, 'serverSessionId')
			KeywordUtil.logInfo("${channelType} Connect berhasil; session ID diterima")
			return serverSessionId
		} catch (Exception e) {
			failAndStop("Gagal decode ${channelType} Connect response: ${e.message}", e)
			return null
		}
	}

	void debugRawPeek(int timeoutMs = 5000) {
		KeywordUtil.markWarning("debugRawPeek(${timeoutMs}) dinonaktifkan karena raw read merusak frame; gunakan receiveConnectResponse() atau receiveMessage()")
	}

	void sendMessage(List messageArray) {
		requireReadySession()
		try {
			byte[] plainBytes = JsonOutput.toJson(messageArray).getBytes('UTF-8')
			byte[] compressed = zlibCompress(rc4(rc4Key, plainBytes))
			writeFrame(compressed)
			KeywordUtil.logInfo("${channelType} message type ${messageArray[0]} terkirim: ${messageArray}")
		} catch (Exception e) {
			failAndStop("Gagal mengirim ${channelType} message: ${e.message}", e)
		}
	}

	List receiveMessage(int timeoutMs = 5000) {
		byte[] frame = readFrame(timeoutMs)
		if (frame == null || frame.length == 0) {
			return null
		}
		try {
			byte[] decompressed = zlibDecompress(frame)
			byte[] plainBytes = channelType == 'FEED' ? rc4(rc4Key, decompressed) : decompressed
			List message = parseListJson(plainBytes, "${channelType} message")
			receivedMessages << message
			KeywordUtil.logInfo("${channelType} response type ${message[0]} diterima: ${message}")
			return message
		} catch (Exception e) {
			KeywordUtil.markWarning("Frame ${channelType} tidak dapat di-decode: ${e.message}")
			return null
		}
	}

	void listen(int seconds) {
		receivedMessages.clear()
		long deadline = System.currentTimeMillis() + (seconds * 1000L)
		while (System.currentTimeMillis() < deadline) {
			int timeout = (int) Math.min(deadline - System.currentTimeMillis(), 1000L)
			if (timeout <= 0) {
				break
			}
			receiveMessage(timeout)
		}
	}

	boolean hasResponse() {
		return !receivedMessages.isEmpty()
	}

	void sendFeedLogin(String userId, String plainPassword, String clientIp, String appVersion, String platformInfo) {
		requireChannel('FEED')
		requireValue(clientIp, 'clientIp')
		requireValue(appVersion, 'appVersion')
		requireValue(platformInfo, 'platformInfo')
		lastFeedLoginTopic = "jms.topic.admin.${serverSessionId}.${System.currentTimeMillis() * 1000L}"
		String subscriptionId = "subs-${UUID.randomUUID()}"
		sendMessage([4, lastFeedLoginTopic, subscriptionId])
		List loginPayload = [1, serverSessionId, userId, plainPassword, clientIp, appVersion, platformInfo]
		sendMessage([6, 'jms.queue.admin', lastFeedLoginTopic, loginPayload])
		KeywordUtil.logInfo("Feed Login terkirim untuk user ${userId}")
	}

	Map waitForFeedLoginResponse(int timeoutMs = 15000) {
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			Map result = parseFeedLoginMessage(message)
			if (result != null) {
				if (!result.success) {
					failAndStop("Feed Login gagal: ${result.message}")
				}
				KeywordUtil.logInfo("Feed Login berhasil; gateway=${result.gatewayId}, autoRenew=${result.autoRenew}")
				return result
			}
		}
		failAndStop("Feed Login timeout setelah ${timeoutMs} ms")
		return [success: false, message: 'timeout']
	}

	Map parseFeedLoginResponse() {
		for (List message : receivedMessages) {
			Map result = parseFeedLoginMessage(message)
			if (result != null) {
				return result
			}
		}
		KeywordUtil.markFailed('Tidak ada response Feed Login yang valid')
		return [success: false, message: 'No valid Feed Login response']
	}

	Map loginFeed(String host, int port, String userId, String plainPassword, String clientIp, String appVersion = '4.17.5', String platformInfo = 'Android', int timeoutMs = 15000) {
		connectSocket(host, port)
		sendConnect(userId, plainPassword, 'FEED', UUID.randomUUID().toString())
		receiveConnectResponse(timeoutMs)
		sendFeedLogin(userId, plainPassword, clientIp, appVersion, platformInfo)
		return waitForFeedLoginResponse(timeoutMs)
	}

	static String buildTradingLoginFix(String userId, String plainPin, String clientIp, String platformInfo) {
		String separator = '\u0001'
		String body = "35=AA${separator}" + "10001=${userId}${separator}" + "10002=${md5Hex(plainPin)}${separator}" + "999930=${clientIp}${separator}" + "58=${platformInfo}${separator}" + "108=45${separator}"
		return "8=FIX.4.2${separator}9=${body.length()}${separator}${body}10=0"
	}

	void sendTradingLogin(String userId, String plainPin, String clientIp, String platformInfo) {
		requireChannel('TRADING')
		requireValue(clientIp, 'clientIp')
		requireValue(platformInfo, 'platformInfo')
		lastTradingLoginTopic = "jms.topic.trading.${serverSessionId}"
		String subscriptionId = "subs-${UUID.randomUUID()}"
		sendMessage([4, lastTradingLoginTopic, subscriptionId])
		String fixMessage = buildTradingLoginFix(userId, plainPin, clientIp, platformInfo)
		List loginPayload = [1, serverSessionId, userId, 'AA', fixMessage]
		sendMessage([6, 'jms.queue.trading', lastTradingLoginTopic, loginPayload])
		KeywordUtil.logInfo("Trading Login terkirim untuk user ${userId}")
	}

	Map waitForTradingLoginResponse(int timeoutMs = 15000) {
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			Map result = parseTradingLoginMessage(message)
			if (result != null) {
				if (!result.success) {
					failAndStop("Trading Login gagal: ${result.message ?: '-'}")
				}
				KeywordUtil.logInfo("Trading Login berhasil; gateway=${result.tag58Value ?: '-'}")
				return result
			}
		}
		failAndStop("Trading Login timeout setelah ${timeoutMs} ms")
		return [success: false, message: 'timeout']
	}

	Map parseTradingLoginResponse() {
		for (List message : receivedMessages) {
			Map result = parseTradingLoginMessage(message)
			if (result != null) {
				return result
			}
		}
		KeywordUtil.markFailed('Tidak ada response Trading Login yang valid')
		return [success: false, message: 'No valid Trading Login response']
	}

	Map loginTrading(String host, int port, String userId, String plainPassword, String plainPin, String clientIp, String platformInfo = 'Android', int timeoutMs = 15000) {
		connectSocket(host, port)
		sendConnect(userId, plainPassword, 'TRADING', UUID.randomUUID().toString())
		receiveConnectResponse(timeoutMs)
		sendTradingLogin(userId, plainPin, clientIp, platformInfo)
		return waitForTradingLoginResponse(timeoutMs)
	}

	static String extractFixTag(String fixString, String tagNumber) {
		if (!fixString) {
			return null
		}
		for (String field : fixString.split('\u0001')) {
			String[] keyValue = field.split('=', 2)
			if (keyValue.length == 2 && keyValue[0] == tagNumber) {
				return keyValue[1]
			}
		}
		return null
	}

	private Map parseFeedLoginMessage(List message) {
		List innerData = unwrapApplicationMessage(message)
		if (!innerData) {
			return null
		}
		int responseType = asInt(innerData[0])
		if (responseType == 2) {
			return [success: true, loginId: valueAt(innerData, 1), gatewayId: valueAt(innerData, 4), message: valueAt(innerData, 6), autoRenew: valueAt(innerData, 7)?.toString()?.equalsIgnoreCase('Y') ?: false]
		}
		if (responseType == 3) {
			return [success: false, message: valueAt(innerData, 2) ?: 'Unknown Feed Login failure']
		}
		if (responseType == 4) {
			return [success: false, message: 'Feed session killed by server']
		}
		return null
	}

	private Map parseTradingLoginMessage(List message) {
		List innerData = unwrapApplicationMessage(message)
		if (!innerData || innerData.size() < 5) {
			return null
		}
		int responseType = asInt(innerData[0])
		String fixMessage = innerData[4]?.toString()
		String tag58Value = extractFixTag(fixMessage, '58')
		if (responseType in [2, 6, 14]) {
			return [success: true, tag58Value: tag58Value, message: tag58Value]
		}
		if (responseType in [3, 4, 7]) {
			return [success: false, tag58Value: tag58Value, message: tag58Value ?: "type ${responseType}"]
		}
		return null
	}

	private static List unwrapApplicationMessage(List message) {
		if (message == null || message.size() < 4 || asInt(message[0]) != 7) {
			return null
		}
		return message[3] instanceof List ? (List) message[3] : null
	}

	private void writeFrame(byte[] payload) {
		requireConnected()
		if (payload.length == 0 || payload.length > MAX_FRAME_BYTES) {
			failAndStop("Ukuran frame tidak valid: ${payload.length}")
		}
		int length = payload.length
		byte[] prefix = [(byte) ((length >>> 24) & 0xFF), (byte) ((length >>> 16) & 0xFF), (byte) ((length >>> 8) & 0xFF), (byte) (length & 0xFF)] as byte[]
		outputStream.write(prefix)
		outputStream.write(payload)
		outputStream.flush()
	}

	private byte[] readFrame(int timeoutMs) {
		requireConnected()
		socket.setSoTimeout(timeoutMs)
		try {
			byte[] prefix = readExactly(4)
			if (prefix == null) {
				return null
			}
			int length = ((prefix[0] & 0xFF) << 24) | ((prefix[1] & 0xFF) << 16) | ((prefix[2] & 0xFF) << 8) | (prefix[3] & 0xFF)
			if (length <= 0 || length > MAX_FRAME_BYTES) {
				throw new IOException("Invalid frame length: ${length}")
			}
			return readExactly(length)
		} catch (SocketTimeoutException ignored) {
			return null
		}
	}

	private byte[] readExactly(int length) {
		byte[] result = new byte[length]
		int offset = 0
		while (offset < length) {
			int count = inputStream.read(result, offset, length - offset)
			if (count < 0) {
				if (offset == 0) {
					return null
				}
				throw new EOFException("Socket ditutup setelah ${offset}/${length} byte")
			}
			offset += count
		}
		return result
	}

	private static List parseListJson(byte[] bytes, String label) {
		Object parsed = new JsonSlurper().parseText(new String(bytes, 'UTF-8'))
		if (!(parsed instanceof List)) {
			throw new IOException("${label} bukan positional array")
		}
		return (List) parsed
	}

	private static int nextReadTimeout(long deadline) {
		return (int) Math.max(1L, Math.min(deadline - System.currentTimeMillis(), 1000L))
	}

	private static int asInt(Object value) {
		return value instanceof Number ? ((Number) value).intValue() : -1
	}

	private static Object valueAt(List values, int index) {
		return values.size() > index ? values[index] : null
	}

	static String formatPrice(Object value) {
		if (value == null || !(value instanceof Number)) {
			return value?.toString() ?: '-'
		}
		double number = (value as Number).doubleValue()
		if (number == Math.floor(number)) {
			return String.format('%,d', (long) number).replace(',', '.')
		}
		return String.format('%,.2f', number).replace(',', '#').replace('.', ',').replace('#', '.')
	}

	private static String safeJson(Object value) {
		try {
			return JsonOutput.toJson(value)
		} catch (Exception ignored) {
			return value?.toString()
		}
	}

	private void requireConnected() {
		if (socket == null || socket.isClosed() || !socket.isConnected() || inputStream == null || outputStream == null) {
			failAndStop('TCP socket belum terhubung')
		}
	}

	private void requireReadySession() {
		requireConnected()
		requireValue(channelType, 'channelType')
		requireValue(rc4Key, 'rc4Key')
		requireValue(serverSessionId, 'serverSessionId')
	}

	private void requireChannel(String expected) {
		requireReadySession()
		if (channelType != expected) {
			failAndStop("Client ini channel ${channelType}; diperlukan ${expected}")
		}
	}

	private static void requireValue(Object value, String name) {
		if (value == null || value.toString().trim().isEmpty()) {
			failAndStop("${name} wajib diisi")
		}
	}

	private static void failAndStop(String message, Throwable cause = null) {
		String detail = cause == null ? message : "${message} (${cause.class.simpleName})"
		KeywordUtil.markFailedAndStop(detail)
		throw new IllegalStateException(detail, cause)
	}

	// ============================================================
	// MARKET DATA (Stock Quote, Market Info, Stock Summary)
	// ============================================================

	List getStockQuoteSnapshot(String symbolWithBoard, int timeoutMs = 5000) {
		requireChannel('FEED')
		requireValue(symbolWithBoard, 'symbolWithBoard')
		String replyTopic = "jms.topic.${serverSessionId}.StockQuote.${System.currentTimeMillis() * 1000L}"
		String subscriptionId = 'subs-1'
		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'StockQuote', 'quote', true, 0, symbolWithBoard, 0]
		sendMessage([6, 'jms.queue.snapshot', replyTopic, queryPayload])
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			if (message == null) {
				continue
			}
			if (message.size() >= 4 && asInt(message[0]) == 7) {
				List innerData = message[3] instanceof List ? (List) message[3] : null
				if (innerData != null && asInt(innerData[0]) == 12 && innerData.size() > 8) {
					List rows = innerData[8] instanceof List ? (List) innerData[8] : []
					if (!rows.isEmpty()) {
						KeywordUtil.logInfo("Stock Quote Snapshot ${symbolWithBoard}: ${rows[0]}")
						return rows[0] as List
					}
				}
			}
		}
		KeywordUtil.markWarning("Tidak ada Stock Quote snapshot untuk ${symbolWithBoard} dalam ${timeoutMs} ms")
		return null
	}

	void subscribeStockQuote(String symbolWithBoard, String subscriptionId = 'subs-2') {
		requireChannel('FEED')
		requireValue(symbolWithBoard, 'symbolWithBoard')
		sendMessage([4, 'jms.topic.quote', subscriptionId, "stock='${symbolWithBoard}'".toString()])
		KeywordUtil.logInfo("Subscribe live Stock Quote untuk ${symbolWithBoard}")
	}

	void unsubscribeStockQuote(String subscriptionId = 'subs-2') {
		requireChannel('FEED')
		sendMessage([5, 'jms.topic.quote', subscriptionId])
		KeywordUtil.logInfo('Unsubscribe live Stock Quote')
	}

	Map<String, String> subscribeMultipleStockQuotes(List<String> symbols) {
		requireChannel('FEED')
		Map<String, String> subscriptionMap = [:]
		symbols.eachWithIndex { symbol, index ->
			String subId = "subs-quote-${index}"
			subscribeStockQuote(symbol, subId)
			subscriptionMap[symbol] = subId
		}
		KeywordUtil.logInfo("Subscribe live Stock Quote untuk ${symbols.size()} saham: ${symbols}")
		return subscriptionMap
	}

	void unsubscribeMultipleStockQuotes(Map<String, String> subscriptionMap) {
		requireChannel('FEED')
		subscriptionMap.each { symbol, subId ->
			unsubscribeStockQuote(subId)
		}
		KeywordUtil.logInfo("Unsubscribe live Stock Quote untuk ${subscriptionMap.keySet()}")
	}

	Map<String, List<Map>> parseAllLiveQuoteUpdates() {
		Map<String, List<Map>> result = [:]
		receivedMessages.each { msg ->
			if (msg.size() >= 4 && asInt(msg[0]) == 7) {
				List row = msg[3] instanceof List ? (List) msg[3] : null
				if (row != null) {
					Map quote = parseStockQuoteRow(row)
					if (quote != null && quote.stockCode != null) {
						String code = quote.stockCode.toString()
						if (!result.containsKey(code)) {
							result[code] = []
						}
						result[code] << quote
					}
				}
			}
		}
		return result
	}

	static List<String> getStocksWithNoMovement(List<String> subscribedSymbols, Map<String, List<Map>> allUpdates) {
		List<String> noMovement = []
		subscribedSymbols.each { symbol ->
			boolean hasUpdate = allUpdates.keySet().any { code -> symbol.startsWith(code) }
			if (!hasUpdate) {
				noMovement << symbol
			}
		}
		return noMovement
	}

	static Map parseStockQuoteRow(List row) {
		if (row == null || row.size() < 22) {
			return null
		}
		def previous = valueAt(row, 5)
		def last = valueAt(row, 6)
		def displayLast = (last == 0 || last == 0.0) ? previous : last
		return [time: valueAt(row, 2), stockCode: valueAt(row, 3), boardCode: valueAt(row, 4), previous: previous, last: last, displayLast: displayLast, lastLot: valueAt(row, 7), open: valueAt(row, 8), high: valueAt(row, 9), low: valueAt(row, 10), change: valueAt(row, 11), changePct: valueAt(row, 12), limitHigh: valueAt(row, 13), limitLow: valueAt(row, 14), average: valueAt(row, 15), bids: valueAt(row, 16), offers: valueAt(row, 17), bestBid: valueAt(row, 20), bestOffer: valueAt(row, 21), trades: row.size() > 24 ? valueAt(row, 24) : null, bestBidVolume: row.size() > 32 ? valueAt(row, 32) : null, bestOfferVolume: row.size() > 33 ? valueAt(row, 33) : null]
	}

	static List<Map> parseOrderbookLevels(Object rawLevels) {
		List<Map> result = []
		if (!(rawLevels instanceof List)) {
			return result
		}
		(rawLevels as List).each { level ->
			if (level instanceof List && level.size() >= 3) {
				result << [price: level[0], lot: level[1], orderCount: level[2]]
			}
		}
		return result
	}

	Map getOrderbookSnapshot(String symbolWithBoard, int timeoutMs = 5000) {
		List quoteRow = getStockQuoteSnapshot(symbolWithBoard, timeoutMs)
		if (quoteRow == null) {
			return null
		}
		Map quote = parseStockQuoteRow(quoteRow)
		if (quote == null) {
			return null
		}
		return [stockCode: quote.stockCode, boardCode: quote.boardCode, last: quote.displayLast, bids: parseOrderbookLevels(quote.bids), offers: parseOrderbookLevels(quote.offers)]
	}

	Map getMarketInfoSnapshot(int timeoutMs = 5000) {
		requireChannel('FEED')
		String replyTopic = "jms.topic.${serverSessionId}.MarketInfo.${System.currentTimeMillis() * 1000L}"
		String subscriptionId = 'subs-1'
		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'MarketInfo', 'marketinfo', true, 0, '', 0]
		sendMessage([6, 'jms.queue.snapshot', replyTopic, queryPayload])
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			if (message == null) {
				continue
			}
			if (message.size() >= 4 && asInt(message[0]) == 7) {
				List innerData = message[3] instanceof List ? (List) message[3] : null
				if (innerData != null && asInt(innerData[0]) == 12 && innerData.size() > 8) {
					List rows = innerData[8] instanceof List ? (List) innerData[8] : []
					if (!rows.isEmpty()) {
						Map parsed = parseMarketInfoRow(rows[0] as List)
						KeywordUtil.logInfo("Market Info Snapshot: ${parsed}")
						return parsed
					}
				}
			}
		}
		KeywordUtil.markWarning("Tidak ada Market Info snapshot dalam ${timeoutMs} ms")
		return null
	}

	static Map parseMarketInfoRow(List row) {
		if (row == null || row.size() < 11) {
			return null
		}
		return [
				marketName : valueAt(row, 2),
				last       : valueAt(row, 3),
				change     : valueAt(row, 4),
				changePct  : valueAt(row, 5),
				value      : valueAt(row, 6),
				volume     : valueAt(row, 7),
				frequency  : valueAt(row, 8),
				status     : valueAt(row, 9),
				description: valueAt(row, 10),
				gainers    : row.size() > 11 ? valueAt(row, 11) : null,
				losers     : row.size() > 12 ? valueAt(row, 12) : null,
				unchanged  : row.size() > 13 ? valueAt(row, 13) : null,
				
		]
	}

	List<List> getStockSummarySnapshot(List<String> stockCodes, int timeoutMs = 5000) {
		requireChannel('FEED')
		String filter = stockCodes ? stockCodes.join(',') : ''
		String replyTopic = "jms.topic.${serverSessionId}.StockSummary.${System.currentTimeMillis() * 1000L}"
		String subscriptionId = 'subs-1'
		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'StockSummary', 'stocksummary', true, 0, filter, 0]
		sendMessage([6, 'jms.queue.snapshot', replyTopic, queryPayload])
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			if (message == null) {
				continue
			}
			if (message.size() >= 4 && asInt(message[0]) == 7) {
				List innerData = message[3] instanceof List ? (List) message[3] : null
				if (innerData != null && asInt(innerData[0]) == 12) {
					List rows = (innerData.size() > 8 && innerData[8] instanceof List) ? (List) innerData[8] : []
					KeywordUtil.logInfo("Stock Summary Snapshot (${stockCodes}): ${rows.size()} baris diterima")
					return rows
				}
			}
		}
		KeywordUtil.markWarning("Tidak ada Stock Summary snapshot untuk ${stockCodes} dalam ${timeoutMs} ms")
		return []
	}

	static Map parseStockSummaryRow(List row) {
		if (row == null || row.size() < 21) {
			return null
		}
		return [stockCode: valueAt(row, 2), boardCode: valueAt(row, 3), remark: valueAt(row, 4), previous: valueAt(row, 5), high: valueAt(row, 6), low: valueAt(row, 7), close: valueAt(row, 8), change: valueAt(row, 9), tradeVolume: valueAt(row, 10), tradeValue: valueAt(row, 11), tradeFrequency: valueAt(row, 12), index: valueAt(row, 13), foreign: valueAt(row, 14), open: valueAt(row, 15), bestBid: valueAt(row, 16), bestBidVolume: valueAt(row, 17), bestOffer: valueAt(row, 18), bestOfferVolume: valueAt(row, 19), changePct: valueAt(row, 20)]
	}

	// ============================================================
	// TRADING QUERY - Portfolio Stock
	// ============================================================

	List<List> getPortfolioStockSnapshot(String userId, int timeoutMs = 5000) {
		requireChannel('TRADING')
		requireValue(userId, 'userId')
		String replyTopic = "jms.topic.${serverSessionId}.PortfolioStock.${System.currentTimeMillis() * 1000L}"
		String subscriptionId = 'subs-25'
		String filter = "PFO#${userId}#%#%"
		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'PortfolioStock', 'portfolio', true, 0, filter, 0]
		sendMessage([6, 'jms.queue.trading.query', replyTopic, queryPayload])
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			if (message == null) {
				continue
			}
			if (message.size() >= 4 && asInt(message[0]) == 7) {
				List innerData = message[3] instanceof List ? (List) message[3] : null
				if (innerData != null && innerData.size() > 8) {
					List rows = innerData[8] instanceof List ? (List) innerData[8] : []
					KeywordUtil.logInfo("Portfolio Stock RAW response untuk ${userId}: ${innerData}")
					return rows
				}
				KeywordUtil.logInfo("Portfolio Stock message diterima (struktur belum sesuai dugaan): ${message}")
			}
		}
		KeywordUtil.markWarning("Tidak ada Portfolio Stock snapshot untuk ${userId} dalam ${timeoutMs} ms")
		return []
	}

	// ============================================================
	// PORTFOLIO REAL-TIME (via Trading Event Detection)
	// ============================================================

	List<Map> watchPortfolioRealtime(String userId, int listenSeconds) {
		requireChannel('TRADING')
		requireValue(userId, 'userId')
		List<Map> updates = []
		long deadline = System.currentTimeMillis() + (listenSeconds * 1000L)
		long lastRefresh = 0L
		long throttleMs = 1000L
		KeywordUtil.logInfo("Mulai memantau Portfolio real-time selama ${listenSeconds} detik untuk user ${userId}...")
		while (System.currentTimeMillis() < deadline) {
			int timeout = (int) Math.min(deadline - System.currentTimeMillis(), 1000L)
			if (timeout <= 0) {
				break
			}
			List message = receiveMessage(timeout)
			if (message == null) {
				continue
			}
			List innerData = unwrapApplicationMessage(message)
			if (innerData == null || innerData.size() < 5) {
				continue
			}
			int innerType = asInt(innerData[0])
			if (innerType != 14) {
				continue
			}
			String fixMessage = innerData[4]?.toString()
			String fixType = extractFixTag(fixMessage, '35')
			if (!(fixType in ['8', '9', 'C8'])) {
				continue
			}
			long now = System.currentTimeMillis()
			if (now - lastRefresh < throttleMs) {
				KeywordUtil.logInfo("Trading event terdeteksi (FIX 35=${fixType}), tapi masih dalam throttle window, dilewati.")
				continue
			}
			lastRefresh = now
			KeywordUtil.logInfo("Trading event terdeteksi (FIX 35=${fixType}), refresh Portfolio...")
			List<List> refreshedPortfolio = getPortfolioStockSnapshot(userId, 5000)
			updates << [trigger: fixType, portfolio: refreshedPortfolio, timestamp: now]
		}
		if (updates.isEmpty()) {
			KeywordUtil.logInfo("Tidak ada trading event yang memicu refresh Portfolio selama ${listenSeconds} detik.")
		} else {
			KeywordUtil.logInfo("Total ${updates.size()} kali Portfolio ter-refresh akibat trading event.")
		}
		return updates
	}

	// ============================================================
	// RUNNING TRADE (LIVE-ONLY, tidak ada snapshot query)
	// ============================================================

	void subscribeRunningTrade(String subscriptionId = 'subs-runningtrade') {
		requireChannel('FEED')
		sendMessage([4, 'jms.topic.trade.live', subscriptionId])
		KeywordUtil.logInfo("Subscribe Running Trade dengan ID: ${subscriptionId}")
	}

	void unsubscribeRunningTrade(String subscriptionId = 'subs-runningtrade') {
		requireChannel('FEED')
		sendMessage([5, 'jms.topic.trade.live', subscriptionId])
		KeywordUtil.logInfo('Unsubscribe Running Trade')
	}

	List<List> getRawRunningTradeMessages() {
		List<List> result = []
		receivedMessages.each { msg ->
			if (msg.size() >= 2 && msg[1]?.toString() == 'jms.topic.trade.live') {
				result << msg
			}
		}
		return result
	}

	List<Map> parseAllRunningTrades() {
		List<Map> result = []
		receivedMessages.each { msg ->
			if (msg.size() >= 2 && msg[1]?.toString() == 'jms.topic.trade.live') {
				List row = null
				if (msg.size() >= 4 && msg[3] instanceof List) {
					row = (List) msg[3]
				} else if (msg.size() >= 2 && msg[1] instanceof List) {
					row = (List) msg[1]
				}
				if (row != null && row.size() >= 19) {
					result << [time: valueAt(row, 2), stockCode: valueAt(row, 3), boardCode: valueAt(row, 4), price: valueAt(row, 6), lot: valueAt(row, 7), bestBid: valueAt(row, 12), change: valueAt(row, 17), percentage: valueAt(row, 18)]
				}
			}
		}
		return result
	}

	static List<Map> filterRunningTradesByStock(List<Map> allTrades, String stockCode) {
		return allTrades.findAll { trade -> trade.stockCode?.toString() == stockCode }
	}

	// ============================================================
	// PLACE ORDER (BUY/SELL)
	// ============================================================

	private String generateClOrdId(boolean isSplit = false) {
		String paddedSession = serverSessionId.toString().padLeft(11, '0')
		String paddedSeq = orderSequence.toString().padLeft(6, '0')
		orderSequence++
		String prefix = isSplit ? 'RSO' : 'R'
		return "${prefix}${paddedSession}J${paddedSeq}"
	}

	static String buildPlaceOrderFix(String clOrdId, String stockCode, String boardCode, String userId, String investorType, String side, String transactTime, int quantityShares, String orderType, BigDecimal price, String timeInForce, String sid, String status, String accountId, String accountType, String customerId, String priority = '0', String exchangeId = 'JSX') {
		String separator = '\u0001'
		String body = "35=D${separator}" + "11=${clOrdId}${separator}" + "55=${stockCode}${separator}" + "65=${boardCode}${separator}" + "18= ${separator}" + "109=${userId}${separator}" + "1=${investorType}${separator}" + "21=1${separator}" + "54=${side}${separator}" + "60=${transactTime}${separator}" + "38=${quantityShares}${separator}" + "40=${orderType}${separator}" + "44=${price}${separator}" + "59=${timeInForce}${separator}" + "376=${sid}${separator}" + "39=${status}${separator}" + "10006=${accountId}${separator}" + "10061=${accountType}${separator}" + "10095=${customerId}${separator}" + "10007=${exchangeId}${separator}" + "10376=${sid}${separator}" + "10168=${priority}${separator}"
		return "8=FIX.4.2${separator}9=${body.length()}${separator}${body}10=0"
	}

	Map placeOrder(String stockCode, String boardCode, String side, int lot, BigDecimal price, String userId, String sid = 'SID-DEMO', String accountId = 'ACC-DEMO', String customerId = 'CUST-DEMO', String accountType = 'R', String investorType = 'I', int timeoutMs = 5000) {
		requireChannel('TRADING')
		String clOrdId = generateClOrdId()
		int quantityShares = lot * 100
		String transactTime = new Date().format('HH:mm:ss')
		String fixMessage = buildPlaceOrderFix(clOrdId, stockCode, boardCode, userId, investorType, side, transactTime, quantityShares, '1', price, '0', sid, '0', accountId, accountType, customerId)
		String orderTopic = lastTradingLoginTopic ?: "jms.topic.trading.${serverSessionId}"
		List orderPayload = [5, serverSessionId, userId, 'D', fixMessage]
		sendMessage([6, 'jms.queue.trading', orderTopic, orderPayload])
		KeywordUtil.logInfo("Place Order terkirim: ClOrdID=${clOrdId}, ${stockCode}${boardCode}, Side=${side == '1' ? 'BUY' : 'SELL'}, Lot=${lot} (${quantityShares} lembar), Price=${price}")
		return waitForPlaceOrderResponse(clOrdId, timeoutMs)
	}

	private Map waitForPlaceOrderResponse(String clOrdId, int timeoutMs) {
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			Map result = parsePlaceOrderMessage(message)
			if (result != null) {
				if (result.success) {
					KeywordUtil.logInfo("✅ Order diterima (non-rejection). ClOrdID=${clOrdId}, Tag35=${result.tag35}, Tag39=${result.tag39}")
				} else {
					KeywordUtil.logInfo("❌ Order DITOLAK. ClOrdID=${clOrdId}, Alasan (tag58)=${result.tag58 ?: '-'}")
				}
				return result
			}
		}
		KeywordUtil.logInfo("⚠️ Timeout menunggu response order setelah ${timeoutMs}ms. PERINGATAN: order mungkin TETAP TERKIRIM ke server meski response tidak diterima. Cek manual status order via getBasicOrderList() sebelum coba kirim ulang.")
		return [success: false, timeout: true, clOrdId: clOrdId, message: 'Timeout - status order tidak pasti']
	}

	private Map parsePlaceOrderMessage(List message) {
		List innerData = unwrapApplicationMessage(message)
		if (!innerData || innerData.size() < 5) {
			return null
		}
		int msgTypeIndicator = asInt(innerData[0])
		if (msgTypeIndicator != 14) {
			return null
		}
		String typeMarker = innerData[3]?.toString()
		if (typeMarker == 'C0') {
			KeywordUtil.logInfo("ℹ️ Mengabaikan pesan status global (C0), bukan response order: ${innerData}")
			return null
		}
		String fixResponse = innerData[4]?.toString()
		if (!fixResponse) {
			return null
		}
		String tag35 = extractFixTag(fixResponse, '35')
		String tag39 = extractFixTag(fixResponse, '39')
		String tag150 = extractFixTag(fixResponse, '150')
		String tag58 = extractFixTag(fixResponse, '58')
		boolean isRejected = (tag35 == '9') || (tag39 == '8') || (tag150 == '8') || (tag58 != null && !tag58.trim().isEmpty())
		return [success: !isRejected, tag35: tag35, tag39: tag39, tag150: tag150, tag58: tag58, rawFix: fixResponse]
	}

	List<Map> getBasicOrderList(String userId) {
		requireChannel('TRADING')
		String replyTopic = "jms.topic.${serverSessionId}.StockOrderList.Basic.${System.currentTimeMillis()}${(Math.random() * 999).toInteger()}"
		String subscriptionId = "subs-${UUID.randomUUID()}"
		List queryPayload = [11, serverSessionId, 'StockOrderList.Basic', 'order', true, 0, "ORD#${userId}#%#%", 0]
		sendMessage([4, replyTopic, subscriptionId])
		sendMessage([6, 'jms.queue.trading.query', replyTopic, queryPayload])
		long deadline = System.currentTimeMillis() + 10000
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			List innerData = unwrapApplicationMessage(message)
			if (innerData != null && asInt(innerData[0]) == 12) {
				List rows = (innerData.size() > 8 && innerData[8] instanceof List) ? (List) innerData[8] : []
				KeywordUtil.logInfo("Basic Order List diterima: ${rows.size()} order")
				return rows
			}
		}
		KeywordUtil.logInfo('⚠️ Timeout menunggu Basic Order List')
		return []
	}

	// ============================================================
	// CUSTOMER & ACCOUNT QUERY
	// ============================================================

	List<List> getCustomerInfo(String userId, int timeoutMs = 10000) {
		requireChannel('TRADING')
		requireValue(userId, 'userId')
		String replyTopic = "jms.topic.${serverSessionId}.CustomerInfo.${System.currentTimeMillis()}${(Math.random() * 999).toInteger()}"
		String subscriptionId = "subs-${UUID.randomUUID()}"
		String filter = "CI#${userId}#%#%"
		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'CustomerInfo', 'account', true, 0, filter, 0]
		sendMessage([6, 'jms.queue.trading.query', replyTopic, queryPayload])
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			List innerData = unwrapApplicationMessage(message)
			if (innerData != null && asInt(innerData[0]) == 12) {
				List rows = (innerData.size() > 8 && innerData[8] instanceof List) ? (List) innerData[8] : []
				KeywordUtil.logInfo("=== CUSTOMER INFO RAW untuk ${userId} ===")
				rows.eachWithIndex { row, idx -> KeywordUtil.logInfo("Row ${idx}: ${row}") }
				return rows
			}
		}
		KeywordUtil.logInfo('⚠️ Timeout menunggu Customer Info')
		return []
	}

	List<List> getAccountList(String userId, int timeoutMs = 10000) {
		requireChannel('TRADING')
		requireValue(userId, 'userId')
		String replyTopic = "jms.topic.${serverSessionId}.Account.${System.currentTimeMillis()}${(Math.random() * 999).toInteger()}"
		String subscriptionId = "subs-${UUID.randomUUID()}"
		String filter = "ACC#${userId}#%#%"
		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'Account', 'account', true, 0, filter, 0]
		sendMessage([6, 'jms.queue.trading.query', replyTopic, queryPayload])
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			List innerData = unwrapApplicationMessage(message)
			if (innerData != null && asInt(innerData[0]) == 12) {
				List rows = (innerData.size() > 8 && innerData[8] instanceof List) ? (List) innerData[8] : []
				KeywordUtil.logInfo("=== ACCOUNT LIST RAW untuk ${userId} ===")
				rows.eachWithIndex { row, idx -> KeywordUtil.logInfo("Row ${idx}: ${row}") }
				return rows
			}
		}
		KeywordUtil.logInfo('⚠️ Timeout menunggu Account List')
		return []
	}

	// ============================================================
	// STOCK MASTER (Daftar Semua Saham)
	// ============================================================

	List<List> getStockMaster(int timeoutMs = 15000) {
		requireChannel('FEED')
		String replyTopic = "jms.topic.${serverSessionId}.Stock.${System.currentTimeMillis() * 1000L}"
		String subscriptionId = 'subs-stockmaster'
		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'Stock', 'stock', true, 0, '', 0]
		sendMessage([6, 'jms.queue.snapshot', replyTopic, queryPayload])
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			if (message == null) {
				continue
			}
			if (message.size() >= 4 && asInt(message[0]) == 7) {
				List innerData = message[3] instanceof List ? (List) message[3] : null
				if (innerData != null && asInt(innerData[0]) == 12 && innerData.size() > 8) {
					List rows = innerData[8] instanceof List ? (List) innerData[8] : []
					KeywordUtil.logInfo("Stock Master diterima: ${rows.size()} saham")
					return rows
				}
			}
		}
		KeywordUtil.markWarning("Tidak ada Stock Master response dalam ${timeoutMs} ms")
		return []
	}

	static Map parseStockMasterRow(List row) {
		if (row == null || row.size() < 13) {
			return null
		}
		return [code: valueAt(row, 2), name: valueAt(row, 3), status: valueAt(row, 4), stockType: valueAt(row, 5), subSectorCode: valueAt(row, 6), ipoPrice: valueAt(row, 7), basePrice: valueAt(row, 8), listedShares: valueAt(row, 9), tradableShares: valueAt(row, 10), lotSize: valueAt(row, 11), corpActionCode: valueAt(row, 12), marginable: row.size() > 13 ? valueAt(row, 13) : null, main: row.size() > 17 ? valueAt(row, 17) : null]
	}

	// ============================================================
	// CORPORATE ACTION
	// Sesuai dokumentasi bagian 8.1 & 8.2.
	// Destination: jms.queue.query | Module: CorporateActionView |
	// Logical queue: CorporateAction | Filter: CA#%#<UPPERCASE_STOCK>%
	// ============================================================

	List<List> getCorporateAction(String stockCode, int timeoutMs = 10000) {
		requireChannel('FEED')
		requireValue(stockCode, 'stockCode')
		String upperStock = stockCode.toUpperCase()
		String replyTopic = "jms.topic.${serverSessionId}.CorporateAction.${System.currentTimeMillis()}${(Math.random() * 999).toInteger()}"
		String subscriptionId = "subs-${UUID.randomUUID()}"
		String filter = "CA#%#${upperStock}%"
		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'CorporateActionView', 'CorporateAction', true, 0, filter, 0]
		sendMessage([6, 'jms.queue.query', replyTopic, queryPayload])
		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			if (message == null) {
				continue
			}
			if (message.size() >= 4 && asInt(message[0]) == 7) {
				List innerData = message[3] instanceof List ? (List) message[3] : null
				if (innerData != null && asInt(innerData[0]) == 12) {
					List rows = (innerData.size() > 8 && innerData[8] instanceof List) ? (List) innerData[8] : []
					KeywordUtil.logInfo("Corporate Action untuk ${upperStock}: ${rows.size()} baris diterima")
					return rows
				}
			}
		}
		KeywordUtil.markWarning("Tidak ada Corporate Action response untuk ${upperStock} dalam ${timeoutMs} ms")
		return []
	}

	static String corporateActionTypeName(String typeCode) {
		Map<String, String> typeNames = ['A': 'IPO', 'B': 'RUPS', 'C': 'Right Issue', 'D': 'Warrant', 'E': 'Stock Split', 'F': 'Reverse Stock', 'G': 'Cash Dividend', 'H': 'Stock Dividend', 'I': 'Bonus', 'J': 'Merger', 'K': 'Tender Offer']
		return typeNames[typeCode] ?: "Tidak diketahui (${typeCode})"
	}

	static Map parseCorporateActionRow(List row) {
		if (row == null || row.size() < 11) {
			return null
		}
		String typeCode = valueAt(row, 0)?.toString()
		return [typeCode: typeCode, typeName: corporateActionTypeName(typeCode), stockCode: valueAt(row, 1), stockName: valueAt(row, 2), stockDate: valueAt(row, 3), stockTime: valueAt(row, 4), amount: valueAt(row, 5), place: valueAt(row, 6), agenda: valueAt(row, 7), ratioOld: valueAt(row, 8), ratioNew: valueAt(row, 9), price: valueAt(row, 10), cumDateRgNg: row.size() > 11 ? valueAt(row, 11) : null, exDateRgNg: row.size() > 13 ? valueAt(row, 13) : null, recordDate: row.size() > 15 ? valueAt(row, 15) : null, paymentDate: row.size() > 16 ? valueAt(row, 16) : null]
	}

	// ============================================================
	// MARKET INDICES (daftar semua indeks pasar)
	// ============================================================

	List<List> getAllMarketIndices(int timeoutMs = 10000) {
		requireChannel('FEED')

		String replyTopic = 'jms.topic.' + serverSessionId + '.MarketInfoAll.' + System.currentTimeMillis()
		String subscriptionId = 'subs-indices'

		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'MarketInfo', 'marketinfo', true, 0, '', 0]
		sendMessage([6, 'jms.queue.snapshot', replyTopic, queryPayload])

		long deadline = System.currentTimeMillis() + timeoutMs
		while (System.currentTimeMillis() < deadline) {
			List message = receiveMessage(nextReadTimeout(deadline))
			if (message == null) {
				continue
			}
			if (message.size() >= 4 && asInt(message[0]) == 7) {
				List innerData = message[3] instanceof List ? (List) message[3] : null
				if (innerData != null && asInt(innerData[0]) == 12) {
					List rows = (innerData.size() > 8 && innerData[8] instanceof List) ? (List) innerData[8] : []
					KeywordUtil.logInfo('Market Indices diterima: ' + rows.size() + ' indeks')
					return rows
				}
			}
		}

		KeywordUtil.markWarning('Tidak ada Market Indices response dalam ' + timeoutMs + ' ms')
		return []
	}

	/**
	 * DEBUG - uji query MarketInfo dengan filter kode spesifik (misal 'ABX'),
	 * untuk cek apakah modul ini bisa di-query per-indeks seperti StockQuote.
	 */
	void debugMarketInfoWithFilter(String indexCode) {
		requireChannel('FEED')

		String replyTopic = 'jms.topic.' + serverSessionId + '.MarketInfoFilter.' + System.currentTimeMillis()
		String subscriptionId = 'subs-marketinfo-filter'

		sendMessage([4, replyTopic, subscriptionId])
		List queryPayload = [11, serverSessionId, 'MarketInfo', 'marketinfo', true, 0, indexCode, 0]
		sendMessage([6, 'jms.queue.snapshot', replyTopic, queryPayload])

		for (int i = 0; i < 5; i++) {
			List msg = receiveMessage(3000)
			if (msg != null) {
				KeywordUtil.logInfo('Response untuk filter "' + indexCode + '": ' + msg)
			}
		}
	}

}