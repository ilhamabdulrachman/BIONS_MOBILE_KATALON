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
import com.utilities.BionsSocketClient as BionsSocketClient

// ============================================================
// KONFIGURASI
// ============================================================
String applicationID = 'id.bions.bnis.android.new_bions_revamp'

String screenshotBasePath = '/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login'

String userId = GlobalVariable.G_userId

String password = GlobalVariable.G_password

String pin = GlobalVariable.G_pin

String host = GlobalVariable.G_tradinghost

int feedPort = GlobalVariable.G_feedport.toInteger()

int tradingPort = GlobalVariable.G_tradingport.toInteger()

String clientIp = GlobalVariable.G_clientip

// ============================================================
// STEP 1: LAUNCH APPLICATION
// ============================================================
try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

    KeywordUtil.logInfo("Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
    KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage(), 
        FailureHandling.STOP_ON_FAILURE)
} 

// ============================================================
// STEP 2: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)

Mobile.takeScreenshot("$screenshotBasePath/Login0.PNG")

Instant start = Instant.now()

Mobile.tap(findTestObject('Login_V2/login'), 0)

Mobile.takeScreenshot("$screenshotBasePath/Login1.PNG")

Instant end = Instant.now()

double loginSeconds = Duration.between(start, end).toMillis() / 1000.0

KeywordUtil.logInfo("Waktu login sampai dashboard: $loginSeconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

Mobile.takeScreenshot("$screenshotBasePath/Login_Biometric.PNG")

Mobile.tap(findTestObject('Login_firebase/Not_now'), 0)

Mobile.tap(findTestObject('SeeAll/seeall'), 0)

Mobile.tap(findTestObject('SeeAll/Running_Trade'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/runingtrade.PNG')

Mobile.delay(3, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/runingtrade2.PNG')

Mobile.delay(3, FailureHandling.STOP_ON_FAILURE)



BionsSocketClient feedClient = new BionsSocketClient()
feedClient.loginFeed(host, feedPort, userId, password, clientIp)

// ===== SESI 1: SEBELUM PAUSE =====
feedClient.subscribeRunningTrade()
feedClient.listen(15)

List<Map> allTradesBeforePause = feedClient.parseAllRunningTrades()
KeywordUtil.logInfo("=== SESI 1 (Sebelum Pause): ${allTradesBeforePause.size()} transaksi ===")
allTradesBeforePause.each { trade ->
    KeywordUtil.logInfo("[${trade.time}] ${trade.stockCode}${trade.boardCode}: Rp${BionsSocketClient.formatPrice(trade.price)} x ${trade.lot} lot")
}

feedClient.unsubscribeRunningTrade()

// ===== TAP PAUSE DI UI =====
Mobile.tap(findTestObject('SeeAll/Pause'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/runingtrade3.PNG')

Mobile.delay(3, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/runingtrade4.PNG')
Mobile.tap(findTestObject('SeeAll/Pause'), 0) 

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/runingtrade5.PNG')

// ===== SESI 2: SETELAH RESUME (pakai feedClient yang SAMA, tidak buat baru) =====
feedClient.subscribeRunningTrade()
feedClient.listen(15)

List<Map> allTradesAfterResume = feedClient.parseAllRunningTrades()
KeywordUtil.logInfo("=== SESI 2 (Setelah Resume): ${allTradesAfterResume.size()} transaksi ===")
allTradesAfterResume.each { trade ->
    KeywordUtil.logInfo("[${trade.time}] ${trade.stockCode}${trade.boardCode}: Rp${BionsSocketClient.formatPrice(trade.price)} x ${trade.lot} lot")
}

feedClient.unsubscribeRunningTrade()
feedClient.closeSocket()

