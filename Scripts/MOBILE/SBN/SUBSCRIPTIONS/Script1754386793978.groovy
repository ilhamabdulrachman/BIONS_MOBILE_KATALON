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

Mobile.startApplication('/Users/bionsrevamp/Downloads/app-development-profile 4.apk', true)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)

Mobile.setText(findTestObject('TEST_LOGIN/user_id'), '1B029', 0)

Mobile.setText(findTestObject('TEST_LOGIN/pasword'), 'x', 0)

Mobile.setText(findTestObject('TEST_LOGIN/pin'), 'x12345', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 1)

Mobile.delay(1, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('SBN/Tap_Fixed_Income'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome.PNG')

Mobile.tap(findTestObject('SBN/Klik_Sbn'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome1.PNG')

Mobile.tap(findTestObject('SBN/ORITEST24-T3'), 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome2.PNG')

Mobile.tap(findTestObject('SBN/ORDER_NOW_SBN'), 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome3.PNG')

Mobile.delay(3, FailureHandling.STOP_ON_FAILURE)

Mobile.setText(findTestObject('SBN/amount - 1,000,000'), '', 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome4.PNG')

Mobile.tap(findTestObject('SBN/Download_Memorandum'), 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome5.PNG')

Mobile.delay(5, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome6.PNG')

Mobile.pressBack()

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome7.PNG')

Mobile.tap(findTestObject('SBN/continue'), 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome8.PNG')

Mobile.tap(findTestObject('SBN/Checkbox_sbn'), 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome9.PNG')

Mobile.tap(findTestObject('SBN/aggre_continue'), 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome10.PNG')

Mobile.checkElement(findTestObject('SBN/confirm_submit_sbn'), 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome11.PNG')

Mobile.tap(findTestObject('SBN/vIew_order_list_sbn'), 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome12.PNG')

Mobile.tap(findTestObject('SBN/skip_sbn'), 10)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincome13.PNG')

Mobile.closeApplication()

