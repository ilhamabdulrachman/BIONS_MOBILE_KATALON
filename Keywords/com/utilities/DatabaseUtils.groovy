package com.utilities
import java.sql.Connection
import java.sql.*
import java.sql.DriverManager
import java.sql.ResultSet
import com.kms.katalon.core.util.KeywordUtil
import java.math.BigDecimal
import java.util.List
import com.kms.katalon.core.annotation.Keyword
import java.util.ArrayList
import java.util.Map



class OrderVerification {
	private static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver"
	// --- KONFIGURASI KONEKSI UTAMA (bnisfix - fodev) ---
	private static final String DB_URL = "jdbc:oracle:thin:@192.168.19.19:1521:fodev"
	private static final String DB_USER = "bnisfix"
	private static final String DB_PASS = "sysdev"

	// --- KONFIGURASI KONEKSI SFO (fodev) ---
	private static final String DB_URL_SFO = "jdbc:oracle:thin:@192.168.19.19:1521:fodev"
	private static final String DB_USER_SFO = "bnisfo"
	private static final String DB_PASS_SFO = "sysdev"

	// --- KONFIGURASI KONEKSI HISTORI (histdev) ---
	private static final String DB_URL_HIST = "jdbc:oracle:thin:@192.168.19.19:1521:histdev"
	private static final String DB_USER_HIST = "dbfeed"
	private static final String DB_PASS_HIST = "sysdev"
	
	// --- KONFIGURASI KONEKSI DBFEED (FEEDDEV) ---
	private static final String DB_URL_feed = "jdbc:oracle:thin:@192.168.19.19:1521:feeddev"
	private static final String DB_USER_feed = "dbfeed"
	private static final String DB_PASS_feed = "sysdev"

	/** * Helper untuk ke database fodev (SFO)
	 */
	private static Connection getConnectionSFO() throws Exception {
		Class.forName(DB_DRIVER)
		return DriverManager.getConnection(DB_URL_SFO, DB_USER_SFO, DB_PASS_SFO)
	}

	/** * Helper untuk ke database histdev (DBFEED)
	 */
	private static Connection getConnectionHist() throws Exception {
		try {
			Class.forName(DB_DRIVER)
			return DriverManager.getConnection(DB_URL_HIST, DB_USER_HIST, DB_PASS_HIST)
		} catch (ClassNotFoundException e) {
			throw new Exception("Oracle Driver tidak ditemukan: " + e.getMessage())
		} catch (SQLException e) {
			throw new Exception("Gagal koneksi ke HISTDEV: " + e.getMessage())
		}
	}

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
				return 'Open'
			case '1':
				return 'Partial'
			case '2':
				return 'Match (Executed)'
			case '4':
				return 'Withdraw (Cancelled)'
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
				return 'PROCESSING'
			case 'RJ':
				return 'REJECT'
			default:
				return "Unknown Status (${statusCode})"
		}
	}

	private static String getMutualFundStatusDescription(String statusCode) {
		switch (statusCode) {
			case 'R0':
				return 'Received'
			case '0':
				return 'Processing'
			case '1':
				return 'Waiting to confirm'
			case '2':
				return 'Confirmed'
			case '4':
				return 'Canceled'
			case '9':
				return 'Rejected'
			default:
				return "Unknown MF Status (${statusCode})"
		}
	}

	private static String getSBNOrderStatusDescription(String statusCode) {
		switch (statusCode) {
			case 'CP':
				return 'Complete'
			case 'RQ':
				return 'Request'
			default:
				return "Unknown SBN Status (${statusCode})"
		}
	}

	@Keyword
	static boolean validateAccountDataIntegrity(
			String clientCode,
			String expectedName,
			BigDecimal expectedFeeBuy,
			BigDecimal expectedFeeSell,
			String expectedBankRDI,
			String expectedInvAccNo,
			BigDecimal expectedCashOnHand,
			BigDecimal expectedBegOutstanding,
			BigDecimal expectedCurrentOutstanding
	) {
		Connection conn = null
		ResultSet rs = null

		String sql = """
        SELECT
            CAC_NAME,
            CAC_FEEBUY_OLT,
            CAC_FEESELL_OLT,
            BANKRDI,
            CAC_INVACCNO,
            CAC_CURRENTCASHONHAND,
            CAC_BEGOUTSTANDING,
            CAC_CURRENTOUTSTANDING
        FROM BNISFO.TB_FO_ACCOUNT
        WHERE CLS_INITIALCODE = ?
    """

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL_SFO, DB_USER_SFO, DB_PASS_SFO)

			def pstmt = conn.prepareStatement(sql.trim())
			pstmt.setString(1, clientCode)
			rs = pstmt.executeQuery()

			if (!rs.next()) {
				KeywordUtil.markFailed("❌ Tidak ditemukan data akun di TB_FO_ACCOUNT untuk Client Code: ${clientCode}")
				return false
			}

			// --- Ambil data aktual dari DB ---
			String actualName            = rs.getString("CAC_NAME")
			BigDecimal actualFeeBuy      = rs.getBigDecimal("CAC_FEEBUY_OLT")
			BigDecimal actualFeeSell     = rs.getBigDecimal("CAC_FEESELL_OLT")
			String actualBankRDI         = rs.getString("BANKRDI")
			String actualInvAccNo        = rs.getString("CAC_INVACCNO")
			BigDecimal actualCashOnHand  = rs.getBigDecimal("CAC_CURRENTCASHONHAND")
			BigDecimal actualBegOutstanding     = rs.getBigDecimal("CAC_BEGOUTSTANDING")
			BigDecimal actualCurrentOutstanding = rs.getBigDecimal("CAC_CURRENTOUTSTANDING")

			// --- Pencocokan tiap field ---
			boolean nameMatch       = actualName?.trim()?.equalsIgnoreCase(expectedName?.trim())
			boolean feeBuyMatch     = expectedFeeBuy == null || actualFeeBuy?.compareTo(expectedFeeBuy) == 0
			boolean feeSellMatch    = expectedFeeSell == null || actualFeeSell?.compareTo(expectedFeeSell) == 0
			boolean bankRDIMatch    = actualBankRDI?.trim()?.equalsIgnoreCase(expectedBankRDI?.trim())
			boolean invAccNoMatch   = actualInvAccNo?.trim()?.equalsIgnoreCase(expectedInvAccNo?.trim())
			boolean cashMatch       = expectedCashOnHand == null || actualCashOnHand?.compareTo(expectedCashOnHand) == 0
			boolean begOutMatch     = expectedBegOutstanding == null || actualBegOutstanding?.compareTo(expectedBegOutstanding) == 0
			boolean curOutMatch     = expectedCurrentOutstanding == null || actualCurrentOutstanding?.compareTo(expectedCurrentOutstanding) == 0

			boolean allMatch = nameMatch && feeBuyMatch && feeSellMatch && bankRDIMatch &&
					invAccNoMatch && cashMatch && begOutMatch && curOutMatch

			if (allMatch) {
				KeywordUtil.logInfo("✅ Verifikasi Account Data Integrity BERHASIL untuk Client: ${clientCode}")
				KeywordUtil.logInfo(
						"[Account] Name=${actualName}, FeeBuy=${actualFeeBuy}%, FeeSell=${actualFeeSell}%, " +
						"RDN=${actualBankRDI}, SRE=${actualInvAccNo}, " +
						"CashOnHand=${actualCashOnHand}, BegOutstanding=${actualBegOutstanding}, " +
						"CurrentOutstanding=${actualCurrentOutstanding}"
						)
				return true
			} else {
				KeywordUtil.markFailed("❌ Verifikasi Account Data Integrity GAGAL untuk Client: ${clientCode}")
				KeywordUtil.logError("	Name       -> Expected: ${expectedName}, Actual: ${actualName}, Match: ${nameMatch}")
				KeywordUtil.logError("	FeeBuy     -> Expected: ${expectedFeeBuy}, Actual: ${actualFeeBuy}, Match: ${feeBuyMatch}")
				KeywordUtil.logError("	FeeSell    -> Expected: ${expectedFeeSell}, Actual: ${actualFeeSell}, Match: ${feeSellMatch}")
				KeywordUtil.logError("	BankRDI    -> Expected: ${expectedBankRDI}, Actual: ${actualBankRDI}, Match: ${bankRDIMatch}")
				KeywordUtil.logError("	InvAccNo   -> Expected: ${expectedInvAccNo}, Actual: ${actualInvAccNo}, Match: ${invAccNoMatch}")
				KeywordUtil.logError("	CashOnHand -> Expected: ${expectedCashOnHand}, Actual: ${actualCashOnHand}, Match: ${cashMatch}")
				KeywordUtil.logError("	BegOutstanding -> Expected: ${expectedBegOutstanding}, Actual: ${actualBegOutstanding}, Match: ${begOutMatch}")
				KeywordUtil.logError("	CurrentOutstanding -> Expected: ${expectedCurrentOutstanding}, Actual: ${actualCurrentOutstanding}, Match: ${curOutMatch}")
				return false
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Error Koneksi/Query DB pada validateAccountDataIntegrity. Pesan Error: " + e.getMessage())
			return false
		} finally {
			if (rs != null) rs.close()
			if (conn != null) conn.close()
		}
	}

	@Keyword
	static boolean checkAccountDataExists(String accountAlias) {
		Connection conn = null
		ResultSet rs = null

		String sql = """
        SELECT
            CAC_NAME,
            CAC_ALIAS,
            CLS_INITIALCODE,
            CAC_FEEBUY_OLT,
            CAC_FEESELL_OLT,
            BANKRDI,
            CAC_INVACCNO,
            CAC_CURRENTCASHONHAND,
            CAC_BEGOUTSTANDING,
            CAC_CURRENTOUTSTANDING
        FROM BNISFO.TB_FO_ACCOUNT
        WHERE CAC_ALIAS = ?
    """

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL_SFO, DB_USER_SFO, DB_PASS_SFO)

			def pstmt = conn.prepareStatement(sql.trim())
			pstmt.setString(1, accountAlias)
			rs = pstmt.executeQuery()

			if (!rs.next()) {
				KeywordUtil.markFailed("❌ Tidak ditemukan data akun untuk Alias: ${accountAlias}")
				return false
			}

			// --- Ambil data aktual dari DB ---
			String name              = rs.getString("CAC_NAME")
			String alias             = rs.getString("CAC_ALIAS")
			String clientCode        = rs.getString("CLS_INITIALCODE")
			BigDecimal feeBuy        = rs.getBigDecimal("CAC_FEEBUY_OLT")
			BigDecimal feeSell       = rs.getBigDecimal("CAC_FEESELL_OLT")
			String bankRDI           = rs.getString("BANKRDI")
			String invAccNo          = rs.getString("CAC_INVACCNO")
			BigDecimal cashOnHand    = rs.getBigDecimal("CAC_CURRENTCASHONHAND")
			BigDecimal begOutstanding     = rs.getBigDecimal("CAC_BEGOUTSTANDING")
			BigDecimal currentOutstanding = rs.getBigDecimal("CAC_CURRENTOUTSTANDING")

			List<String> issues = []

			// --- Validasi Data Tidak Null / Tidak Kosong ---
			if (!name || name.trim().isEmpty()) {
				issues << "CAC_NAME kosong/null"
			}
			if (!clientCode || clientCode.trim().isEmpty()) {
				issues << "CLS_INITIALCODE kosong/null"
			}
			if (feeBuy == null) {
				issues << "CAC_FEEBUY_OLT null"
			}
			if (feeSell == null) {
				issues << "CAC_FEESELL_OLT null"
			}
			if (!bankRDI || bankRDI.trim().isEmpty()) {
				issues << "BANKRDI (RDN) kosong/null"
			}
			if (!invAccNo || invAccNo.trim().isEmpty()) {
				issues << "CAC_INVACCNO (SRE) kosong/null"
			}
			if (cashOnHand == null) {
				issues << "CAC_CURRENTCASHONHAND null"
			}
			if (currentOutstanding == null) {
				issues << "CAC_CURRENTOUTSTANDING null"
			}

			// --- Validasi Nilai Masuk Akal (Sanity Check) ---
			if (feeBuy != null && (feeBuy.compareTo(BigDecimal.ZERO) < 0 || feeBuy.compareTo(new BigDecimal("100")) > 0)) {
				issues << "CAC_FEEBUY_OLT tidak masuk akal: ${feeBuy}%"
			}
			if (feeSell != null && (feeSell.compareTo(BigDecimal.ZERO) < 0 || feeSell.compareTo(new BigDecimal("100")) > 0)) {
				issues << "CAC_FEESELL_OLT tidak masuk akal: ${feeSell}%"
			}

			if (issues.isEmpty()) {
				KeywordUtil.logInfo("✅ Data Akun '${alias}' DITEMUKAN & VALID (Client: ${clientCode})")
				KeywordUtil.logInfo(
						"[Account] Name=${name}, Alias=${alias}, ClientCode=${clientCode}, " +
						"FeeBuy=${feeBuy}%, FeeSell=${feeSell}%, " +
						"RDN=${bankRDI}, SRE=${invAccNo}, " +
						"CashOnHand=${cashOnHand}, BegOutstanding=${begOutstanding}, " +
						"CurrentOutstanding=${currentOutstanding}"
						)
				return true
			} else {
				KeywordUtil.markFailed("❌ Data Akun '${accountAlias}' DITEMUKAN tetapi TIDAK LENGKAP/VALID")
				issues.each { issue ->
					KeywordUtil.logError("	⚠️ ${issue}")
				}
				KeywordUtil.logError(
						"[Data Aktual] Name=${name}, Alias=${alias}, ClientCode=${clientCode}, " +
						"FeeBuy=${feeBuy}, FeeSell=${feeSell}, " +
						"RDN=${bankRDI}, SRE=${invAccNo}, " +
						"CashOnHand=${cashOnHand}, BegOutstanding=${begOutstanding}, " +
						"CurrentOutstanding=${currentOutstanding}"
						)
				return false
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Error Koneksi/Query DB pada checkAccountDataExists. Pesan Error: " + e.getMessage())
			return false
		} finally {
			if (rs != null) rs.close()
			if (conn != null) conn.close()
		}
	}
	
	
	@Keyword
