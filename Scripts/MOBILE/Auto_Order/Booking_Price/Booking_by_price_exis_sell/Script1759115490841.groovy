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
import com.utilities.OrderVerification as OrderVerification
import java.math.BigDecimal as BigDecimal

String clientID = '1B029'

String stockCode = 'ADRO'

BigDecimal orderPrice = new BigDecimal('500')

int lotAmount = 2

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

def elemenDashboard = findTestObject('NAVBAR/home_')

def Page_Portofolio = findTestObject('Portofolio/PAGE_PORTO')

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
Mobile.setText(findTestObject('Login_firebase/User_id'), '1B029', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'q12345', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

Instant start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

//NetworkChecker.verifyInternetConnection()
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

TcpClient client = new TcpClient()

client.connect('192.168.19.61', 62229 // FEED_SERVER_1
    //client.connect('trade.bions.id', 62229 // FEED_SERVER_1
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

Mobile.delay(3, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('Auto_order/_more_'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder.PNG')

Mobile.tap(findTestObject('Auto_order/Menu_Auto_Order'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder1.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder2.PNG')

Mobile.tap(findTestObject('Auto_order/tick_auto_order'), 0)

Mobile.tap(findTestObject('Auto_order/I accept'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder3.PNG')

//ShimmerWait.waitForShimmerToDisappear(Form_Auto_Order, 3)
Mobile.tap(findTestObject('Auto_order/Change_stock'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder4.PNG')

Mobile.setText(findTestObject('Auto_order/Select_Stock'), 'ADRO', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorde5.PNG')

Mobile.tap(findTestObject('Auto_order/tapsaham'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder6.PNG')

Mobile.tap(findTestObject('Auto_order/Select_Condition'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder7.PNG')

Mobile.tap(findTestObject('Auto_order/Booking_By_Price'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder8.PNG')

Mobile.setText(findTestObject('Auto_order/Input_Price_'), '500', 0)

Mobile.delay(5, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder9.PNG')

//Mobile.tap(findTestObject('Auto_order/buy_sendorder'), 0)
Mobile.swipe(500, 1500, 500, 500)

//Mobile.setText(findTestObject('Auto_order/Input_Price'), '133', 0)
Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.setText(findTestObject('Auto_order/Lot'), '', 0)

Mobile.setText(findTestObject('Auto_order/Lot'), '2', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder10.PNG')

Mobile.tap(findTestObject('Auto_order/ORDER_SEND_BUTTON'), 0)

Mobile.tap(findTestObject('Auto_order/Confirm_And_Submit'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder31.PNG')

KeywordUtil.logInfo('Order Sent at ' + now.format(fmt))

Mobile.tap(findTestObject('Auto_order/View_Order_List'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder32.PNG')

Mobile.delay(10, FailureHandling.STOP_ON_FAILURE)

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder33.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder34.PNG')

Mobile.delay(10, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder35.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder36.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Allmenuautoorder37.PNG')

KeywordUtil.logInfo("Memulai verifikasi database untuk order client ID $clientID...")

// Panggil Custom Keyword untuk menjalankan query Oracle dan membandingkan data
boolean dbVerificationResult = CustomKeywords.'com.utilities.OrderVerification.verifyLatestOrder'(clientID, stockCode, lotAmount, 
    orderPrice)

if (dbVerificationResult) {
    KeywordUtil.logInfo('✅ STATUS: Transaksi order berhasil dan SINKRON dengan Database Oracle.')
} else {
    KeywordUtil.logError('❌ STATUS: Ketidaksesuaian data order ditemukan di database.')
}

Mobile.closeApplication()

