import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import java.time.ZonedDateTime as ZonedDateTime
import java.time.ZoneId as ZoneId
import java.time.format.DateTimeFormatter as DateTimeFormatter
import java.time.Instant as Instant
import java.time.Duration as Duration
import com.utilities.TradingHours as TradingHours
import com.utilities.ShimmerWait as ShimmerWait
import groovy.json.JsonSlurper as JsonSlurper
import com.utilities.BionsSocketClient

// ============================================================
// KONFIGURASI
// ============================================================
String applicationID = 'id.bions.bnis.android.new_bions_revamp'
String screenshotBasePath = '/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login'

String userId = GlobalVariable.G_userId
String password = GlobalVariable.G_password
String pin = GlobalVariable.G_pin
String host = GlobalVariable.G_tradinghost
int feedPort = GlobalVariable.G_feedport as int
int tradingPort = GlobalVariable.G_tradingport as int
String clientIp = GlobalVariable.G_clientip

List<String> symbols = ['RAJARG', 'BRMSRG', 'SUPARG', 'DADARG','GOTO']
int liveMonitorSeconds = 10

// ============================================================
// STEP 1: LAUNCH APPLICATION
// ============================================================
try {
	Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)
	KeywordUtil.logInfo("Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
} catch (Exception e) {
	KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage(),
			FailureHandling.STOP_ON_FAILURE)
}

// ============================================================
// STEP 2: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)
Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)
Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login0.PNG")

Instant start = Instant.now()
Mobile.tap(findTestObject('Login_V2/login'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login1.PNG")

Instant end = Instant.now()
double loginSeconds = (Duration.between(start, end).toMillis() / 1000.0)
KeywordUtil.logInfo("Waktu login sampai dashboard: ${loginSeconds} detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))
def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

Mobile.takeScreenshot("${screenshotBasePath}/Login_Biometric.PNG")
Mobile.tap(findTestObject('Login_firebase/Not_now'), 0)

// ============================================================
// STEP 3: NAVIGASI KE HALAMAN PORTFOLIO (SEMUA VERIFIKASI UI DI SINI)
// ============================================================
Mobile.tap(findTestObject('NavBar_Scalability/Portofolio'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login1_V2.PNG")

Mobile.swipe(500, 1500, 500, 500)
Mobile.delay(3, FailureHandling.STOP_ON_FAILURE)
Mobile.takeScreenshot("${screenshotBasePath}/Login1_V3.PNG")

// CATATAN: Semua kebutuhan verifikasi UI (screenshot, tap, swipe) HARUS
// sudah selesai di titik ini. Setelah socket login di bawah dijalankan,
// sesi UI mobile kemungkinan akan ter-logout otomatis (server menerapkan
// kebijakan single-session per user).

// ============================================================
// STEP 4: SOCKET LOGIN (FEED) - Dilakukan PALING AKHIR
// ============================================================
BionsSocketClient feedClient = new BionsSocketClient()

Map feedResult = feedClient.loginFeed(
		host,
		feedPort,
		userId,
		password,
		clientIp
)

KeywordUtil.logInfo("Feed Login berhasil! Gateway: ${feedResult.gatewayId}")
feedClient.receivedMessages.each { msg ->
	KeywordUtil.logInfo("Feed Response Diterima: ${msg}")
}


// ============================================================
// STEP 5: SOCKET LOGIN (TRADING) + AMBIL DATA PORTFOLIO
// ============================================================
BionsSocketClient tradingClient = new BionsSocketClient()

Map tradingResult = tradingClient.loginTrading(
		host,
		tradingPort,
		userId,
		password,
		pin,
		clientIp
)

KeywordUtil.logInfo("Trading Login berhasil! Tag58: ${tradingResult.tag58Value}")

KeywordUtil.logInfo("=== PORTFOLIO SEBELUM MONITORING ===")
List<List> initialPortfolio = tradingClient.getPortfolioStockSnapshot(userId)
initialPortfolio.each { row -> KeywordUtil.logInfo("  ${row}") }

KeywordUtil.logInfo("=== MEMANTAU SELAMA 10 DETIK ===")
List<Map> updates = tradingClient.watchPortfolioRealtime(userId, 10)

updates.each { update ->
	KeywordUtil.logInfo("Trigger: FIX 35=${update.trigger}")
	update.portfolio.each { row -> KeywordUtil.logInfo("  ${row}") }
}

KeywordUtil.logInfo("=== PORTFOLIO SETELAH MONITORING (untuk verifikasi final) ===")
List<List> finalPortfolio = tradingClient.getPortfolioStockSnapshot(userId)   // rename biar lebih jelas maksudnya
finalPortfolio.each { row -> KeywordUtil.logInfo("  ${row}") }

// Bandingkan
if (initialPortfolio.size() != finalPortfolio.size()) {
	KeywordUtil.logInfo("✅ Ada perubahan jumlah saham di Portfolio (${initialPortfolio.size()} → ${finalPortfolio.size()})")
} else {
	KeywordUtil.logInfo("ℹ️ Jumlah saham di Portfolio tidak berubah")
}

KeywordUtil.logInfo("=== LIVE UPDATE (${liveMonitorSeconds} detik) ===")

Map<String, String> subscriptions = feedClient.subscribeMultipleStockQuotes(symbols)
feedClient.listen(liveMonitorSeconds)
Map<String, List> allUpdates = feedClient.parseAllLiveQuoteUpdates()

allUpdates.each { stockCode, quotes ->
	quotes.each { quote ->
		KeywordUtil.logInfo("${stockCode}: Rp${BionsSocketClient.formatPrice(quote.displayLast)} (${quote.changePct}%)")
	}
}

List<String> noMovement = BionsSocketClient.getStocksWithNoMovement(symbols, allUpdates)
if (!noMovement.isEmpty()) {
	KeywordUtil.logInfo("ℹ️ Saham tanpa pergerakan (wajar kalau market tutup): ${noMovement}")
}

feedClient.unsubscribeMultipleStockQuotes(subscriptions)

feedClient.closeSocket()
tradingClient.closeSocket()


// ============================================================
// STEP 6: CLOSE APPLICATION
// ============================================================
// Sesi UI kemungkinan sudah ter-logout akibat socket login di atas,
// tapi ini tidak masalah karena semua screenshot/verifikasi UI
// sudah selesai dilakukan sebelumnya di STEP 3.
Mobile.closeApplication()