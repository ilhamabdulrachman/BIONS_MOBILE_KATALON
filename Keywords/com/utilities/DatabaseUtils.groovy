package com.utilities

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import com.kms.katalon.core.util.KeywordUtil
import java.math.BigDecimal

class OrderVerification {

	// --- 1. KONFIGURASI ORACLE ---
	private static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver"
	private static final String DB_URL = "jdbc:oracle:thin:@192.168.19.19:1521:fodev"
	private static final String DB_USER = "bnisfix"
	private static final String DB_PASS = "sysdev" 

	/**
	 * Memverifikasi data auto order terbaru di TB_FO_CRITERIAORDER.
	 */
	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyLatestOrder(String clientCode, String expectedStockCode, int expectedLot, BigDecimal expectedPrice) {
		Connection conn = null
		ResultSet rs = null

	
		String sql = """
            SELECT * FROM (
                SELECT * FROM BNISFIX.TB_FO_CRITERIAORDER 
                WHERE USR_ID = ? 
                ORDER BY CRO_REQENTDT DESC
            ) WHERE ROWNUM <= 1
        """

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			def pstmt = conn.prepareStatement(sql)
			pstmt.setString(1, clientCode) // Menggunakan clientCode

			rs = pstmt.executeQuery()

			if (rs.next()) {
				
				String actualStockCode = rs.getString("CRO_CRITERIASYMBOL") 
				int actualLot = rs.getInt("CRO_LOT") 
				BigDecimal actualPrice = rs.getBigDecimal("CRO_PRICE") 

				// --- Logika Verifikasi ---
				boolean stockMatch = actualStockCode.equalsIgnoreCase(expectedStockCode)
				boolean lotMatch = actualLot == expectedLot
				boolean priceMatch = actualPrice.compareTo(expectedPrice) == 0

				if (stockMatch && lotMatch && priceMatch) {
					KeywordUtil.logInfo("✅ Verifikasi DB Order Berhasil. Semua detail auto order cocok.")
					KeywordUtil.logInfo("   Kode Saham: ${actualStockCode}, Lot: ${actualLot}, Harga: ${actualPrice}")
					return true
				} else {
					KeywordUtil.markFailed("❌ Verifikasi DB Order GAGAL. Data tidak cocok.")
					KeywordUtil.logError("   Ekspektasi Saham: ${expectedStockCode}, Aktual: ${actualStockCode}")
					KeywordUtil.logError("   Ekspektasi Lot: ${expectedLot}, Aktual: ${actualLot}")
					KeywordUtil.logError("   Ekspektasi Harga: ${expectedPrice}, Aktual: ${actualPrice}")
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
			if (rs != null) rs.close()
			if (conn != null) conn.close()
		}
	}
}
