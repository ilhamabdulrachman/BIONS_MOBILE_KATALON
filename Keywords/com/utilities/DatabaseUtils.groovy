package com.utilities

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import com.kms.katalon.core.util.KeywordUtil
import java.math.BigDecimal
import java.util.List
import com.kms.katalon.core.annotation.Keyword
import java.util.ArrayList
import java.util.Map

class OrderVerification {

	// --- 1. KONFIGURASI ORACLE ---
	private static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver"

	// --- KONFIGURASI KONEKSI UTAMA (bnisfix) ---
	private static final String DB_URL = "jdbc:oracle:thin:@192.168.19.19:1521:fodev"
	private static final String DB_USER = "bnisfix"
	private static final String DB_PASS = "sysdev"

	// --- KONFIGURASI KONEKSI ALTERNATIF (bnisfo) ---
	private static final String DB_URL_ALT = "jdbc:oracle:thin:@192.168.19.19:1521:fodev"
	private static final String DB_USER_ALT = "bnisfo"
	private static final String DB_PASS_ALT = "sysdev"

	/**
	 * Menerjemahkan kode CRO_STATUS
	 */
	private static String getCriteriaStatusDescription(String statusCode) {
		// status untuk TB_FO_CRITERIAORDER
		switch (statusCode) {
			case '0':
				return 'Queuing'
			case 'R0':
				return 'Request'
			case 'R4':
				return 'Cancel Request'
			case '4':
				return 'Cancelled'
			case '8':
				return 'Rejected'
			case '1':
				return 'Executed'
			default:
				return "Unknown Status (${statusCode})"
		}
	}

	/**
	 * Menerjemahkan kode STATUS dari TB_FO_ORDER menjadi deskripsi yang mudah dibaca.
	 */
	private static String getOrderStatusDescription(String statusCode) {
		// Pemetaan status umum untuk order book utama (TB_FO_ORDER)
		switch (statusCode) {
			case '0':
				return 'Open' // Sebelumnya: New/Pending
			case '1':
				return 'Partial' // Sebelumnya: Partially Filled
			case '2':
				return 'Match (Executed)' // Sebelumnya: Filled (Executed)
			case '4':
				return 'Withdraw (Cancelled)' // Sebelumnya: Cancelled
			case '5':
				return 'Amend'
			case '8':
				return 'Reject'
			case 'A':
				return 'Pending New'
			case 'B1':
				return 'Hold Booking'
			case 'B2':
				return 'Booked'
			case 'D':
			case 'N5':
				return 'New Order Amend'
			case 'R0':
				return 'Request Entry'
			case 'R4':
				return 'Request Withdraw (Cancel Request)'
			case 'R5':
				return 'Request Amend'
			case 'RB':
			case 'RC':
			case 'RD':
			case 'RT':
			case 'T':
				return 'Temporary'
			default:
				return "Unknown Status (${statusCode})"
		}
	}


	private static String getBondTransactionStatusDescription(String statusCode) {
		// Pemetaan status untuk TB_FO_BONDTRANSACTION
		switch (statusCode) {
			case 'CR':
				return 'CONFIRMED'
			case 'RQ':
				return 'PROCCESING'
			case 'RJ':
				return 'REJECT'
			default:
				return "Unknown Status (${statusCode})"
		}
	}