static int listAllStockFavoriteWithGroupName(String clientCode) {
    Connection conn = null
    ResultSet rs = null

    String sql = """
        SELECT 
            f.STOCK,
            f.GROUPID,
            f.ENTRYDATE AS FAVORITE_ENTRYDATE,
            g.NAME AS GROUP_NAME,
            g.ENTRYDATE AS GROUP_ENTRYDATE
        FROM DBFEED.TB_FO_STOCKFAVORITE f
        LEFT JOIN DBFEED.TB_FO_STOCKFAVORITEGROUP g
            ON f.GROUPID = g.ID
            AND f.USR = g.USR
        WHERE f.USR = ?
        ORDER BY g.NAME, f.ENTRYDATE DESC
    """

    try {
        Class.forName(DB_DRIVER)
        conn = DriverManager.getConnection(DB_URL_feed, DB_USER_feed, DB_PASS_feed)

        def pstmt = conn.prepareStatement(sql.trim())
        pstmt.setString(1, clientCode)
        rs = pstmt.executeQuery()

        List<Map> favoriteList = []

        while (rs.next()) {
            favoriteList.add([
                stock       : rs.getString("STOCK"),
                groupId     : rs.getString("GROUPID"),
                groupName   : rs.getString("GROUP_NAME"),
                entryDate   : rs.getString("FAVORITE_ENTRYDATE")
            ])
        }

        int total = favoriteList.size()

        if (total == 0) {
            KeywordUtil.logInfo("ℹ️ Client ${clientCode} belum memiliki Stock Favorite sama sekali.")
            return 0
        }

        KeywordUtil.logInfo("📋 Daftar Stock Favorite (dengan Nama Group) untuk Client: ${clientCode}")
        KeywordUtil.logInfo("=".multiply(60))

        // Kelompokkan berdasarkan nama group (kasih label kalau null/tidak ada group)
        Map<String, List<Map>> groupedByGroupName = favoriteList.groupBy { 
            it.groupName ?: "Uncategorized/No Group (GroupID: ${it.groupId ?: 'FAVORITE'})" 
        }

        groupedByGroupName.each { groupName, stocks ->
            KeywordUtil.logInfo("🔖 Group: ${groupName} (${stocks.size()} saham)")
            stocks.eachWithIndex { item, idx ->
                KeywordUtil.logInfo("    ${idx + 1}. ${item.stock}  |  Entry: ${item.entryDate}")
            }
        }

        KeywordUtil.logInfo("=".multiply(60))
        KeywordUtil.logInfo("📊 TOTAL KESELURUHAN Stock Favorite: ${total} saham")

        return total

    } catch (Exception e) {
        KeywordUtil.markFailed("❌ Error Koneksi/Query DB pada listAllStockFavoriteWithGroupName. Pesan Error: " + e.getMessage())
        return 0
    } finally {
        if (rs != null) rs.close()
        if (conn != null) conn.close()
    }
}

