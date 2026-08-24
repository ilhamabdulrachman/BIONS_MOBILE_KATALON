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

	// Env.socketPublicKey. Do not replace this with API_PUBLIC_KEY or
	// API_PUBLIC_KEY_REGIS; those keys are for REST endpoints.
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

		List connectPayload = [
				0,
				userId.toUpperCase(),
				passwordValue,
				connectType,
				sessionKeyUuid,
				0,
				2,
		]

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

	/**
	 * Kept only so an older test case fails clearly instead of consuming bytes.
	 * Reading the stream here would steal the framed response from the parser.
	 */
	void debugRawPeek(int timeoutMs = 5000) {
		KeywordUtil.markWarning(
				"debugRawPeek(${timeoutMs}) dinonaktifkan karena raw read merusak frame; " +
						'gunakan receiveConnectResponse() atau receiveMessage()',
		)
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

	void sendFeedLogin(
			String userId,
			String plainPassword,
			String clientIp,
			String appVersion,
			String platformInfo
	) {
		requireChannel('FEED')
		requireValue(clientIp, 'clientIp')
		requireValue(appVersion, 'appVersion')
		requireValue(platformInfo, 'platformInfo')

		lastFeedLoginTopic = "jms.topic.admin.${serverSessionId}.${System.currentTimeMillis() * 1000L}"
		String subscriptionId = "subs-${UUID.randomUUID()}"

		sendMessage([4, lastFeedLoginTopic, subscriptionId])
		List loginPayload = [
				1,
				serverSessionId,
				userId,
				plainPassword,
				clientIp,
				appVersion,
				platformInfo,
		]
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
				KeywordUtil.logInfo(
						"Feed Login berhasil; gateway=${result.gatewayId}, autoRenew=${result.autoRenew}",
				)
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

	Map loginFeed(
			String host,
			int port,
			String userId,
			String plainPassword,
			String clientIp,
			String appVersion = '4.17.5',
			String platformInfo = 'Android',
			int timeoutMs = 15000
	) {
		connectSocket(host, port)
		sendConnect(userId, plainPassword, 'FEED', UUID.randomUUID().toString())
		receiveConnectResponse(timeoutMs)
		sendFeedLogin(userId, plainPassword, clientIp, appVersion, platformInfo)
		return waitForFeedLoginResponse(timeoutMs)
	}

	static String buildTradingLoginFix(
			String userId,
			String plainPin,
			String clientIp,
			String platformInfo
	) {
		String separator = '\u0001'
		String body = "35=AA${separator}" +
				"10001=${userId}${separator}" +
				"10002=${md5Hex(plainPin)}${separator}" +
				"999930=${clientIp}${separator}" +
				"58=${platformInfo}${separator}" +
				"108=45${separator}"
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

	Map loginTrading(
			String host,
			int port,
			String userId,
			String plainPassword,
			String plainPin,
			String clientIp,
			String platformInfo = 'Android',
			int timeoutMs = 15000
	) {
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
			return [
					success  : true,
					loginId  : valueAt(innerData, 1),
					gatewayId: valueAt(innerData, 4),
					message  : valueAt(innerData, 6),
					autoRenew: valueAt(innerData, 7)?.toString()?.equalsIgnoreCase('Y') ?: false,
			]
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
		byte[] prefix = [
				(byte) ((length >>> 24) & 0xFF),
				(byte) ((length >>> 16) & 0xFF),
				(byte) ((length >>> 8) & 0xFF),
				(byte) (length & 0xFF),
		] as byte[]
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

			int length = ((prefix[0] & 0xFF) << 24) |
					((prefix[1] & 0xFF) << 16) |
					((prefix[2] & 0xFF) << 8) |
					(prefix[3] & 0xFF)
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

	/**
	 * Format angka harga saham supaya rapi seperti tampilan UI aplikasi:
	 * - Hilangkan desimal ".0" kalau memang bilangan bulat
	 * - Tambahkan pemisah ribuan (titik/koma sesuai locale Indonesia)
	 * Contoh: 4880.0 -> "4.880" | 4880.5 -> "4.880,5"
	 */
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
	// MARKET DATA (Stock Quote, Market Info)
	// Sesuai dokumentasi bagian 3. Stock and Market Data.
	// WAJIB dipanggil SETELAH Feed Login berhasil (channelType == FEED).
	// ============================================================

	/**
	 * Meminta Stock Quote snapshot (one-shot, bukan live) untuk 1 simbol saham.
	 * Contoh: getStockQuoteSnapshot("BBNIRG") -> simbol saham + kode papan (RG/TN/NG dst).
	 *
	 * @param symbolWithBoard kode saham + papan digabung, misal "BBNIRG" (BBNI + papan RG)
	 * @return List berisi row Stock Quote sesuai skema 3.5, atau null kalau tidak ada respons
	 */
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
			// Format: [7, topic, subsId, [12, session, module, queue, singleResult, seq, page, count, [[row]]]]
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

	/**
	 * Subscribe live Stock Quote untuk 1 simbol saham. Setiap ada perubahan harga,
	 * server akan kirim update baru ke topic yang sama ("jms.topic.quote").
	 * Panggil unsubscribeStockQuote() setelah selesai untuk berhenti menerima update.
	 *
	 * @param symbolWithBoard kode saham + papan, misal "BBNIRG"
	 */
	void subscribeStockQuote(String symbolWithBoard, String subscriptionId = 'subs-2') {
		requireChannel('FEED')
		requireValue(symbolWithBoard, 'symbolWithBoard')

		sendMessage([4, 'jms.topic.quote', subscriptionId, "stock='${symbolWithBoard}'".toString()])
		KeywordUtil.logInfo("Subscribe live Stock Quote untuk ${symbolWithBoard}")
	}

	/**
	 * Berhenti menerima live update Stock Quote yang sebelumnya di-subscribe.
	 */
	void unsubscribeStockQuote(String subscriptionId = 'subs-2') {
		requireChannel('FEED')
		sendMessage([5, 'jms.topic.quote', subscriptionId])
		KeywordUtil.logInfo('Unsubscribe live Stock Quote')
	}

	/**
	 * Subscribe live Stock Quote untuk BEBERAPA saham sekaligus.
	 * Tiap simbol otomatis diberi subscriptionId unik (subs-quote-0, subs-quote-1, dst)
	 * supaya tidak saling menimpa subscription satu sama lain.
	 *
	 * @param symbols List kode saham + papan, misal ["BBNIRG", "TLKMRG"]
	 * @return Map<String symbol, String subscriptionId> - simpan ini untuk unsubscribe nanti
	 */
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

	/**
	 * Berhenti menerima live update untuk BEBERAPA saham sekaligus.
	 * Gunakan Map hasil dari subscribeMultipleStockQuotes() sebagai input.
	 */
	void unsubscribeMultipleStockQuotes(Map<String, String> subscriptionMap) {
		requireChannel('FEED')
		subscriptionMap.each { symbol, subId ->
			unsubscribeStockQuote(subId)
		}
		KeywordUtil.logInfo("Unsubscribe live Stock Quote untuk ${subscriptionMap.keySet()}")
	}

	/**
	 * Helper untuk mem-parsing SEMUA live update Stock Quote dari receivedMessages,
	 * mengelompokkan hasilnya berdasarkan kode saham. Berguna setelah listen()
	 * dipanggil untuk multi-simbol subscription.
	 *
	 * @return Map<String stockCode, List<Map> quotes> - bisa lebih dari 1 update per saham
	 */
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

	/**
	 * Mengecek dari List simbol yang di-subscribe, mana saja yang TIDAK ada
	 * update sama sekali selama listen() - artinya harga saham itu TIDAK
	 * bergerak (statis/flat) sepanjang durasi monitoring.
	 *
	 * @param subscribedSymbols List simbol asli yang di-subscribe, misal ["AMMNRG", "AALIRG"]
	 * @param allUpdates Hasil dari parseAllLiveQuoteUpdates()
	 * @return List simbol yang TIDAK bergerak (tidak ada di allUpdates)
	 */
	static List<String> getStocksWithNoMovement(List<String> subscribedSymbols, Map<String, List<Map>> allUpdates) {
		List<String> noMovement = []

		subscribedSymbols.each { symbol ->
			// allUpdates key cuma kode saham polos (misal "AMMN"), sedangkan
			// subscribedSymbols termasuk kode papan (misal "AMMNRG").
			// Cocokkan dengan prefix, supaya tidak bergantung panjang kode papan.
			boolean hasUpdate = allUpdates.keySet().any { code -> symbol.startsWith(code) }
			if (!hasUpdate) {
				noMovement << symbol
			}
		}

		return noMovement
	}

	/**
	 * Mem-parsing 1 baris Stock Quote (dari snapshot atau live update) menjadi Map
	 * yang mudah dibaca, sesuai skema index di dokumentasi bagian 3.5.
	 */
	static Map parseStockQuoteRow(List row) {
		if (row == null || row.size() < 22) {
			return null
		}
		def previous = valueAt(row, 5)
		def last = valueAt(row, 6)
		// Kalau 'last' masih 0 (belum ada transaksi hari ini / market tutup),
		// tampilkan 'previous' sebagai harga terakhir, sesuai perilaku UI aplikasi.
		def displayLast = (last == 0 || last == 0.0) ? previous : last

		return [
				time         : valueAt(row, 2),
				stockCode    : valueAt(row, 3),
				boardCode    : valueAt(row, 4),
				previous     : previous,
				last         : last,
				displayLast  : displayLast,
				lastLot      : valueAt(row, 7),
				open         : valueAt(row, 8),
				high         : valueAt(row, 9),
				low          : valueAt(row, 10),
				change       : valueAt(row, 11),
				changePct    : valueAt(row, 12),
				limitHigh    : valueAt(row, 13),
				limitLow     : valueAt(row, 14),
				average      : valueAt(row, 15),
				bids         : valueAt(row, 16),
				offers       : valueAt(row, 17),
				bestBid      : valueAt(row, 20),
				bestOffer    : valueAt(row, 21),
				trades       : row.size() > 24 ? valueAt(row, 24) : null,
				bestBidVolume: row.size() > 32 ? valueAt(row, 32) : null,
				bestOfferVolume: row.size() > 33 ? valueAt(row, 33) : null,
		]
	}

	/**
	 * Mem-parsing array bid/offer mentah (dari field 'bids' atau 'offers'
	 * hasil parseStockQuoteRow) menjadi List<Map> yang mudah dibaca.
	 * Sesuai skema: setiap item = [price, lot, orderCount].
	 *
	 * @param rawLevels List mentah dari quote.bids atau quote.offers
	 * @return List<Map> dengan key: price, lot, orderCount
	 */
	static List<Map> parseOrderbookLevels(Object rawLevels) {
		List<Map> result = []
		if (!(rawLevels instanceof List)) {
			return result
		}

		(rawLevels as List).each { level ->
			if (level instanceof List && level.size() >= 3) {
				result << [
						price     : level[0],
						lot       : level[1],
						orderCount: level[2],
				]
			}
		}

		return result
	}

	/**
	 * Mengambil Orderbook (multi-level Bid & Offer) untuk 1 saham,
	 * meniru tampilan UI (Queue | Lot | Bid -- Offer | Lot | Queue).
	 * WAJIB dipanggil setelah loginFeed() berhasil (channelType FEED).
	 *
	 * @param symbolWithBoard kode saham + papan, misal "BBNIRG"
	 * @return Map berisi: stockCode, bids (List<Map>), offers (List<Map>)
	 *         atau null kalau tidak ada respons
	 */
	Map getOrderbookSnapshot(String symbolWithBoard, int timeoutMs = 5000) {
		List quoteRow = getStockQuoteSnapshot(symbolWithBoard, timeoutMs)
		if (quoteRow == null) {
			return null
		}

		Map quote = parseStockQuoteRow(quoteRow)
		if (quote == null) {
			return null
		}

		return [
				stockCode: quote.stockCode,
				boardCode: quote.boardCode,
				last     : quote.displayLast,
				bids     : parseOrderbookLevels(quote.bids),
				offers   : parseOrderbookLevels(quote.offers),
		]
	}

	/**
	 * Meminta Market Info snapshot (indeks pasar keseluruhan, bukan per saham).
	 *
	 * @return Map berisi data market info sesuai skema 3.6, atau null kalau tidak ada respons
	 */
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

	/**
	 * Mem-parsing 1 baris Market Info menjadi Map, sesuai skema index bagian 3.6.
	 */
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
		]
	}

	/**
	 * Meminta Stock Summary snapshot untuk BEBERAPA saham sekaligus (multi-simbol).
	 * Beda dari getStockQuoteSnapshot yang cuma 1 simbol, method ini bisa banyak
	 * sekaligus dalam 1 request, dipisah koma sesuai spesifikasi dokumentasi.
	 *
	 * @param stockCodes List kode saham TANPA kode papan, misal ["BBNI", "TLKM"]
	 * @return List of List (tiap baris = 1 saham), sesuai skema Stock Summary
	 */
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
				if (innerData != null && asInt(innerData[0]) == 12 && innerData.size() > 8) {
					List rows = innerData[8] instanceof List ? (List) innerData[8] : []
					KeywordUtil.logInfo("Stock Summary Snapshot (${stockCodes}): ${rows.size()} baris diterima")
					return rows
				}
			}
		}

		KeywordUtil.markWarning("Tidak ada Stock Summary snapshot untuk ${stockCodes} dalam ${timeoutMs} ms")
		return []
	}

	/**
	 * Mem-parsing 1 baris Stock Summary menjadi Map, sesuai skema index bagian 3.5.
	 */
	static Map parseStockSummaryRow(List row) {
		if (row == null || row.size() < 21) {
			return null
		}
		return [
				stockCode      : valueAt(row, 2),
				boardCode      : valueAt(row, 3),
				remark         : valueAt(row, 4),
				previous       : valueAt(row, 5),
				high           : valueAt(row, 6),
				low            : valueAt(row, 7),
				close          : valueAt(row, 8),
				change         : valueAt(row, 9),
				tradeVolume    : valueAt(row, 10),
				tradeValue     : valueAt(row, 11),
				tradeFrequency : valueAt(row, 12),
				index          : valueAt(row, 13),
				foreign        : valueAt(row, 14),
				open           : valueAt(row, 15),
				bestBid        : valueAt(row, 16),
				bestBidVolume  : valueAt(row, 17),
				bestOffer      : valueAt(row, 18),
				bestOfferVolume: valueAt(row, 19),
				changePct      : valueAt(row, 20),
		]
	}

	// ============================================================
	// TRADING QUERY - Portfolio Stock
	// Sesuai dokumentasi bagian 9.2. CATATAN: dokumen ini TIDAK
	// menyediakan skema kolom response (beda dari StockQuote/Summary
	// di bagian 3 yang punya "Row Schemas" eksplisit). Method ini
	// cuma kirim request dan kembalikan RAW rows apa adanya - JANGAN
	// asumsikan urutan kolom tanpa verifikasi manual ke data asli
	// (cocokkan ke tampilan Portfolio di aplikasi, seperti yang
	// sebelumnya kita lakukan untuk StockQuote).
	// ============================================================

	/**
	 * Meminta Portfolio Stock snapshot untuk satu user, via Trading channel.
	 * WAJIB dipanggil setelah Trading Login berhasil (channelType == TRADING).
	 *
	 * @param userId User ID pemilik portfolio, sesuai format filter PFO#<USER_ID>#%#%
	 * @return List of List (RAW rows, kolom belum diberi nama - lihat catatan di atas)
	 */
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
			// Format envelope diasumsikan sama seperti Feed snapshot: [7, topic, subsId, [12, ..., [[row],[row]]]]
			// TAPI ini BELUM diverifikasi untuk channel Trading - cek log raw dulu setelah dijalankan.
			if (message.size() >= 4 && asInt(message[0]) == 7) {
				List innerData = message[3] instanceof List ? (List) message[3] : null
				if (innerData != null && innerData.size() > 8) {
					List rows = innerData[8] instanceof List ? (List) innerData[8] : []
					KeywordUtil.logInfo("Portfolio Stock RAW response untuk ${userId}: ${innerData}")
					return rows
				}
				// Fallback: kalau struktur envelope beda dari dugaan, tetap log semuanya
				KeywordUtil.logInfo("Portfolio Stock message diterima (struktur belum sesuai dugaan): ${message}")
			}
		}

		KeywordUtil.markWarning("Tidak ada Portfolio Stock snapshot untuk ${userId} dalam ${timeoutMs} ms")
		return []
	}

	// ============================================================
	// PORTFOLIO REAL-TIME (via Trading Event Detection)
	// Sesuai dokumentasi bagian 12: Portfolio TIDAK punya subscribe
	// langsung. Update terjadi saat server kirim inner type 14 dengan
	// FIX 35=8 (Execution Report), 35=9 (Order Cancel Reject), atau
	// 35=C8 - itu sinyal untuk refresh ulang Portfolio/Order/Trade List.
	// ============================================================

	/**
	 * Mendengarkan channel Trading selama N detik, mendeteksi setiap
	 * trading event (inner type 14, FIX 35=8/9/C8) yang mengindikasikan
	 * Portfolio perlu di-refresh. Setiap kali event terdeteksi, otomatis
	 * panggil ulang getPortfolioStockSnapshot() dan simpan hasilnya.
	 *
	 * WAJIB dipanggil setelah loginTrading() berhasil (channelType TRADING).
	 *
	 * @param userId User ID untuk query ulang Portfolio
	 * @param listenSeconds Total durasi mendengarkan event (detik)
	 * @return List of Map, tiap Map = { trigger: FIX type, portfolio: List<List> rows }
	 */
	List<Map> watchPortfolioRealtime(String userId, int listenSeconds) {
		requireChannel('TRADING')
		requireValue(userId, 'userId')

		List<Map> updates = []
		long deadline = System.currentTimeMillis() + (listenSeconds * 1000L)
		long lastRefresh = 0L
		long throttleMs = 1000L  // sesuai dokumentasi: "trailing throttle satu detik"

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
	// Sesuai dokumentasi bagian 4.3 & 3.6: subscribe via topic
	// "jms.topic.trade.live". Skema field row:
	//   [0-1 ignored, 2 time, 3 stockCode, 4 boardCode, 5 ignored,
	//    6 price, 7 lot, 8-11 ignored, 12 bestBid,
	//    13-16 ignored, 17 change, 18 percentage]
	// ============================================================

	/**
	 * Subscribe ke feed Running Trade (transaksi yang sedang terjadi,
	 * real-time, SEMUA saham - tidak ada filter simbol/board opsional
	 * di source code saat ini). WAJIB dipanggil setelah loginFeed() berhasil.
	 *
	 * @param subscriptionId ID unik untuk subscription ini
	 */
	void subscribeRunningTrade(String subscriptionId = 'subs-runningtrade') {
		requireChannel('FEED')
		sendMessage([4, 'jms.topic.trade.live', subscriptionId])
		KeywordUtil.logInfo("Subscribe Running Trade dengan ID: ${subscriptionId}")
	}

	/**
	 * Berhenti menerima update Running Trade.
	 */
	void unsubscribeRunningTrade(String subscriptionId = 'subs-runningtrade') {
		requireChannel('FEED')
		sendMessage([5, 'jms.topic.trade.live', subscriptionId])
		KeywordUtil.logInfo('Unsubscribe Running Trade')
	}

	/**
	 * Mengambil SEMUA raw message Running Trade yang sudah diterima
	 * dari receivedMessages (setelah listen() dipanggil).
	 *
	 * @return List of raw envelope arrays yang berasal dari topic Running Trade
	 */
	List<List> getRawRunningTradeMessages() {
		List<List> result = []
		receivedMessages.each { msg ->
			if (msg.size() >= 2 && msg[1]?.toString() == 'jms.topic.trade.live') {
				result << msg
			}
		}
		return result
	}

	/**
	 * Mem-parsing SEMUA update Running Trade yang sudah diterima menjadi
	 * List of Map yang mudah dibaca (time, stockCode, price, lot, dll),
	 * sesuai skema resmi dokumentasi bagian 3.6.
	 *
	 * @return List<Map> - satu Map per transaksi yang terjadi
	 */
	List<Map> parseAllRunningTrades() {
		List<Map> result = []

		receivedMessages.each { msg ->
			if (msg.size() >= 2 && msg[1]?.toString() == 'jms.topic.trade.live') {
				// Envelope biasanya: [7, topic, subsId, [row]] - cek posisi row
				List row = null
				if (msg.size() >= 4 && msg[3] instanceof List) {
					row = (List) msg[3]
				} else if (msg.size() >= 2 && msg[1] instanceof List) {
					row = (List) msg[1]
				}

				if (row != null && row.size() >= 19) {
					result << [
							time      : valueAt(row, 2),
							stockCode : valueAt(row, 3),
							boardCode : valueAt(row, 4),
							price     : valueAt(row, 6),
							lot       : valueAt(row, 7),
							bestBid   : valueAt(row, 12),
							change    : valueAt(row, 17),
							percentage: valueAt(row, 18),
					]
				}
			}
		}

		return result
	}

	/**
	 * Filter hasil parseAllRunningTrades() untuk 1 kode saham tertentu.
	 * Berguna karena subscribe Running Trade menerima SEMUA saham sekaligus
	 * (tidak ada filter simbol di level subscribe).
	 *
	 * @param allTrades Hasil dari parseAllRunningTrades()
	 * @param stockCode Kode saham polos, misal "BBCA" (tanpa kode papan)
	 */
	static List<Map> filterRunningTradesByStock(List<Map> allTrades, String stockCode) {
		return allTrades.findAll { trade -> trade.stockCode?.toString() == stockCode }
	}
}