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

//boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()
//if (isMarketOpen) {
//  KeywordUtil.logInfo('Bursa sedang buka. Melanjutkan pengujian login...' // Jika bursa tutup, hentikan tes
//      )
//} else {
//    KeywordUtil.markFailed('Tes gagal. Bursa sedang tutup.', FailureHandling.STOP_ON_FAILURE)
//}
def elemenDashboard = findTestObject('TEST_LOGIN/stock')

//NetworkChecker.verifyInternetConnection()
String applicationID = 'id.bions.bnis.android.v2'

try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

    KeywordUtil.logInfo("✅ Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
    KeywordUtil.markFailed('❌ Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage(), 
        FailureHandling.STOP_ON_FAILURE)
} 

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

//NetworkChecker.verifyInternetConnection()
Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)

Mobile.setText(findTestObject('Login_firebase/User_id'), '1B029', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)

Mobile.setText(findTestObject('TEST_LOGIN/Pin2'), 'q12345', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

Instant start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

Instant end = Instant.now()

long seconds = Duration.between(start, end).toMillis() / 1000

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

//NetworkChecker.verifyInternetConnection()
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

TcpClient client = new TcpClient()

client.connect('192.168.19.61', 62229 // FEED_SERVER_1
    )

// Kirim login
client.sendMessage('{ "action":"login", "user":"1B029", "password":"q" }')

// Listen 5 detik untuk capture response login
client.listen(5)

// 🔌 Tutup koneksi
client.close()

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('Profile/Profile'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Profile.PNG')

//Mobile.tap(findTestObject('Profile/Finish_'), 0)
//
//Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Profile11.PNG')
Mobile.tap(findTestObject('Profile/wd'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/WDR.PNG')

Mobile.setText(findTestObject('WITHDRAW/Enter_Amount'), '500000', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/WDR1.PNG')

//Mobile.hideKeyboard()

//Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/WDR1.PNG')

//Mobile.hideKeyboard()

Mobile.setText(findTestObject('WITHDRAW/enter_pin'), 'q12345', 0)

Mobile.tap(findTestObject('Profile/Continue'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/WDR2.PNG')

Mobile.tap(findTestObject('Mobile/TICK_wd'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/WDR3.PNG')

start1 = Instant.now()

Mobile.tap(findTestObject('Profile/confirm_submit'), 0)

end1 = Instant.now()

seconds = (Duration.between(start1, end1).toMillis() / 1000)

KeywordUtil.logInfo("⏱️ Waktu Saat withdrawl: $seconds detik")

KeywordUtil.logInfo('Withdrawl successful at ' + now.format(fmt))

Mobile.tap(findTestObject('Profile/View_History'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/WDR4.PNG')

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/WDR5.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/WDR6.PNG')

Mobile.closeApplication()

