package com.utilities

import com.kms.katalon.core.util.KeywordUtil

class TcpClient {
	Socket socket
	BufferedReader reader
	PrintWriter writer
	List<String> receivedMessages = []

	def connect(String host, int port, int timeoutMs = 5000) {
		try {
			socket = new Socket()
			socket.connect(new InetSocketAddress(host, port), timeoutMs)
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
			writer = new PrintWriter(socket.getOutputStream(), true)

			KeywordUtil.logInfo("✅ Connected to TCP socket ${host}:${port}")
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Connection failed: " + e.message)
		}
	}

	def sendMessage(String message) {
		if (writer == null) {
			KeywordUtil.markFailed("❌ Tidak bisa mengirim pesan: koneksi belum terbentuk (writer null)")
			return
		}
		try {
			writer.println(message)
			KeywordUtil.logInfo("📤 Sent: " + message)
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Send failed: " + e.message)
		}
	}


	def listen(int seconds) {
		if (reader == null) {
			KeywordUtil.markWarning("⚠️ Tidak bisa listen: koneksi belum terbentuk (reader null)")
			return
		}

		receivedMessages.clear()

		long endTime = System.currentTimeMillis() + (seconds * 1000)
		try {
			while (System.currentTimeMillis() < endTime && reader != null) {
				if (reader.ready()) {
					String line = reader.readLine()
					if (line != null) {
						KeywordUtil.logInfo("📩 Received: " + line)
						receivedMessages << line
					}
				}
				Thread.sleep(200)
			}
		} catch (Exception e) {
			KeywordUtil.markWarning("⚠️ Listen error: " + e.message)
		}
	}


	boolean responseContains(String keyword) {
		return receivedMessages.any { it.toLowerCase().contains(keyword.toLowerCase()) }
	}


	String getAllResponses() {
		return receivedMessages.join('\n')
	}


	boolean hasResponse() {
		return !receivedMessages.isEmpty()
	}

	def close() {
		try {
			reader?.close()
			writer?.close()
			socket?.close()
			KeywordUtil.logInfo("🔌 Socket closed")
		} catch (Exception e) {
			KeywordUtil.markWarning("⚠️ Error closing socket: " + e.message)
		}
	}
}