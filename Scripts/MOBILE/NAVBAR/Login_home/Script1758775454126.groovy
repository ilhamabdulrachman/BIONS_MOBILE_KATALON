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
import com.utilities.TcpClient as TcpClient
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import java.time.ZonedDateTime as ZonedDateTime
import java.time.ZoneId as ZoneId
import java.time.format.DateTimeFormatter as DateTimeFormatter
import java.time.Instant as Instant
import java.time.Duration as Duration
import com.utilities.TradingHours as TradingHours
import com.utilities.ShimmerWait as ShimmerWait
import groovy.json.JsonSlurper as JsonSlurper

boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()

if (isMarketOpen) {
    KeywordUtil.logInfo('Bursa sedang buka. Melanjutkan pengujian...')
} else {
    boolean isMarketBreak = CustomKeywords.'com.utilities.TradingHours.isMarketBreak'()

    if (isMarketBreak) {
        KeywordUtil.markFailed('Tes gagal. Bursa sedang istirahat.', FailureHandling.STOP_ON_FAILURE)
    } else {
        KeywordUtil.markFailed('Tes gagal. Bursa sedang tutup.', FailureHandling.STOP_ON_FAILURE)
    }
}

def elemenDashboard = findTestObject('TEST_LOGIN/stock')

//NetworkChecker.verifyInternetConnection()
//Mobile.startApplication('/Users/bionsrevamp/Downloads/app-development-profile 1 (1).apk', true)
String applicationID = 'id.bions.bnis.android.v2'

try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

    KeywordUtil.logInfo("✅ Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
    KeywordUtil.markFailed('❌ Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage(), 
        FailureHandling.STOP_ON_FAILURE)
} 

//NetworkChecker.verifyInternetConnection()
//Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)
Mobile.setText(findTestObject('Login_firebase/User_id'), '23AA50456', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'kittiw222', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'kittiw333', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

Instant start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

//NetworkChecker.verifyInternetConnection()
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

TcpClient client = new TcpClient()

//client.connect('192.168.19.61', 62229 // FEED_SERVER_1
client.connect('trade.bions.id', 62229 // FEED_SERVER_1
    )

// Kirim login
//client.sendMessage('{ "action":"login", "user":"1B029", "password":"q" }')
//client.sendMessage('{ "action":"login", "user":"23AA50456", "password":"kittiw222" }')
// Listen 5 detik untuk capture response login
//client.listen(5)
// 🔌 Tutup koneksi
client.close()

Instant end = Instant.now()

long seconds = Duration.between(start, end).toMillis() / 1000

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

//Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 0)
ShimmerWait.waitForShimmerToDisappear(elemenDashboard, 2)

Mobile.swipe(500, 1500, 500, 500)

ShimmerWait.waitForShimmerToDisappear(elemenDashboard, 2)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Dashboard1.PNG')

Mobile.swipe(500, 1500, 500, 500)

ShimmerWait.waitForShimmerToDisappear(elemenDashboard, 2)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Dashboard2.PNG')

Mobile.swipe(500, 1500, 500, 500)

ShimmerWait.waitForShimmerToDisappear(elemenDashboard, 2)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Dashboard3.PNG')

Mobile.swipe(500, 1500, 500, 500)

ShimmerWait.waitForShimmerToDisappear(elemenDashboard, 2)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Dashboard4.PNG')

Mobile.tap(findTestObject('Profile/Profile'), 0)

Mobile.swipe(500, 1500, 500, 500)

Mobile.swipe(500, 1500, 500, 500)

Mobile.tap(findTestObject('Profile/Log_out'),0)

Mobile.tap (findTestObject('Profile/Sign_Out'),0)

Mobile.closeApplication()