@Keyword
static boolean verifyStockFavoriteGroupNameExists(String clientCode, String expectedGroupName) {
	Connection conn = null
	ResultSet rs = null

	String sql = """
        SELECT ID, NAME, USR, ENTRYDATE
        FROM DBFEED.TB_FO_STOCKFAVORITEGROUP
        WHERE USR = ?
          AND UPPER(NAME) = UPPER(?)
    """

	try {
		Class.forName(DB_DRIVER)
		conn = DriverManager.getConnection(DB_URL_feed, DB_USER_feed, DB_PASS_feed)

		def pstmt = conn.prepareStatement(sql.trim())
		pstmt.setString(1, clientCode)
		pstmt.setString(2, expectedGroupName)
		rs = pstmt.executeQuery()

		if (rs.next()) {
			String actualId = rs.getString("ID")
			String actualName = rs.getString("NAME")
			String actualEntryDate = rs.getString("ENTRYDATE")

			KeywordUtil.logInfo("✅ Group '${actualName}' DITEMUKAN untuk Client: ${clientCode}")
			KeywordUtil.logInfo("[Group Detail] ID=${actualId}, Name=${actualName}, EntryDate=${actualEntryDate}")
			return true
		} else {
			KeywordUtil.markFailed(
					"❌ Group dengan nama '${expectedGroupName}' TIDAK DITEMUKAN untuk Client: ${clientCode}"
			)
			return false
		}
	} catch (Exception e) {
		KeywordUtil.markFailed("❌ Error Koneksi/Query DB pada verifyStockFavoriteGroupNameExists. Pesan Error: " + e.getMessage())
		return false
	} finally {
		if (rs != null) rs.close()
		if (conn != null) conn.close()
	}
}

@Keyword
static boolean verifyStockFavoriteGroupName(String clientCode, String groupId, String expectedGroupName) {
	Connection conn = null
	ResultSet rs = null

	String sql = """
        SELECT ID, NAME, USR, ENTRYDATE
        FROM DBFEED.TB_FO_STOCKFAVORITEGROUP
        WHERE USR = ?
          AND ID = ?
    """

	try {
		Class.forName(DB_DRIVER)
		conn = DriverManager.getConnection(DB_URL_feed, DB_USER_feed, DB_PASS_feed)

		def pstmt = conn.prepareStatement(sql.trim())
		pstmt.setString(1, clientCode)
		pstmt.setString(2, groupId)
		rs = pstmt.executeQuery()

		if (rs.next()) {
			String actualId = rs.getString("ID")
			String actualName = rs.getString("NAME")
			String actualUsr = rs.getString("USR")
			String actualEntryDate = rs.getString("ENTRYDATE")

			boolean nameMatch = actualName?.trim()?.equalsIgnoreCase(expectedGroupName?.trim())

			if (nameMatch) {
				KeywordUtil.logInfo("✅ Nama Group SESUAI. GroupID: ${actualId}, Name: ${actualName}")
				return true
			} else {
				KeywordUtil.markFailed(
						"❌ Nama Group TIDAK SESUAI. GroupID: ${actualId}, " +
						"Expected: ${expectedGroupName}, Actual: ${actualName}"
				)
				return false
			}
		} else {
			KeywordUtil.markFailed(
					"❌ GroupID: ${groupId} TIDAK DITEMUKAN di TB_FO_STOCKFAVORITEGROUP untuk Client: ${clientCode}"
			)
			return false
		}
	} catch (Exception e) {
		KeywordUtil.markFailed("❌ Error Koneksi/Query DB pada verifyStockFavoriteGroupName. Pesan Error: " + e.getMessage())
		return false
	} finally {
		if (rs != null) rs.close()
		if (conn != null) conn.close()
	}
}

@Keyword
static boolean verifyStockFavoriteGroupDeleted(String clientCode, String groupName) {
	Connection conn = null
	ResultSet rs = null

	String sql = """
        SELECT ID, NAME
        FROM DBFEED.TB_FO_STOCKFAVORITEGROUP
        WHERE USR = ?
          AND UPPER(NAME) = UPPER(?)
    """

	try {
		Class.forName(DB_DRIVER)
		conn = DriverManager.getConnection(DB_URL_feed, DB_USER_feed, DB_PASS_feed)

		def pstmt = conn.prepareStatement(sql.trim())
		pstmt.setString(1, clientCode)
		pstmt.setString(2, groupName)
		rs = pstmt.executeQuery()

		if (!rs.next()) {
			KeywordUtil.logInfo("✅ Group '${groupName}' SUDAH TERHAPUS (tidak ditemukan) untuk Client: ${clientCode}")
			return true
		} else {
			String actualId = rs.getString("ID")
			KeywordUtil.markFailed(
					"❌ Group '${groupName}' MASIH ADA (belum terhapus). ID: ${actualId}, Client: ${clientCode}"
			)
			return false
		}
	} catch (Exception e) {
		KeywordUtil.markFailed("❌ Error Koneksi/Query DB pada verifyStockFavoriteGroupDeleted. Pesan Error: " + e.getMessage())
		return false
	} finally {
		if (rs != null) rs.close()
		if (conn != null) conn.close()
	}
}
	
	@Keyword
static BigDecimal calculateTotalMarketValue(String clientCode) {
    Connection conn = null
    ResultSet rs = null

    String sql = """
        SELECT 
            NVL(SUM(CASE WHEN STATUS = 'CP' THEN NOMINAL ELSE 0 END), 0) -
            NVL(SUM(CASE WHEN STATUS = 'CF' THEN NOMINAL ELSE 0 END), 0) AS TOTAL_MARKET_VALUE
        FROM BNISFO.TB_FO_SBNORDER
        WHERE CLS_INITIALCODE = ?
    """

    try {
        Class.forName(DB_DRIVER)
        conn = DriverManager.getConnection(DB_URL_SFO, DB_USER_SFO, DB_PASS_SFO)

        def pstmt = conn.prepareStatement(sql.trim())
        pstmt.setString(1, clientCode)
        rs = pstmt.executeQuery()

        if (rs.next()) {
            BigDecimal total = rs.getBigDecimal("TOTAL_MARKET_VALUE")
            KeywordUtil.logInfo("💰 Total Market Value SBN (CP - CF) untuk Client ${clientCode}: ${total}")
            return total
        }
        return BigDecimal.ZERO
    } catch (Exception e) {
        KeywordUtil.markFailed("❌ Error menghitung Total Market Value: " + e.getMessage())
        return BigDecimal.ZERO
    } finally {
        if (rs != null) rs.close()
        if (conn != null) conn.close()
    }
}

