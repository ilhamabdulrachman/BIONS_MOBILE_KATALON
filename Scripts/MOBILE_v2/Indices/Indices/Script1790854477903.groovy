import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import internal.GlobalVariable as GlobalVariable
import java.time.Instant as Instant
import java.time.Duration as Duration
import com.utilities.BionsSocketClient as BionsSocketClient
import com.utilities.NetworkDiagnostic as NetworkDiagnostic
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import com.kms.katalon.core.llm.keyword.LlmKeywords as LLM
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import org.openqa.selenium.Keys as Keys

// ============================================================
// KONFIGURASI
// ============================================================
String applicationID = 'id.bions.bnis.android.new_bions_revamp'

String userId = GlobalVariable.G_userId

String password = GlobalVariable.G_password

String pin = GlobalVariable.G_pin

String host = GlobalVariable.G_tradinghost

int feedPort = GlobalVariable.G_feedport.toInteger()

String clientIp = GlobalVariable.G_clientip

// ============================================================
// STEP 1: LAUNCH APPLICATION
// ============================================================
try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

    KeywordUtil.logInfo("Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
    KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Error: ' + e.getMessage())

    return null
} 

// ============================================================
// STEP 2: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

Instant loginStart = Instant.now()

Mobile.tap(findTestObject('Login_V2/button_login'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login2.PNG')

boolean loginSuccess = Mobile.verifyElementExist(findTestObject('Login_V2/button_notnow'), 10, FailureHandling.OPTIONAL)

if (!(loginSuccess)) {
    Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login3.PNG')

    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)

    KeywordUtil.markFailed('❌ Login GAGAL - biometric prompt tidak muncul dalam 10 detik. ' + "Diagnosa: $diagnostic.diagnosis")

    return null
}

Instant loginEnd = Instant.now()

double loginSeconds = Duration.between(loginStart, loginEnd).toMillis() / 1000.0

KeywordUtil.logInfo("⏱️ Waktu login: $loginSeconds detik")

KeywordUtil.logInfo('✅ Login berhasil terverifikasi.')

Mobile.tap(findTestObject('Login_V2/button_notnow'), 0)

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login5.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login6.PNG')

Mobile.tap(findTestObject('Indices/SeeAll_Indices'), 0)

Mobile.swipe(500, 1500, 500, 500)

Mobile.closeApplication()

BionsSocketClient feedClient = new BionsSocketClient()

try {
   Map feedResult = feedClient.loginFeed(host, feedPort, userId, password, clientIp)
   KeywordUtil.logInfo('Feed Login: ' + feedResult.success)
} catch (Exception e) {
   KeywordUtil.logInfo('Feed Login gagal: ' + e.message)
   Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)
   KeywordUtil.markFailed('Feed Login gagal. Diagnosa: ' + diagnostic.diagnosis)
   return null
}

List indicesRaw = feedClient.getAllMarketIndices()

String garis = '='.multiply(70)
KeywordUtil.logInfo(garis)
KeywordUtil.logInfo('DAFTAR SEMUA INDEKS PASAR')
KeywordUtil.logInfo(garis)

int idx = 1
for (List row : indicesRaw) {
   Map indexData = BionsSocketClient.parseMarketInfoRow(row)
   if (indexData != null) {
	   String baris = idx + '. ' + indexData.marketName +
			   ' | Last: ' + indexData.last +
			   ' | Change: ' + indexData.change +
			   ' (' + indexData.changePct + '%)'
	   KeywordUtil.logInfo(baris)
   }
   idx = idx + 1
}

KeywordUtil.logInfo(garis)
KeywordUtil.logInfo('Total indeks ditemukan: ' + indicesRaw.size())
KeywordUtil.logInfo(garis)

feedClient.closeSocket()


