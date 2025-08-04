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

Mobile.startApplication('/Users/bionsrevamp/Downloads/app-development-profile 2.apk', true)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)

Mobile.setText(findTestObject('TEST_LOGIN/user_id'), '23AA50456', 0)

Mobile.setText(findTestObject('TEST_LOGIN/pasword'), 'kittiw222', 0)

Mobile.setText(findTestObject('TEST_LOGIN/pin'), 'kittiw111', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 0)

Mobile.delay(1, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('TEST_LOGIN/see_all_trading_idea'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trading_Idea.PNG')

Mobile.tap(findTestObject('TEST_LOGIN/Klik_Trading_Idea'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trading_Idea1.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trading_Idea2.PNG')

Mobile.tap(findTestObject('TEST_LOGIN/Back_Trading_Ideas'), 1)

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trading_Idea3.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trading_Idea4.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trading_Idea5.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trading_Idea6.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trading_Idea7.PNG')

Mobile.closeApplication()

