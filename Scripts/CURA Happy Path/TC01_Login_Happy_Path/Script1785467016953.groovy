import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

TestObject byId(String id) {
    return new TestObject(id).addProperty('id', ConditionType.EQUALS, id)
}

try {
    WebUI.openBrowser('')
    WebUI.navigateToUrl('https://katalon-demo-cura.herokuapp.com/')

    WebUI.waitForElementClickable(byId('btn-make-appointment'), 10)
    WebUI.click(byId('btn-make-appointment'))

    WebUI.waitForElementVisible(byId('txt-username'), 10)
    WebUI.setText(byId('txt-username'), 'John Doe')
    WebUI.setEncryptedText(byId('txt-password'), 'XV7iB5TALmQ=')
    WebUI.click(byId('btn-login'))

    WebUI.waitForElementVisible(byId('btn-book-appointment'), 10)
    WebUI.verifyTextPresent('Make Appointment', false)
    WebUI.verifyElementPresent(byId('combo_facility'), 10)
    WebUI.verifyElementPresent(byId('txt_visit_date'), 10)
} finally {
    WebUI.closeBrowser()
}
