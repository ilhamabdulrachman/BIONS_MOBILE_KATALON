package com.utilities

import com.kms.katalon.core.util.KeywordUtil

class TcpClient {
	Socket socket
	BufferedReader reader
	PrintWriter writer

	def connect(String host, int port) {
		try {
			socket = new Socket(host, port)
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
			writer = new PrintWriter(socket.getOutputStream(), true)

			KeywordUtil.logInfo("✅ Connected to TCP socket ${host}:${port}")
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Connection failed: " + e.message)
		}
	}

	def sendMessage(String message) {
		try {
			writer.println(message)
			KeywordUtil.logInfo("📤 Sent: " + message)
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Send failed: " + e.message)
		}
	}

	/**
	 * Listen for incoming messages for a given duration (in seconds)
	 */
	def listen(int seconds) {
		long endTime = System.currentTimeMillis() + (seconds * 1000)
		try {
			while (System.currentTimeMillis() < endTime && reader != null) {
				if (reader.ready()) {
					String line = reader.readLine()
					if (line != null) {
						KeywordUtil.logInfo("📩 Received: " + line)
					}
				}
				Thread.sleep(200) // biar CPU nggak full
			}
		} catch (Exception e) {
			KeywordUtil.markWarning("⚠️ Listen error: " + e.message)
		}
	}

	def close() {
		try {
			socket?.close()
			KeywordUtil.logInfo("🔌 Socket closed")
		} catch (Exception e) {
			KeywordUtil.markWarning("⚠️ Error closing socket: " + e.message)
		}
	}
}