	/**
	 * [FUNGSI UTAMA UNTUK KRITERIA/AUTO ORDER]
	 * Memverifikasi data auto order terbaru di TB_FO_CRITERIAORDER dan memastikan
	 * order BUKAN order yang terkirim di TB_FO_ORDER.
	 */
	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyLatestOrder(String clientCode, String expectedStockCode, int expectedLot, BigDecimal expectedPrice) {
		Connection conn = null
		ResultSet rsCriteria = null
		ResultSet rsOrder = null

		// Query 1: Memeriksa data terbaru di TB_FO_CRITERIAORDER
		String sqlCriteria = """
			SELECT * FROM (
				SELECT * FROM BNISFIX.TB_FO_CRITERIAORDER
				WHERE USR_ID = ?
				ORDER BY CRO_REQENTDT DESC
			) WHERE ROWNUM <= 1
		"""

		// Query 2: Memeriksa apakah order yang SAMA masuk ke TB_FO_ORDER
		String sqlOrder = """
			SELECT * FROM (
				SELECT * FROM BNISFIX.TB_FO_ORDER
				WHERE CLS_INITIALCODE = ?
				ORDER BY ORDER_TIME DESC 
			) WHERE ROWNUM <= 1
		"""

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			def pstmtCriteria = conn.prepareStatement(sqlCriteria.trim())
			pstmtCriteria.setString(1, clientCode)

			rsCriteria = pstmtCriteria.executeQuery()

			if (rsCriteria.next()) {
				String actualOrderId = rsCriteria.getString("ORD_ID")
				String actualStockCode = rsCriteria.getString("CRO_CRITERIASYMBOL")
				int actualLot = rsCriteria.getInt("CRO_LOT")
				BigDecimal actualPrice = rsCriteria.getBigDecimal("CRO_PRICE")
				String rawStatus = rsCriteria.getString("CRO_STATUS")
				String actualStatus = getCriteriaStatusDescription(rawStatus)

				boolean stockMatch = actualStockCode.equalsIgnoreCase(expectedStockCode)
				boolean lotMatch = actualLot == expectedLot
				boolean priceMatch = actualPrice.compareTo(expectedPrice) == 0

				boolean orderTerkirim = false

				def pstmtOrder = conn.prepareStatement(sqlOrder.trim())
				pstmtOrder.setString(1, clientCode)
				rsOrder = pstmtOrder.executeQuery()

				if (rsOrder.next()) {
					orderTerkirim = true
					String sentStockCode = rsOrder.getString("STK_INITIALCODE")
					String sentOrderID = rsOrder.getString("ORD_ID")

					if (sentStockCode.equalsIgnoreCase(expectedStockCode)) {
						KeywordUtil.markFailed("❌ Verifikasi GAGAL: Auto Order (Kriteria) ditemukan, TETAPI Order dengan saham ${sentStockCode} juga ditemukan di TB_FO_ORDER (Order ID: ${sentOrderID}). Seharusnya belum terkirim.")
						return false
					}
				}

				if (stockMatch && lotMatch && priceMatch && !orderTerkirim) {
					KeywordUtil.logInfo("✅ Verifikasi DB Berhasil: Data kriteria cocok dan order belum terkirim ke Order Book.")
					KeywordUtil.logInfo("	[Kriteria Order]: ID: ${actualOrderId}, Status: ${actualStatus}, Saham: ${actualStockCode}, Lot: ${actualLot}, Harga: ${actualPrice}")
					return true
				} else if (orderTerkirim) {
					KeywordUtil.markFailed("❌ Verifikasi GAGAL: Order ditemukan di Order Book utama. Order Kriteria seharusnya hanya tersimpan, belum terkirim.")
					return false
				}
				else {
					KeywordUtil.markFailed("❌ Verifikasi Kriteria GAGAL: Detail data kriteria tidak cocok.")
					KeywordUtil.logError("	Order ID Ditemukan: ${actualOrderId}, Status: ${actualStatus}")
					KeywordUtil.logError("	Ekspektasi Saham: ${expectedStockCode}, Aktual: ${actualStockCode}")
					KeywordUtil.logError("	Ekspektasi Lot: ${expectedLot}, Aktual: ${actualLot}")
					KeywordUtil.logError("	Ekspektasi Harga: ${expectedPrice}, Aktual: ${actualPrice}")
					return false
				}
			} else {
				KeywordUtil.markFailed("❌ Tidak ada order yang ditemukan di TB_FO_CRITERIAORDER untuk USR_ID: ${clientCode}")
				return false
			}
		} catch (Exception e) {

			KeywordUtil.markFailed("❌ Error Koneksi/Query DB. Pesan Error: " + e.getMessage())
			return false
		} finally {
			if (rsCriteria != null) rsCriteria.close()
			if (rsOrder != null) rsOrder.close()
			if (conn != null) conn.close()
		}
	}



	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyLatestRegularOrder(String clientCode, String expectedStockCode, int expectedLot, BigDecimal expectedPrice, List<String> expectedStatuses, String expectedSide, List<String> expectedBoardID) {
		Connection conn = null
		ResultSet rsOrder = null


		String dbSideCode
		if (expectedSide.equalsIgnoreCase('B')) {
			dbSideCode = '1'
		} else if (expectedSide.equalsIgnoreCase('S')) {
			dbSideCode = '2'
		} else {
			KeywordUtil.markFailed("❌ Tipe transaksi tidak valid: Harus 'B' (Buy) atau 'S' (Sell). Diterima: ${expectedSide}")
			return false
		}


		String sqlOrder = """
			SELECT * FROM (
				SELECT * FROM BNISFIX.TB_FO_ORDER
				WHERE CLS_INITIALCODE = ?
				 AND ORD_SIDE = ?
				ORDER BY ORS_REQENTDT DESC
			) WHERE ROWNUM <= 1
		"""

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			def pstmtOrder = conn.prepareStatement(sqlOrder.trim())
			pstmtOrder.setString(1, clientCode)
			pstmtOrder.setString(2, dbSideCode)

			rsOrder = pstmtOrder.executeQuery()

			if (rsOrder.next()) {
				String actualOrderId = rsOrder.getString("ORD_ID")
				String actualStockCode = rsOrder.getString("SEC_ID")
				int actualLot = rsOrder.getInt("ORD_LOT")
				BigDecimal actualPrice = rsOrder.getBigDecimal("ORD_PRICE")
				String rawStatus = rsOrder.getString("ORS_ID")
				String actualSideRaw = rsOrder.getString("ORD_SIDE")

				String actualBoardID = rsOrder.getString("BRD_ID")

				String actualRejectDate = rsOrder.getString("ORS_REJECTDT")
				String actualRejectReason = rsOrder.getString("ORS_REJECTDESC")

				String actualStatus = getOrderStatusDescription(rawStatus)
				String actualSide = (actualSideRaw == '1' ? 'Buy (B)' : (actualSideRaw == '2' ? 'Sell (S)' : "Unknown (${actualSideRaw})"))

				boolean stockMatch = actualStockCode.equalsIgnoreCase(expectedStockCode)
				boolean lotMatch = actualLot == expectedLot
				boolean priceMatch = actualPrice.compareTo(expectedPrice) == 0
				boolean sideMatch = actualSideRaw.equals(dbSideCode)
				boolean statusMatch = expectedStatuses.contains(actualStatus)

				boolean boardIDMatch = actualBoardID != null && expectedBoardID.contains(actualBoardID.toUpperCase())


				if (stockMatch && lotMatch && priceMatch && statusMatch && sideMatch && boardIDMatch) {
					KeywordUtil.logInfo("✅ Verifikasi DB Order Transaksi Berhasil (Termasuk BRD_ID).")
					KeywordUtil.logInfo("	[Detail Order]: ID: ${actualOrderId}, Side: ${actualSide}, Board ID: ${actualBoardID}, Status: ${actualStatus}, Saham: ${actualStockCode}, Lot: ${actualLot}, Harga: ${actualPrice}")
					KeywordUtil.logInfo("	[Reject Info]: Tanggal (ORS_REJECTDT): ${actualRejectDate}, Alasan (ORS_REJECTDESC): ${actualRejectReason}")
					return true
				} else {
					KeywordUtil.markFailed("❌ Verifikasi DB Order Transaksi GAGAL. Data tidak cocok.")
					KeywordUtil.logError("	Order ID Ditemukan: ${actualOrderId}, Status: ${actualStatus} (Raw: ${rawStatus})")
					KeywordUtil.logError("	Ekspektasi Saham: ${expectedStockCode}, Aktual: ${actualStockCode}")
					KeywordUtil.logError("	Ekspektasi Lot: ${expectedLot}, Aktual: ${actualLot}")
					KeywordUtil.logError("	Ekspektasi Harga: ${expectedPrice}, Aktual: ${actualPrice}")
					KeywordUtil.logError("	Ekspektasi Side: ${expectedSide} (DB: ${dbSideCode}), Aktual: ${actualSide}")
					KeywordUtil.logError("	Ekspektasi Status (List): ${expectedStatuses}, Aktual: ${actualStatus} (Raw: ${rawStatus})")
					KeywordUtil.logError("	Ekspektasi Board ID (List): ${expectedBoardID}, Aktual: ${actualBoardID}")
					KeywordUtil.logError("	[Reject Info]: Tanggal (ORS_REJECTDT): ${actualRejectDate}, Alasan (ORS_REJECTDESC): ${actualRejectReason}")
					return false
				}
			} else {
				KeywordUtil.markFailed("❌ Tidak ada order yang ditemukan di TB_FO_ORDER untuk CLS_INITIALCODE: ${clientCode} dan SIDE: ${expectedSide} (DB: ${dbSideCode})")
				return false
			}
		} catch (Exception e) {

			KeywordUtil.markFailed("❌ Error Koneksi/Query DB. Pesan Error: " + e.getMessage())
			return false
		} finally {
			if (rsOrder != null) rsOrder.close()
			if (conn != null) conn.close()
		}
	}


	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyMultipleOrders(List<Map> expectedOrders, String clientCode) {
		Connection conn = null
		ResultSet rsOrders = null
		boolean overallResult = true

		if (expectedOrders == null || expectedOrders.isEmpty()) {
			KeywordUtil.markFailed("❌ Input GAGAL: List of expectedOrders tidak boleh kosong.")
			return false
		}

		int ordersToFetch = expectedOrders.size()
		KeywordUtil.logInfo("Proses Verifikasi Multiple Order dimulai. Ekspektasi ${ordersToFetch} order terbaru untuk client: ${clientCode}")

		// Query untuk mengambil N order terbaru, diurutkan berdasarkan waktu request entry (ORS_REQENTDT)
		String sqlMultipleOrders = """
			SELECT ORD_ID, SEC_ID, ORD_LOT, ORD_PRICE, ORD_SIDE, ORS_ID, BRD_ID, ORS_REJECTDT, ORS_REJECTDESC FROM BNISFIX.TB_FO_ORDER
			WHERE CLS_INITIALCODE = ?
			ORDER BY ORS_REQENTDT DESC
			FETCH FIRST ? ROWS ONLY
		"""
		// NOTE: FETCH FIRST N ROWS ONLY adalah standar SQL modern, ROWNUM <= N juga bisa digunakan
		// Disesuaikan kembali dengan ROWNUM agar kompatibel dengan Oracle versi lama yang mungkin digunakan.
		String sqlMultipleOrdersRowNum = """
			SELECT * FROM (
				SELECT ORD_ID, SEC_ID, ORD_LOT, ORD_PRICE, ORD_SIDE, ORS_ID, BRD_ID, ORS_REJECTDT, ORS_REJECTDESC FROM BNISFIX.TB_FO_ORDER
				WHERE CLS_INITIALCODE = ?
				ORDER BY ORS_REQENTDT DESC
			) WHERE ROWNUM <= ?
		"""


		// List untuk menyimpan data aktual yang ditemukan di DB
		List<Map> actualOrders = new ArrayList<Map>()

		try {
			Class.forName(DB_DRIVER)
			// Menggunakan koneksi UTAMA (bnisfix)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			// --- 1. FETCH DATA AKTUAL DARI DB ---
			def pstmtOrders = conn.prepareStatement(sqlMultipleOrdersRowNum.trim())
			pstmtOrders.setString(1, clientCode)
			pstmtOrders.setInt(2, ordersToFetch)

			rsOrders = pstmtOrders.executeQuery()

			// Loop untuk menyimpan hasil query ke List<Map>
			while (rsOrders.next()) {
				String rawSide = rsOrders.getString("ORD_SIDE")
				String side = rawSide == '1' ? 'BUY' : (rawSide == '2' ? 'SELL' : rawSide)

				// Konversi raw status menjadi deskripsi untuk perbandingan status yang lebih mudah
				String rawStatus = rsOrders.getString("ORS_ID")
				String statusDesc = getOrderStatusDescription(rawStatus)

				// Pastikan semua kolom yang diambil tidak null sebelum dimasukkan ke Map
				actualOrders.add([
					ordId: rsOrders.getString("ORD_ID"),
					stockCode: rsOrders.getString("SEC_ID"),
					lotAmount: rsOrders.getInt("ORD_LOT"),
					price: rsOrders.getBigDecimal("ORD_PRICE"),
					side: side,
					rawStatus: rawStatus,
					statusDesc: statusDesc,
					boardID: rsOrders.getString("BRD_ID")?.toUpperCase(), // Pastikan uppercase untuk matching
					rejectDate: rsOrders.getString("ORS_REJECTDT"),
					rejectReason: rsOrders.getString("ORS_REJECTDESC"),
					// Flag untuk menandai order sudah dicocokkan
					matched: false
				])
			}

			// --- 2. VALIDASI JUMLAH DATA ---
			if (actualOrders.size() < ordersToFetch) {
				KeywordUtil.markFailed("❌ Verifikasi GAGAL: Hanya ditemukan ${actualOrders.size()} order, tetapi diekspektasikan ${ordersToFetch} order. Order mungkin belum masuk/terkirim.")
				return false
			}

			// --- 3. MATCHING DATA (Expected vs Actual) ---
			for (int i = 0; i < expectedOrders.size(); i++) {
				Map expected = expectedOrders.get(i)
				KeywordUtil.logInfo("Memverifikasi Order Ekspektasi #${i + 1}: Saham ${expected.stockCode}, Side ${expected.side}, Lot ${expected.lotAmount}")

				// Ambil dan konversi ekspektasi
				String expectedStockCode = expected.get("stockCode")?.toString()
				String expectedSide = expected.get("side")?.toString()?.toUpperCase()
				// Konversi ke tipe yang tepat (int atau BigDecimal) dan penanganan null
				int expectedLot = expected.get("lotAmount") ? expected.get("lotAmount") as int : -1
				BigDecimal expectedPrice = expected.get("expectedPrice") ? expected.get("expectedPrice") as BigDecimal : new BigDecimal(-1)

				// Perbaikan: Konversi List dengan aman. Jika null, inisialisasi dengan ArrayList kosong.
				List<String> expectedStatuses = (List<String>) (expected.get("expectedStatuses") ?: new ArrayList<String>())
				List<String> expectedBoardIDs = (List<String>) (expected.get("expectedBoardIDs") ?: new ArrayList<String>())

				// Validasi input ekspektasi yang wajib ada. Cek List menggunakan .isEmpty() jika hasil dari Map input tidak dijamin List<String>
				if (!expectedStockCode || !expectedSide || expectedLot == -1 || expectedPrice.compareTo(new BigDecimal(-1)) == 0 || expectedStatuses.isEmpty() || expectedBoardIDs.isEmpty()) {
					KeywordUtil.markFailed("❌ Verifikasi Order #${i + 1} GAGAL: Parameter ekspektasi wajib (stockCode, side, lotAmount, expectedPrice, expectedStatuses, expectedBoardIDs) tidak lengkap atau tidak valid. Pastikan List Status dan Board tidak kosong.")
					overallResult = false
					continue
				}

				// Coba cari kecocokan di data aktual
				int matchIndex = -1

				for (int j = 0; j < actualOrders.size(); j++) {
					Map actual = actualOrders.get(j)

					// Lewati order aktual yang sudah dicocokkan (matched: true)
					if (actual.matched) continue

						// Cek semua kriteria
						boolean stockMatch = actual.stockCode.equalsIgnoreCase(expectedStockCode)
					boolean lotMatch = actual.lotAmount == expectedLot
					boolean priceMatch = actual.price.compareTo(expectedPrice) == 0
					boolean sideMatch = actual.side.equalsIgnoreCase(expectedSide)

					// Cek status: actual status description harus ada di list expected statuses
					boolean statusMatch = actual.statusDesc != null && expectedStatuses.contains(actual.statusDesc)

					// Cek Board ID: actual board ID harus ada di list expected board IDs
					boolean boardIDMatch = actual.boardID != null && expectedBoardIDs.contains(actual.boardID)

					if (stockMatch && lotMatch && priceMatch && sideMatch && statusMatch && boardIDMatch) {
						matchIndex = j // Kecocokan ditemukan

						// Tandai order aktual ini sudah dicocokkan agar tidak dipakai lagi
						actual.matched = true
						KeywordUtil.logInfo("	✅ Match Berhasil dengan Order DB ID: ${actual.ordId}, Status: ${actual.statusDesc}, Board: ${actual.boardID}")
						break
					}
				}

				// Jika setelah looping tidak ada kecocokan
				if (matchIndex == -1) {
					KeywordUtil.markFailed("❌ Verifikasi Order #${i + 1} GAGAL: Tidak ada order aktual yang cocok dengan ekspektasi.")
					KeywordUtil.logError("	Ekspektasi: Saham: ${expectedStockCode}, Side: ${expectedSide}, Lot: ${expectedLot}, Harga: ${expectedPrice}, Status: ${expectedStatuses}, Board: ${expectedBoardIDs}")
					overallResult = false
					// Lanjutkan ke order berikutnya, tetapi hasil akhir akan false
				}
			}

			// Log hasil akhir
			if (overallResult) {
				KeywordUtil.logInfo("🎉 Verifikasi DB Multiple Order Berhasil! Semua ${ordersToFetch} order cocok dengan data aktual.")
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Error Koneksi/Query DB. Pesan Error: " + e.getMessage())
			overallResult = false
		} finally {
			if (rsOrders != null) rsOrders.close()
			if (conn != null) conn.close()
		}

		return overallResult
	}

	/**
	 * [FUNGSI UTAMA UNTUK BOND TRANSACTION]
	 * Memverifikasi data transaksi Obligasi/Bond terbaru di TB_FO_BONDTRANSACTION.
	 * Memerlukan verifikasi nama kolom di DB.
	 */
	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyLatestBondTransaction(String clientCode, String expectedBondCode, BigDecimal expectedNominal, BigDecimal expectedPrice, List<String> expectedStatuses) {
		Connection conn = null
		ResultSet rsBond = null

		// Query: Memeriksa data terbaru di TB_FO_BONDTRANSACTION
		String sqlBond = """
			SELECT * FROM (
				SELECT * FROM BNISFO.TB_FO_BONDTRANSACTION 
				WHERE USR_ID = ?
				ORDER BY TRXDATE DESC
			) WHERE ROWNUM <= 1
		"""

		try {
			Class.forName(DB_DRIVER)

			// PERBAIKAN: Menggunakan koneksi ALTERNATIF (bnisfo) karena query mengakses skema BNISFO.
			conn = DriverManager.getConnection(DB_URL_ALT, DB_USER_ALT, DB_PASS_ALT)

			def pstmtBond = conn.prepareStatement(sqlBond.trim())
			pstmtBond.setString(1, clientCode) // Binding USR_ID

			rsBond = pstmtBond.executeQuery()

			if (rsBond.next()) {

				String actualTransactionId = rsBond.getString("TRXID")
				String actualBondCode = rsBond.getString("BONDID")
				BigDecimal actualNominal = rsBond.getBigDecimal("NOMINAL")
				BigDecimal actualPrice = rsBond.getBigDecimal("PRICE")
				String actualTrxDate = rsBond.getString("TRXDATE")
				String rawStatus = rsBond.getString("STATUS") // <-- Asumsi nama kolom 'STATUS' benar
				String actualRejectReason = rsBond.getString("REJECT_DESC")

				// Konversi Status Bond
				String actualStatus = getBondTransactionStatusDescription(rawStatus)

				// --- Logika Pencocokan Data Bond Transaksi ---
				boolean bondCodeMatch = actualBondCode.equalsIgnoreCase(expectedBondCode)
				boolean nominalMatch = actualNominal.compareTo(expectedNominal) == 0
				boolean priceMatch = actualPrice.compareTo(expectedPrice) == 0

				// Verifikasi status order: cek apakah status aktual ada di dalam List status yang diekspektasi
				boolean statusMatch = expectedStatuses.contains(actualStatus)

				if (bondCodeMatch && nominalMatch && priceMatch && statusMatch) {
					KeywordUtil.logInfo("✅ Verifikasi DB Bond Transaksi Berhasil (CODE, NOMINAL, PRICE, STATUS).")
					KeywordUtil.logInfo("	[Detail Bond]: ID: ${actualTransactionId}, Bond: ${actualBondCode}, Nominal: ${actualNominal}, Price: ${actualPrice}, Status: ${actualStatus} (Raw: ${rawStatus})")
					KeywordUtil.logInfo("	[Info Tambahan]: TRX Date: ${actualTrxDate}, Reject Reason: ${actualRejectReason}")
					return true
				} else {
					KeywordUtil.markFailed("❌ Verifikasi DB Bond Transaksi GAGAL. Data tidak cocok.")
					KeywordUtil.logError("	Transaksi ID Ditemukan: ${actualTransactionId}")
					KeywordUtil.logError("	Ekspektasi Bond Code: ${expectedBondCode}, Aktual: ${actualBondCode}")
					KeywordUtil.logError("	Ekspektasi Nominal: ${expectedNominal}, Aktual: ${actualNominal}")
					KeywordUtil.logError("	Ekspektasi Price: ${expectedPrice}, Aktual: ${actualPrice}")
					KeywordUtil.logError("	Ekspektasi Status (List): ${expectedStatuses}, Aktual: ${actualStatus} (Raw: ${rawStatus})")
					KeywordUtil.logError("	[Info Tambahan]: TRX Date: ${actualTrxDate}, Reject Reason: ${actualRejectReason}")
					return false
				}
			} else {
				KeywordUtil.markFailed("❌ Tidak ada transaksi Bond yang ditemukan di TB_FO_BONDTRANSACTION untuk USR_ID: ${clientCode}")
				return false
			}
		} catch (Exception e) {

			KeywordUtil.markFailed("❌ Error Koneksi/Query DB. Pesan Error: " + e.getMessage())
			return false
		} finally {
			if (rsBond != null) rsBond.close()
			if (conn != null) conn.close()
		}
	}
}