@Keyword
static boolean verifyTotalMarketValue(String clientCode, BigDecimal expectedTotal) {
    BigDecimal actualTotal = calculateTotalMarketValue(clientCode)
    boolean isMatch = actualTotal.compareTo(expectedTotal) == 0

    if (isMatch) {
        KeywordUtil.logInfo("✅ Total Market Value SESUAI. Expected: ${expectedTotal}, Actual (DB): ${actualTotal}")
    } else {
        BigDecimal selisih = actualTotal.subtract(expectedTotal)
        KeywordUtil.markFailed("❌ Total Market Value TIDAK SESUAI. Expected (UI): ${expectedTotal}, Actual (DB): ${actualTotal}, Selisih: ${selisih}")
    }

    return isMatch
}

	/**
	 * [FUNGSI UNTUK SBN ORDER]
	 * Memverifikasi data order SBN (Surat Berharga Negara) terbaru di TB_FO_SBNORDER.
	 */
	@Keyword
	static boolean verifyLatestSBNOrder(
			String clientCode,
			String expectedSbnId,
			BigDecimal expectedNominal,
			String expectedKodeBiling,
			String expectedPaymentChannel,
			List<String> expectedStatuses
	) {
		Connection conn = null
		ResultSet rs = null

		String sql = """
        SELECT * FROM (
            SELECT * FROM BNISFO.TB_FO_SBNORDER
            WHERE CLS_INITIALCODE = ?
            ORDER BY ENTRYDATE DESC
        ) WHERE ROWNUM <= 1
    """

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL_SFO, DB_USER_SFO, DB_PASS_SFO)

			def pstmt = conn.prepareStatement(sql.trim())
			pstmt.setString(1, clientCode)
			rs = pstmt.executeQuery()

			if (rs.next()) {

				String actualTrxId          = rs.getString("TRXID")
				String actualTrxDate        = rs.getString("TRXDATE")
				String actualSbnId          = rs.getString("SBNID")
				BigDecimal actualNominal    = rs.getBigDecimal("NOMINAL")
				String actualSettleDate     = rs.getString("SETTLEDATE")
				String actualEntryDate      = rs.getString("ENTRYDATE")
				String actualVerifiedDate   = rs.getString("VERIFIEDDATE")
				String actualCompletedDate  = rs.getString("COMPLETEDDATE")
				String actualKodeBiling     = rs.getString("KODEBILING")
				String actualSid            = rs.getString("SID")
				String actualRekSb          = rs.getString("REKSB")
				String actualRekDana        = rs.getString("REKDANA")
				String actualKodePemesanan  = rs.getString("KODEPEMESANAN")
				BigDecimal actualBallNominal = rs.getBigDecimal("BALNOMINAL")
				String actualNtpn           = rs.getString("NTPN")
				String actualNtb            = rs.getString("NTB")
				String actualPaymentChannel = rs.getString("PAYMENTCHANNEL")
				String rawStatus            = rs.getString("STATUS")

				String actualStatus = getSBNOrderStatusDescription(rawStatus)

				// --- Pencocokan Data ---
				boolean sbnIdMatch = expectedSbnId == null ||
						actualSbnId?.equalsIgnoreCase(expectedSbnId)

				boolean nominalMatch = expectedNominal == null ||
						(actualNominal != null && actualNominal.compareTo(expectedNominal) == 0)

				boolean kodeBilingMatch = expectedKodeBiling == null ||
						actualKodeBiling?.equalsIgnoreCase(expectedKodeBiling)

				boolean paymentChannelMatch = expectedPaymentChannel == null ||
						actualPaymentChannel?.equalsIgnoreCase(expectedPaymentChannel)

				boolean statusMatch = expectedStatuses == null || expectedStatuses.isEmpty() ||
						expectedStatuses.contains(actualStatus)

				if (sbnIdMatch && nominalMatch && kodeBilingMatch && paymentChannelMatch && statusMatch) {
					KeywordUtil.logInfo("✅ Verifikasi DB SBN Order BERHASIL untuk Client: ${clientCode}")
					KeywordUtil.logInfo(
							"[SBN Order] TrxID=${actualTrxId}, SBN ID=${actualSbnId}, Nominal=${actualNominal}, " +
							"KodeBiling=${actualKodeBiling}, PaymentChannel=${actualPaymentChannel}, " +
							"Status=${actualStatus} (Raw: ${rawStatus})"
							)
					KeywordUtil.logInfo(
							"	[Detail Tanggal]: TrxDate=${actualTrxDate}, EntryDate=${actualEntryDate}, " +
							"SettleDate=${actualSettleDate}, VerifiedDate=${actualVerifiedDate}, CompletedDate=${actualCompletedDate}"
							)
					KeywordUtil.logInfo(
							"	[Info Tambahan]: SID=${actualSid}, RekSB=${actualRekSb}, RekDana=${actualRekDana}, " +
							"KodePemesanan=${actualKodePemesanan}, BalNominal=${actualBallNominal}, " +
							"NTPN=${actualNtpn}, NTB=${actualNtb}"
							)
					return true
				} else {
					KeywordUtil.markFailed("❌ Verifikasi DB SBN Order GAGAL. Data tidak cocok untuk Client: ${clientCode}")
					KeywordUtil.logError("	TrxID Ditemukan: ${actualTrxId}, Status: ${actualStatus} (Raw: ${rawStatus})")
					KeywordUtil.logError("	Ekspektasi SBN ID: ${expectedSbnId}, Aktual: ${actualSbnId}, Match: ${sbnIdMatch}")
					KeywordUtil.logError("	Ekspektasi Nominal: ${expectedNominal}, Aktual: ${actualNominal}, Match: ${nominalMatch}")
					KeywordUtil.logError("	Ekspektasi KodeBiling: ${expectedKodeBiling}, Aktual: ${actualKodeBiling}, Match: ${kodeBilingMatch}")
					KeywordUtil.logError("	Ekspektasi PaymentChannel: ${expectedPaymentChannel}, Aktual: ${actualPaymentChannel}, Match: ${paymentChannelMatch}")
					KeywordUtil.logError("	Ekspektasi Status (List): ${expectedStatuses}, Aktual: ${actualStatus} (Raw: ${rawStatus}), Match: ${statusMatch}")
					KeywordUtil.logError(
							"	[Info Tambahan]: SID=${actualSid}, RekSB=${actualRekSb}, RekDana=${actualRekDana}, " +
							"KodePemesanan=${actualKodePemesanan}, NTPN=${actualNtpn}, NTB=${actualNtb}"
							)
					return false
				}
			} else {
				KeywordUtil.markFailed("❌ Tidak ada order SBN yang ditemukan di TB_FO_SBNORDER untuk CLS_INITIALCODE: ${clientCode}")
				return false
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Error Koneksi/Query DB pada verifyLatestSBNOrder. Pesan Error: " + e.getMessage())
			return false
		} finally {
			if (rs != null) rs.close()
			if (conn != null) conn.close()
		}
	}

	//	@Keyword
	//	static boolean verifyLatestSBNOrder(
	//			 String clientCode,
	//        BigDecimal expectedNominal
	//	) {
	//		return verifyLatestSBNOrder(
	//				clientCode,
	//				expectedSbnId,
	//				expectedNominal,
	//				null,
	//				null,
	//				null
	//		)
	//	}


	@Keyword
	static boolean verifyLatestSBNOrderSimple(
			String clientCode,
			BigDecimal expectedNominal
	) {
		return verifyLatestSBNOrder(
				clientCode,
				expectedNominal,
				(String) null
				)
	}

	@Keyword
	static boolean verifyLatestSBNOrder(
			String clientCode,
			BigDecimal expectedNominal,
			String expectedSbnId
	) {
		return verifyLatestSBNOrder(
				clientCode,
				expectedSbnId,
				expectedNominal,
				null,
				null,
				null
				)
	}

	@Keyword
	static boolean waitUntilPortfolioDelta(
			String clientCode,
			String stockCode,
			int expectedLot,
			int beforeVolume,
			int timeoutSeconds
	) {
		int waited = 0
		int interval = 3
		int lotSize = 100

		int beforeLot = beforeVolume / lotSize

		while (waited < timeoutSeconds) {

			int afterVolume = getStockVolumeFromPortfolio(clientCode, stockCode)
			int afterLot = afterVolume / lotSize

			int volumeDiff = afterVolume - beforeVolume
			int lotDiff = afterLot - beforeLot

			if (lotDiff == expectedLot) {
				KeywordUtil.logInfo(
						"✅ Portfolio UPDATE | ${stockCode} |\n" +
						"Before = ${beforeVolume} saham (${beforeLot} lot),\n" +
						"After  = ${afterVolume} saham (${afterLot} lot),\n" +
						"${volumeDiff >= 0 ? '+' : ''}${volumeDiff} saham " +
						"(${lotDiff >= 0 ? '+' : ''}${lotDiff} lot)"
						)
				return true
			}

			KeywordUtil.logInfo(
					"⏳ Menunggu portfolio update | ${stockCode} | " +
					"Expected = ${expectedLot} lot, " +
					"Current = ${lotDiff} lot (${waited}/${timeoutSeconds}s)"
					)

			Thread.sleep(interval * 1000)
			waited += interval
		}

		KeywordUtil.markFailed(
				"❌ Portfolio TIDAK UPDATE | ${stockCode} | " +
				"Expected = ${expectedLot} lot, " +
				"Actual = ${(getStockVolumeFromPortfolio(clientCode, stockCode) - beforeVolume) / lotSize} lot"
				)
		return false
	}


	@Keyword
	static def showSampleByDate(String tradeDate) {
		Connection conn = null
		PreparedStatement pstmt = null
		ResultSet rs = null

		String query = """
        SELECT * FROM (
                SELECT 
                    TRADE_DATE,
                    FOREIGN_VOLUME_BUY, FOREIGN_VOLUME_SELL,
                    FOREIGN_VALUE_BUY, FOREIGN_VALUE_SELL,
                    DOMESTIC_VOLUME_BUY, DOMESTIC_VOLUME_SELL,
                    TOTAL_VOLUME, TOTAL_VALUE,
                    LAST_UPDATED
                FROM TB_FT_INVESTORACTIVITY
                WHERE TRADE_DATE = TO_DATE(?,'YYYY-MM-DD')
                ORDER BY LAST_UPDATED DESC
            ) WHERE ROWNUM <= 5
        """

		try {
			conn = getConnection()
			KeywordUtil.logInfo("🚀 Memulai verifikasi data Foreign Summary untuk tanggal: " + tradeDate)
			pstmt = conn.prepareStatement(query)
			pstmt.setString(1, tradeDate)
			rs = pstmt.executeQuery()


			StringBuilder sb = new StringBuilder()
			int count = 0
			Timestamp latestUpdated = null

			while (rs.next()) {
				Date tgl = rs.getDate("TRADE_DATE")
				Timestamp lastUpdated = rs.getTimestamp("LAST_UPDATED")

				// Ambil data Volume untuk pelaporan singkat
				long fVolBuy = rs.getLong("FOREIGN_VOLUME_BUY")
				long fVolSell = rs.getLong("FOREIGN_VOLUME_SELL")
				long fValBuy = rs.getLong("FOREIGN_VALUE_BUY")
				long fValSell = rs.getLong("FOREIGN_VALUE_SELL")
				long fDomBuy = rs.getLong ("DOMESTIC_VOLUME_BUY")
				long fDomSell = rs.getLong ("DOMESTIC_VOLUME_SELL")


				if (latestUpdated == null || (lastUpdated != null && lastUpdated.after(latestUpdated))) {
					latestUpdated = lastUpdated
				}

				sb.append("""
[Data #${count + 1}]
Date        : ${tgl}
F. Vol Buy  : ${fVolBuy}
F. Vol Sell : ${fVolSell}
F. Val Buy  : ${fValBuy}
F. Val Sell : ${fValSell}
F. Dom Buy  : ${fDomBuy}
F. Dom Sell : ${fDomSell}

Last Update : ${lastUpdated}
----------------------------------------
""")
				count++
			}

			if (count == 0) {
				KeywordUtil.markFailed("❌ Tidak ada data di TB_FT_INVESTORACTIVITY untuk tanggal ${tradeDate}")
			} else {
				long totalDataMinimal = 1
				if (count >= totalDataMinimal) {
					KeywordUtil.markPassed("✅ Verifikasi Sukses: Ditemukan " + count + " data untuk tanggal " + tradeDate)
				}

				String logHeader = "🔍 **DETAIL DATA DITEMUKAN** 🔍\n"
				logHeader += "==========================================\n"
				logHeader += "🕒 Update Terakhir : " + latestUpdated + "\n"
				logHeader += "🔢 Total Baris     : " + count + " baris\n"
				logHeader += "------------------------------------------\n"

				String logFooter = "\n=========================================="

				// 3. Cetak ke log
				KeywordUtil.logInfo(logHeader + sb.toString() + logFooter)
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Gagal eksekusi showSampleByDate: " + e.getMessage())
		} finally {
			if (rs != null) rs.close()
			if (pstmt != null) pstmt.close()
			if (conn != null) conn.close()
		}
	}
	private static Connection getConnection() throws Exception {
		Class.forName(DB_DRIVER)
		//return DriverManager.getConnection(DB_URL_HIST, DB_USER_HIST, DB_PASS_HIST)
		return DriverManager.getConnection(DB_URL_HIST, "dbfeed", "sysdev")
	}
	@Keyword
	static int getStockVolumeFromPortfolio(String clientCode, String stockCode) {

		Connection conn = null
		ResultSet rs = null

		String sql = """
       SELECT NVL(SUM(PFO_CURRENTVOLUME), 0) AS VOLUME
        FROM BNISFO.TB_FO_PORTFOLIOS
        WHERE CLS_INITIALCODE = ?
          AND SEC_ID = ?
    """

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			def pstmt = conn.prepareStatement(sql.trim())
			pstmt.setString(1, clientCode)
			pstmt.setString(2, stockCode)

			rs = pstmt.executeQuery()

			if (rs.next()) {
				return rs.getInt("VOLUME")
			}
			return 0
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Error ambil volume portfolio: ${e.message}")
			return 0
		} finally {
			if (rs != null) rs.close()
			if (conn != null) conn.close()
		}
	}

	@Keyword
	static boolean verifyPortfolioLotDelta(
			String clientCode,
			String stockCode,
			int orderLot,
			String side,            // 'B' atau 'S'
			int beforeVolume
	) {

		int afterVolume = getStockVolumeFromPortfolio(clientCode, stockCode)
		int delta = afterVolume - beforeVolume

		int expectedDelta
		if (side.equalsIgnoreCase('B')) {
			expectedDelta = orderLot
		} else if (side.equalsIgnoreCase('S')) {
			expectedDelta = -orderLot
		} else {
			KeywordUtil.markFailed("❌ Side tidak valid: ${side}")
			return false
		}

		if (delta == expectedDelta) {
			KeywordUtil.logInfo(
					"✅ ${side == 'B' ? 'BUY' : 'SELL'} ${stockCode} BERHASIL | " +
					"Client: ${clientCode} | " +
					"Before: ${beforeVolume} | After: ${afterVolume} | Delta: ${delta}"
					)
			return true
		} else {
			KeywordUtil.markFailed(
					"❌ ${side == 'B' ? 'BUY' : 'SELL'} ${stockCode} GAGAL | " +
					"Expected Delta: ${expectedDelta} | Actual: ${delta}"
					)
			return false
		}
	}

	@Keyword
	static boolean isOrderInStatus(
			String clientCode,
			String stockCode,
			String side,
			List<String> statusCodes
	) {
		Connection conn = null
		ResultSet rs = null

		try {
			String dbSide = side.equalsIgnoreCase('B') ? '1' : '2'

			String inClause = statusCodes.collect { "'${it}'" }.join(',')

			String sql = """
            SELECT 1
            FROM BNISFIX.TB_FO_ORDER
            WHERE CLS_INITIALCODE = ?
              AND SEC_ID = ?
              AND ORD_SIDE = ?
              AND ORS_ID IN (${inClause})
              AND ROWNUM = 1
        """

			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			def ps = conn.prepareStatement(sql)
			ps.setString(1, clientCode)
			ps.setString(2, stockCode)
			ps.setString(3, dbSide)

			rs = ps.executeQuery()

			return rs.next()
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Error cek status order: ${e.message}")
			return false
		} finally {
			if (rs != null) rs.close()
			if (conn != null) conn.close()
		}
	}

	@Keyword
	static String getLatestOrderStatus(
			String clientCode,
			String stockCode,
			String side
	) {
		Connection conn = null
		ResultSet rs = null

		String sql = """
        SELECT ORS_ID
        FROM (
            SELECT ORS_ID
            FROM BNISFIX.TB_FO_ORDER
            WHERE CLS_INITIALCODE = ?
              AND SEC_ID = ?
              AND ORD_SIDE = ?
            ORDER BY ORS_REQENTDT DESC
        )
        WHERE ROWNUM = 1
    """

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			def ps = conn.prepareStatement(sql.trim())
			ps.setString(1, clientCode)
			ps.setString(2, stockCode)
			ps.setString(3, side.equalsIgnoreCase('B') ? '1' : '2')

			rs = ps.executeQuery()

			if (rs.next()) {
				return getOrderStatusDescription(rs.getString("ORS_ID"))
			}
			return null
		} finally {
			if (rs != null) rs.close()
			if (conn != null) conn.close()
		}
	}

	@Keyword
	static boolean waitUntilOrderExecuted(
			String clientCode,
			String stockCode,
			String side,
			int timeoutSeconds
	) {
		int waited = 0
		int interval = 3

		List<String> stopStatus = [
			'Reject',
			'Withdraw (Cancelled)',
			'Cancelled'
		]

		while (waited < timeoutSeconds) {

			String currentStatus =
					getLatestOrderStatus(clientCode, stockCode, side)

			if (currentStatus == null) {
				KeywordUtil.logInfo("⏳ Order ${stockCode} belum tercatat")
			}

			// ❌ STOP LANGSUNG JIKA GAGAL
			else if (stopStatus.contains(currentStatus)) {
				KeywordUtil.markFailed(
						"❌ Order ${stockCode} GAGAL dengan status ${currentStatus} — Test dihentikan"
						)
				return false
			}

			// ✅ SUKSES
			else if (currentStatus == 'Match (Executed)') {
				KeywordUtil.logInfo(
						"✅ Order ${stockCode} MATCH (Executed)"
						)
				return true
			}

			// ⏳ MASIH PROSES
			KeywordUtil.logInfo(
					"⏳ Menunggu order ${stockCode} status ${currentStatus} " +
					"(${waited}/${timeoutSeconds} detik)"
					)

			Thread.sleep(interval * 1000)
			waited += interval
		}

		KeywordUtil.markFailed(
				"❌ Order ${stockCode} TIDAK MATCH dalam ${timeoutSeconds} detik"
				)
		return false
	}




	/**
	 * [FUNGSI UTAMA UNTUK KRITERIA/AUTO ORDER]
	 * Memverifikasi data auto order terbaru di TB_FO_CRITERIAORDER dan memastikan
	 * order BUKAN order yang terkirim di TB_FO_ORDER.
	 */
	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyLatestOrder(String clientCode, String expectedStockCode, int expectedLot, BigDecimal expectedPrice, String DB_DRIVER, String DB_URL, String DB_USER, String DB_PASS) {
		Connection conn = null
		ResultSet rsCriteria = null
		ResultSet rsOrderCheck = null

		// Query 1: Memeriksa data terbaru di TB_FO_CRITERIAORDER
		String sqlCriteria = """
        SELECT CRO_ID, CRO_CRITERIASYMBOL, CRO_LOT, CRO_PRICE, CRO_STATUS 
        FROM (
            SELECT CRO_ID, CRO_CRITERIASYMBOL, CRO_LOT, CRO_PRICE, CRO_STATUS, CRO_REQENTDT 
            FROM BNISFIX.TB_FO_CRITERIAORDER
            WHERE USR_ID = ?
            ORDER BY CRO_REQENTDT DESC
        ) 
        WHERE ROWNUM <= 1
    """

		// Query 2: Memeriksa apakah order DENGAN ID SPESIFIK INI sudah terkirim ke TB_FO_ORDER.
		// Jika record di TB_FO_CRITERIAORDER belum terkirim, seharusnya tidak ada record di TB_FO_ORDER yang memiliki CRO_ID tersebut.
		String sqlOrderCheck = """
        SELECT 1
        FROM BNISFIX.TB_FO_ORDER
        WHERE CRO_ID = ?
        AND ROWNUM <= 1
    """

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			// --- 1. Cek TB_FO_CRITERIAORDER ---
			def pstmtCriteria = conn.prepareStatement(sqlCriteria.trim())
			pstmtCriteria.setString(1, clientCode)
			rsCriteria = pstmtCriteria.executeQuery()

			if (rsCriteria.next()) {
				String actualOrderId = rsCriteria.getString("CRO_ID")
				String actualStockCode = rsCriteria.getString("CRO_CRITERIASYMBOL")
				int actualLot = rsCriteria.getInt("CRO_LOT")
				BigDecimal actualPrice = rsCriteria.getBigDecimal("CRO_PRICE")
				String rawStatus = rsCriteria.getString("CRO_STATUS")

				// Asumsi getCriteriaStatusDescription() didefinisikan di tempat lain
				// String actualStatus = getCriteriaStatusDescription(rawStatus)
				String actualStatus = rawStatus

				boolean stockMatch = actualStockCode.equalsIgnoreCase(expectedStockCode)
				boolean lotMatch = actualLot == expectedLot
				boolean priceMatch = actualPrice.compareTo(expectedPrice) == 0

				// --- 2. Cek TB_FO_ORDER menggunakan CRO_ID SPESIFIK ---
				def pstmtOrderCheck = conn.prepareStatement(sqlOrderCheck.trim())
				pstmtOrderCheck.setString(1, actualOrderId)
				rsOrderCheck = pstmtOrderCheck.executeQuery()

				boolean orderTerkirim = rsOrderCheck.next() // True jika record order kriteria ini ditemukan di Order Book

				// --- 3. Verifikasi Akhir ---
				if (stockMatch && lotMatch && priceMatch && !orderTerkirim) {
					// Kriteria cocok DAN Order SPESIFIK ini belum terkirim
					KeywordUtil.logInfo("✅ Verifikasi DB Berhasil: Data kriteria cocok dan order belum terkirim ke Order Book.")
					KeywordUtil.logInfo("	[Kriteria Order]: ID: ${actualOrderId}, Status: ${actualStatus}, Saham: ${actualStockCode}, Lot: ${actualLot}, Harga: ${actualPrice}")
					return true
				} else if (orderTerkirim) {
					// Kriteria cocok TAPI Order SPESIFIK ini SUDAH terkirim
					KeywordUtil.markFailed("❌ Verifikasi GAGAL: Kriteria Order (ID: ${actualOrderId}) ditemukan, tetapi Order terkait sudah ditemukan di Order Book utama. Seharusnya belum terkirim.")
					return false
				} else {
					// Kriteria tidak cocok
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
			// Logging error lebih detail untuk debugging
			KeywordUtil.markFailed("❌ Error Koneksi/Query DB. Pesan Error: " + e.getMessage())
			return false
		} finally {
			if (rsCriteria != null) rsCriteria.close()
			if (rsOrderCheck != null) rsOrderCheck.close() // Ganti dari rsOrder ke rsOrderCheck
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

	/**
	 * [FUNGSI BARU UNTUK VARIED SPLIT ORDER]
	 * Memverifikasi N order terbaru, memastikan setiap order memiliki lot yang berbeda
	 * sesuai dengan urutan yang diekspektasikan dalam List<Integer> expectedSplitLots.
	 */
	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyLatestVariedSplitOrders(
			String clientCode,
			String expectedStockCode,
			List<Integer> expectedSplitLots,
			BigDecimal expectedPrice,
			List<String> expectedStatuses,
			String expectedSide,
			List<String> expectedBoardID
	) {

		Connection conn = null
		ResultSet rsOrders = null
		boolean overallResult = true

		int expectedSplitCount = expectedSplitLots.size()

		KeywordUtil.logInfo(
				"🔍 Verifikasi Varied Split Order | Client=${clientCode}, Stock=${expectedStockCode}, Split=${expectedSplitCount}"
				)

		// === SIDE MAPPING ===
		String dbSideCode
		if (expectedSide.equalsIgnoreCase('B')) {
			dbSideCode = '1'
		} else if (expectedSide.equalsIgnoreCase('S')) {
			dbSideCode = '2'
		} else {
			KeywordUtil.markFailed("❌ Side tidak valid: ${expectedSide}")
			return false
		}

		// === QUERY ===
		String sql = """
        SELECT * FROM (
            SELECT
                ORD_ID,
                SEC_ID,
                ORD_LOT,
                ORD_PRICE,
                ORD_SIDE,
                ORS_ID,
                BRD_ID
            FROM BNISFIX.TB_FO_ORDER
            WHERE CLS_INITIALCODE = ?
              AND ORD_SIDE = ?
            ORDER BY ORS_REQENTDT DESC
        )
        WHERE ROWNUM <= ?
    """

		List<Map> actualOrders = []

		try {
			Class.forName(DB_DRIVER)
			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)

			def pstmt = conn.prepareStatement(sql.trim())
			pstmt.setString(1, clientCode)
			pstmt.setString(2, dbSideCode)
			pstmt.setInt(3, expectedSplitCount)

			rsOrders = pstmt.executeQuery()

			while (rsOrders.next()) {
				actualOrders.add([
					ordId     : rsOrders.getString("ORD_ID"),
					stockCode : rsOrders.getString("SEC_ID"),
					lotAmount : rsOrders.getInt("ORD_LOT"),
					price     : rsOrders.getBigDecimal("ORD_PRICE"),
					rawStatus : rsOrders.getString("ORS_ID"),
					boardID   : rsOrders.getString("BRD_ID")?.toUpperCase(),
					side      : rsOrders.getString("ORD_SIDE") == '1' ? 'BUY' : 'SELL'
				])
			}

			// === VALIDASI JUMLAH ORDER ===
			if (actualOrders.size() != expectedSplitCount) {
				KeywordUtil.markFailed(
						"❌ Jumlah order tidak sesuai. Expected=${expectedSplitCount}, Actual=${actualOrders.size()}"
						)
				return false
			}

			// =====================================================
			// 🔑 VALIDASI LOT (SYSTEM GENERATED / RANDOM)
			// =====================================================
			List<Integer> actualLots = actualOrders.collect { it.lotAmount }

			KeywordUtil.logInfo("📊 LOT AKTUAL (ENGINE GENERATED): ${actualLots}")

			// aturan minimal: semua lot harus > 0
			boolean lotValid = actualLots.every { it > 0 }

			if (!lotValid) {
				KeywordUtil.markFailed(
						"❌ LOT INVALID: ditemukan lot <= 0\nActual: ${actualLots}"
						)
				return false
			}

			// =====================================================
			// VALIDASI ATRIBUT LAIN (NON-LOT)
			// =====================================================
			actualOrders.eachWithIndex { actual, idx ->

				String actualStatusDesc = getOrderStatusDescription(actual.rawStatus)

				boolean stockMatch =
						actual.stockCode?.equalsIgnoreCase(expectedStockCode)

				boolean priceMatch =
						expectedPrice == null ||
						(actual.price != null && actual.price.compareTo(expectedPrice) == 0)

				boolean sideMatch =
						(expectedSide == 'B' && actual.side == 'BUY') ||
						(expectedSide == 'S' && actual.side == 'SELL')

				boolean statusMatch =
						expectedStatuses.contains(actualStatusDesc)

				boolean boardMatch =
						actual.boardID != null && expectedBoardID.contains(actual.boardID)

				if (stockMatch && priceMatch && sideMatch && statusMatch && boardMatch) {
					KeywordUtil.logInfo(
							"✅ Order #${idx + 1} OK | ID=${actual.ordId}, Lot=${actual.lotAmount}, Status=${actualStatusDesc}"
							)
				} else {
					KeywordUtil.markFailed(
							"❌ Order #${idx + 1} GAGAL\n" +
							"StockMatch=${stockMatch}, PriceMatch=${priceMatch}, SideMatch=${sideMatch},\n" +
							"Status=${actualStatusDesc}, Board=${actual.boardID}"
							)
					overallResult = false
				}
			}

			if (overallResult) {
				KeywordUtil.logInfo("🎉 SEMUA SPLIT ORDER TERVERIFIKASI DENGAN BENAR")
			}
		} catch (Exception e) {
			KeywordUtil.markFailed("❌ Error DB Verification: ${e.message}")
			overallResult = false
		} finally {
			if (rsOrders != null) rsOrders.close()
			if (conn != null) conn.close()
		}

		return overallResult
	}

	@Keyword
	static boolean verifyLatestSplitOrdersByCount(
			String clientCode,
			String expectedStockCode,
			int expectedCount,
			BigDecimal expectedPrice,
			List<String> expectedStatuses,
			String expectedSide,
			List<String> expectedBoardID
	) {
		List<Integer> dummyLots =
				(1..expectedCount).collect { -1 }

		return verifyLatestVariedSplitOrders(
				clientCode,
				expectedStockCode,
				dummyLots,
				expectedPrice,
				expectedStatuses,
				expectedSide,
				expectedBoardID
				)
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

				int matchIndex = -1

				for (int j = 0; j < actualOrders.size(); j++) {
					Map actual = actualOrders.get(j)

					if (actual.matched) continue

						boolean stockMatch = actual.stockCode.equalsIgnoreCase(expectedStockCode)
					boolean lotMatch = (expectedLot < 0) || (actual.lotAmount == expectedLot)
					boolean priceMatch = actual.price.compareTo(expectedPrice) == 0
					boolean sideMatch = actual.side.equalsIgnoreCase(expectedSide)

					boolean statusMatch = actual.statusDesc != null && expectedStatuses.contains(actual.statusDesc)

					boolean boardIDMatch = actual.boardID != null && expectedBoardIDs.contains(actual.boardID)

					if (stockMatch && lotMatch && priceMatch && sideMatch && statusMatch && boardIDMatch) {
						matchIndex = j

						actual.matched = true
						KeywordUtil.logInfo("	✅ Match Berhasil dengan Order DB ID: ${actual.ordId}, Status: ${actual.statusDesc}, Board: ${actual.boardID}")
						break
					}
				}

				if (matchIndex == -1) {
					KeywordUtil.markFailed("❌ Verifikasi Order #${i + 1} GAGAL: Tidak ada order aktual yang cocok dengan ekspektasi.")
					KeywordUtil.logError("	Ekspektasi: Saham: ${expectedStockCode}, Side: ${expectedSide}, Lot: ${expectedLot}, Harga: ${expectedPrice}, Status: ${expectedStatuses}, Board: ${expectedBoardIDs}")
					overallResult = false
				}
			}

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

	@com.kms.katalon.core.annotation.Keyword
	static List<Integer> calculateEqualSplitLots(int totalLot, int splitCount) {
		if (splitCount <= 0 || totalLot <= 0) {
			KeywordUtil.logError("Lot atau Split Count tidak valid.")
			return new ArrayList<Integer>()
		}

		List<Integer> splitLots = new ArrayList<Integer>()
		int baseLot = totalLot / splitCount
		int remainder = totalLot % splitCount

		for (int i = 0; i < splitCount; i++) {
			int currentLot = baseLot
			if (i < remainder) {
				currentLot += 1
			}
			splitLots.add(currentLot)
		}
		return splitLots
	}


	/**
	 * [FUNGSI UTAMA UNTUK BOND TRANSACTION]
	 * Memverifikasi data transaksi Obligasi/Bond terbaru di TB_FO_BONDTRANSACTION.
	 * Memerlukan verifikasi nama kolom di DB.
	 */
	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyLatestBondTransaction(String clientCode, String expectedBondCode, BigDecimal expectedNominal, BigDecimal expectedPrice, List<String> expectedStatuses, String expectedEstampDuty, BigDecimal expectedTotalPayment) {
    Connection conn = null
    ResultSet rsBond = null

    // **KOREKSI 2: Menghilangkan Karakter Ilegal di akhir baris TB_FO_BONDTRANSACTION**
    String sqlBond = """
    SELECT * FROM (
        SELECT * FROM BNISFO.TB_FO_BONDTRANSACTION
        WHERE USR_ID = ?
        ORDER BY TRXID DESC
    ) WHERE ROWNUM <= 1
"""

    try {
        Class.forName(DB_DRIVER)

        conn = DriverManager.getConnection(DB_URL_SFO, DB_USER_SFO, DB_PASS_SFO)
        def pstmtBond = conn.prepareStatement(sqlBond.trim())
        pstmtBond.setString(1, clientCode)

        rsBond = pstmtBond.executeQuery()

        if (rsBond.next()) {

            String actualTransactionId = rsBond.getString("TRXID")
            String actualBondCode = rsBond.getString("BONDID")
            BigDecimal actualNominal = rsBond.getBigDecimal("NOMINAL")
            BigDecimal actualPrice = rsBond.getBigDecimal("PRICE")
            String actualTrxDate = rsBond.getString("TRXDATE")
            String rawStatus = rsBond.getString("STATUS")
            String actualRejectReason = rsBond.getString("REJECT_DESC")
            String actualEstampDuty = rsBond.getString("ESTAMP_DUTY")
            BigDecimal actualTotalPayment = rsBond.getBigDecimal("TOTAL_PAYMENT")

            // Konversi Status Bond
            String actualStatus = getBondTransactionStatusDescription(rawStatus)

            // --- Logika Pencocokan Data Bond Transaksi ---
            boolean bondCodeMatch = actualBondCode.equalsIgnoreCase(expectedBondCode)
            boolean nominalMatch = actualNominal.compareTo(expectedNominal) == 0
            boolean priceMatch   = actualPrice.compareTo(expectedPrice) == 0
            boolean statusMatch  = expectedStatuses.contains(actualStatus)
            boolean estampDutyMatch = true
            boolean totalPaymentMatch = true

            if (expectedEstampDuty != null) {
                estampDutyMatch = actualEstampDuty?.equalsIgnoreCase(expectedEstampDuty)
            }

            if (expectedTotalPayment != null) {
                totalPaymentMatch = actualTotalPayment.compareTo(expectedTotalPayment) == 0
            }

            if (bondCodeMatch && nominalMatch && priceMatch &&
                    statusMatch && estampDutyMatch && totalPaymentMatch) {

                KeywordUtil.logInfo("✅ Verifikasi DB Bond Transaksi BERHASIL (SEMUA FIELD MATCH)")
                KeywordUtil.logInfo(
                        "[Bond] ID=${actualTransactionId}, Code=${actualBondCode}, " +
                        "Nominal=${actualNominal}, Price=${actualPrice}, " +
                        "Status=${actualStatus}, EstampDuty=${actualEstampDuty}, " +
                        "TotalPayment=${actualTotalPayment}"
                )
                return true
            } else {
                KeywordUtil.logInfo("❌ Verifikasi DB Bond Transaksi GAGAL. Data tidak cocok.")
                KeywordUtil.logInfo("	Transaksi ID Ditemukan: ${actualTransactionId}")
                KeywordUtil.logInfo("	Ekspektasi Bond Code: ${expectedBondCode}, Aktual: ${actualBondCode}, Match: ${bondCodeMatch}")
                KeywordUtil.logInfo("	Ekspektasi Nominal: ${expectedNominal}, Aktual: ${actualNominal}, Match: ${nominalMatch}")
                KeywordUtil.logInfo("	Ekspektasi Price: ${expectedPrice}, Aktual: ${actualPrice}, Match: ${priceMatch}")
                KeywordUtil.logInfo("	Ekspektasi Status (List): ${expectedStatuses}, Aktual: ${actualStatus} (Raw: ${rawStatus}), Match: ${statusMatch}")
                KeywordUtil.logInfo("	Ekspektasi EstampDuty: ${expectedEstampDuty}, Aktual: ${actualEstampDuty}, Match: ${estampDutyMatch}")
                KeywordUtil.logInfo("	Ekspektasi TotalPayment: ${expectedTotalPayment}, Aktual: ${actualTotalPayment}, Match: ${totalPaymentMatch}")
                KeywordUtil.logInfo("	[Info Tambahan]: TRX Date: ${actualTrxDate}, Reject Reason: ${actualRejectReason}")
                markFailed("❌ Verifikasi DB Bond Transaksi GAGAL. Data tidak cocok.")
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

	@com.kms.katalon.core.annotation.Keyword
	static boolean verifyLatestMutualFundOrder(String clientCode, String expectedFundCode, BigDecimal expectedAmount, String expectedTrxType, List<String> expectedStatuses) {
		Connection conn = null
		ResultSet rsMF = null

		// Query untuk mengambil 1 transaksi Reksa Dana terbaru
		// **KOREKSI 3: Menghilangkan Karakter Ilegal di akhir baris TB_MF_FUNDORDER**
		String sqlMF = """
			SELECT * FROM (
				SELECT * FROM BNISBO.TB_MF_FUNDORDER
				WHERE CLS_INITIALCODE = ?
				ORDER BY ORD_DATE DESC
			) WHERE ROWNUM <= 1
		"""

		try {
			Class.forName(DB_DRIVER)
			// Menggunakan koneksi alternatif BNISFO/BNISBO
			conn = DriverManager.getConnection(DB_URL_SFO, DB_USER_SFO, DB_PASS_SFO)
			def pstmtMF = conn.prepareStatement(sqlMF.trim())
			pstmtMF.setString(1, clientCode)

			rsMF = pstmtMF.executeQuery()

			if (rsMF.next()) {

				// Ambil data aktual dari database
				String actualMFID = rsMF.getString("ORDID")
				String actualFundCode = rsMF.getString("FUNDID")
				BigDecimal actualAmount = rsMF.getBigDecimal("REQVALUE_SUBS") // Nominal transaksi
				String actualTrxType = rsMF.getString("OTY_ID") // Kode Tipe Transaksi
				String actualOrderDate = rsMF.getString("ORD_DATE")
				String rawStatus = rsMF.getString("STTSCD") // Kode Status

				// Konversi Kode Tipe Transaksi (Asumsi: 1=Beli (Subsc), 2=Jual (Redeem), 3=Switch)
				String actualTrxTypeDesc = (actualTrxType == '1' ? 'SUBSCRIPTION (Beli)' : (actualTrxType == '2' ? 'REDEMPTION (Jual)' : (actualTrxType == '3' ? 'SWITCH' : "Unknown (${actualTrxType})")))

				// Konversi Kode Status Reksa Dana
				String actualStatus = getMutualFundStatusDescription(rawStatus)


				// --- Logika Pencocokan Data Reksa Dana ---
				boolean fundCodeMatch = actualFundCode.equalsIgnoreCase(expectedFundCode)
				boolean amountMatch = actualAmount.compareTo(expectedAmount) == 0
				boolean trxTypeMatch = actualTrxType.equalsIgnoreCase(expectedTrxType) // Ekspektasi berupa kode (1, 2, atau 3)

				// Verifikasi status order: cek apakah status aktual ada di dalam List status yang diekspektasi
				boolean statusMatch = expectedStatuses.contains(actualStatus)

				if (fundCodeMatch && amountMatch && trxTypeMatch && statusMatch) {
					KeywordUtil.logInfo("✅ Verifikasi DB Reksa Dana Berhasil (FUND, AMOUNT, TYPE, STATUS).")
					KeywordUtil.logInfo("	[Detail MF Order]: ID: ${actualMFID}, Fund Code: ${actualFundCode}, Amount: ${actualAmount}, Type: ${actualTrxTypeDesc}, Status: ${actualStatus} (Raw: ${rawStatus})")
					KeywordUtil.logInfo("	[Info Tambahan]: Order Date: ${actualOrderDate}")
					return true
				} else {
					KeywordUtil.markFailed("❌ Verifikasi DB Reksa Dana GAGAL. Data tidak cocok.")
					KeywordUtil.logError("	Transaksi ID Ditemukan: ${actualMFID}")
					KeywordUtil.logError("	Ekspektasi Fund Code: ${expectedFundCode}, Aktual: ${actualFundCode}")
					KeywordUtil.logError("	Ekspektasi Amount: ${expectedAmount}, Aktual: ${actualAmount}")
					KeywordUtil.logError("	Ekspektasi Tipe Transaksi (Kode): ${expectedTrxType}, Aktual: ${actualTrxTypeDesc} (Raw: ${actualTrxType})")
					KeywordUtil.logError("	Ekspektasi Status (List): ${expectedStatuses}, Aktual: ${actualStatus} (Raw: ${rawStatus})")
					return false
				}
			} else {
				KeywordUtil.markFailed("❌ Tidak ada transaksi Reksa Dana yang ditemukan di TB_MF_FUNDORDER untuk USRID: ${clientCode}")
				return false
			}
		} catch (Exception e) {

			KeywordUtil.markFailed("❌ Error Koneksi/Query DB untuk Mutual Fund. Pesan Error: " + e.getMessage())
			return false
		} finally {
			if (rsMF != null) rsMF.close()
			if (conn != null) conn.close()
		}
	}
}