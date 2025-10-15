package com.utilities

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import com.kms.katalon.core.util.KeywordUtil
import java.math.BigDecimal
import java.util.List
import com.kms.katalon.core.annotation.Keyword // Pastikan import Keyword benar

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
		//  status untuk TB_FO_CRITERIAORDER
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
				return 'Reject' // Sebelumnya: Rejected
			case 'A':
				return 'Pending New' // Tetap
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

	/**
	 * Menerjemahkan kode STATUS dari TB_FO_BONDTRANSACTION.
	 * Kriteria: 'CR'=CONFIRMED, 'RQ'=PROCCESING, 'RJ'=REJECT
	 */
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
			// Menggunakan koneksi UTAMA (bnisfix)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			// --- VERIFIKASI TABEL 1: TB_FO_CRITERIAORDER ---
			def pstmtCriteria = conn.prepareStatement(sqlCriteria.trim())
			pstmtCriteria.setString(1, clientCode)

			rsCriteria = pstmtCriteria.executeQuery()

			if (rsCriteria.next()) {
				// Ambil Data Utama dari TB_FO_CRITERIAORDER
				String actualOrderId = rsCriteria.getString("ORD_ID")
				String actualStockCode = rsCriteria.getString("CRO_CRITERIASYMBOL")
				int actualLot = rsCriteria.getInt("CRO_LOT")
				BigDecimal actualPrice = rsCriteria.getBigDecimal("CRO_PRICE")
				String rawStatus = rsCriteria.getString("CRO_STATUS")
				String actualStatus = getCriteriaStatusDescription(rawStatus)

				// --- Logika Pencocokan Data Kriteria ---
				boolean stockMatch = actualStockCode.equalsIgnoreCase(expectedStockCode)
				boolean lotMatch = actualLot == expectedLot
				boolean priceMatch = actualPrice.compareTo(expectedPrice) == 0

				// --- Logika Verifikasi TB_FO_ORDER ---
				boolean orderTerkirim = false

				// Siapkan statement untuk TB_FO_ORDER
				def pstmtOrder = conn.prepareStatement(sqlOrder.trim())
				pstmtOrder.setString(1, clientCode)
				rsOrder = pstmtOrder.executeQuery()

				if (rsOrder.next()) {
					orderTerkirim = true
					String sentStockCode = rsOrder.getString("STK_INITIALCODE")
					String sentOrderID = rsOrder.getString("ORD_ID")

					// Jika order kriteria dan order yang terkirim cocok (Gagal karena seharusnya kriteria belum terpicu)
					if (sentStockCode.equalsIgnoreCase(expectedStockCode)) {
						KeywordUtil.markFailed("❌ Verifikasi GAGAL: Auto Order (Kriteria) ditemukan, TETAPI Order dengan saham ${sentStockCode} juga ditemukan di TB_FO_ORDER (Order ID: ${sentOrderID}). Seharusnya belum terkirim.")
						return false
					}
				}

				// --- HASIL VERIFIKASI AKHIR KRITERIA ---
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

	/**
	 * [FUNGSI BARU UNTUK REGULAR ORDER]
	 * Memverifikasi data order terbaru di TB_FO_ORDER (Order Transaksi Langsung).
	 * Telah disempurnakan untuk menerima List Status, parameter Side, dan List Tipe Board.
	 * * @param clientCode ID Klien
	 * @param expectedStockCode Kode Saham
	 * @param expectedLot Jumlah Lot
	 * @param expectedPrice Harga Order
	 * @param expectedStatuses List Status yang Diharapkan
	 * @param expectedSide Tipe Transaksi ('B' atau 'S')
	 * @param expectedBoardIDs List Tipe Board (misalnya ['RG', 'TN'])
	 */
	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyLatestRegularOrder(String clientCode, String expectedStockCode, int expectedLot, BigDecimal expectedPrice, List<String> expectedStatuses, String expectedSide, List<String> expectedBoardID) {
		Connection conn = null
		ResultSet rsOrder = null
		
		// --- 1. Persiapan Data Ekspektasi ---
		// Konversi Side (B/S) menjadi kode DB (1/2)
		String dbSideCode
		if (expectedSide.equalsIgnoreCase('B')) {
			dbSideCode = '1' // 1 = Buy
		} else if (expectedSide.equalsIgnoreCase('S')) {
			dbSideCode = '2' // 2 = Sell
		} else {
			KeywordUtil.markFailed("❌ Tipe transaksi tidak valid: Harus 'B' (Buy) atau 'S' (Sell). Diterima: ${expectedSide}")
			return false
		}
		
		// Query: Memeriksa data terbaru di TB_FO_ORDER
		// DITAMBAHKAN kolom BRD_ID di SELECT
		// CATATAN: QUERY TETAP DIBIARKAN AGAR TIDAK MENGGUNAKAN 'IN' KARENA BATASAN ORACLE/DRIVER.
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
			// Menggunakan koneksi UTAMA (bnisfix)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			def pstmtOrder = conn.prepareStatement(sqlOrder.trim())
			pstmtOrder.setString(1, clientCode)
			pstmtOrder.setString(2, dbSideCode) // Binding kode SIDE

			rsOrder = pstmtOrder.executeQuery()

			if (rsOrder.next()) {
				// Ambil Data Utama dari TB_FO_ORDER
				String actualOrderId = rsOrder.getString("ORD_ID")
				String actualStockCode = rsOrder.getString("SEC_ID")
				int actualLot = rsOrder.getInt("ORD_LOT")
				BigDecimal actualPrice = rsOrder.getBigDecimal("ORD_PRICE")
				String rawStatus = rsOrder.getString("ORS_ID")
				String actualSideRaw = rsOrder.getString("ORD_SIDE")
				
				// --- BARU: Ambil kolom BRD_ID ---
				String actualBoardID = rsOrder.getString("BRD_ID")

				// --- Tambahkan kolom Reject Date/Description ---
				String actualRejectDate = rsOrder.getString("ORS_REJECTDT")
				String actualRejectReason = rsOrder.getString("ORS_REJECTDESC")

				String actualStatus = getOrderStatusDescription(rawStatus)
				String actualSide = (actualSideRaw == '1' ? 'Buy (B)' : (actualSideRaw == '2' ? 'Sell (S)' : "Unknown (${actualSideRaw})"))

				// --- Logika Pencocokan Data Order Transaksi ---
				boolean stockMatch = actualStockCode.equalsIgnoreCase(expectedStockCode)
				boolean lotMatch = actualLot == expectedLot
				boolean priceMatch = actualPrice.compareTo(expectedPrice) == 0
				boolean sideMatch = actualSideRaw.equals(dbSideCode)
				boolean statusMatch = expectedStatuses.contains(actualStatus)
				
				// --- DIPERBAIKI: Logika Pencocokan Board ID (Cek apakah actual ada dalam List) ---
				boolean boardIDMatch = actualBoardID != null && expectedBoardID.contains(actualBoardID.toUpperCase())


				if (stockMatch && lotMatch && priceMatch && statusMatch && sideMatch && boardIDMatch) {
					KeywordUtil.logInfo("✅ Verifikasi DB Order Transaksi Berhasil (Termasuk BRD_ID).")
					KeywordUtil.logInfo("	[Detail Order]: ID: ${actualOrderId}, Side: ${actualSide}, Board ID: ${actualBoardID}, Status: ${actualStatus}, Saham: ${actualStockCode}, Lot: ${actualLot}, Harga: ${actualPrice}")
					// Log info Reject (Tanggal dan Alasan)
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
					// --- DIPERBAIKI: Log Error BRD_ID ---
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
			// Menggunakan koneksi UTAMA (bnisfix)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			def pstmtBond = conn.prepareStatement(sqlBond.trim())
			pstmtBond.setString(1, clientCode) // Binding USR_ID

			rsBond = pstmtBond.executeQuery()

			if (rsBond.next()) {
				// Ambil Data Utama dari TB_FO_BONDTRANSACTION
				//
				// !!! PENTING: MOHON VERIFIKASI NAMA-NAMA KOLOM DI BAWAH INI SESUAI DENGAN SKEMA DB ANDA.
				//              GANTI STRING DALAM TANDA KUTIP DENGAN NAMA KOLOM YANG SEBENARNYA JIKA BERBEDA.
				//
				String actualTransactionId = rsBond.getString("TRXID")
				String actualBondCode = rsBond.getString("BONDID")
				BigDecimal actualNominal = rsBond.getBigDecimal("NOMINAL")
				BigDecimal actualPrice = rsBond.getBigDecimal("PRICE")
				String actualTrxDate = rsBond.getString("TRXDATE")
				String rawStatus = rsBond.getString("STATUS") // <-- KEMUNGKINAN BESAR ERROR DI SINI
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
