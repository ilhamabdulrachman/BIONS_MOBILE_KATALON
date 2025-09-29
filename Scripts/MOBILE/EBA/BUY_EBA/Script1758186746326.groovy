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
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.Duration

//def elemenDashboard = findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR')
//NetworkChecker.verifyInternetConnection()
Mobile.startApplication('/Users/bionsrevamp/Downloads/app-development-profile 1 (1).apk', true)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

//NetworkChecker.verifyInternetConnection()
Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)

Mobile.setText(findTestObject('Login_firebase/User_id'), '1B029', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'q12345', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

Instant start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

def now = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
def fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

KeywordUtil.logInfo("Login successful at " + now.format(fmt))

//NetworkChecker.verifyInternetConnection()
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

TcpClient client = new TcpClient()

client.connect('192.168.19.61', 62229 // FEED_SERVER_1
    )

// Kirim login
client.sendMessage('{ "action":"login", "user":"1B029", "password":"q" }')

// Listen 5 detik untuk capture response login
client.listen(5)

// 🔌 Tutup koneksi
client.close()
end = Instant.now()

seconds = (Duration.between(start, end).toMillis() / 1000)

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 0)

Mobile.delay(1, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('SBN/Tap_Fixed_Income'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba1.PNG')

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('EBA_BUY/INDAH0105'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba2.PNG')

Mobile.tap(findTestObject('EBA_BUY/BUTTON_BUY'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba3.PNG')

Mobile.setText(findTestObject('EBA_BUY/AMOUNT'), '1000000', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba4.PNG')

Mobile.tap(findTestObject('EBA_BUY/Buy_Eba'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba5.PNG')

Mobile.tap(findTestObject('EBA_BUY/Tick_buy_eba'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba6.PNG')

Mobile.tap(findTestObject('EBA_BUY/confirm_submit'), 0)

KeywordUtil.logInfo("Order Sent at " + now.format(fmt))

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba7.PNG')

Instant start = Instant.now()

Mobile.tap(findTestObject('EBA_BUY/view_order_lisf'), 0)

Instant end = Instant.now()
long seconds = Duration.between(start, end).toMillis() / 1000

KeywordUtil.markPassed("⏱️ Order List terbuka dalam ${seconds} detik")

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba8.PNG')

Mobile.tap(findTestObject('EBA_BUY/skip_tour_eba'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba9.PNG')

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba10.PNG')

Mobile.closeApplication()

