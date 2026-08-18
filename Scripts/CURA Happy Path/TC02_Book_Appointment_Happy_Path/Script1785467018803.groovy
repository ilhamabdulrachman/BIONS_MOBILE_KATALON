import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

TestObject byId(String id) {
    return new TestObject(id).addProperty('id', ConditionType.EQUALS, id)
}

try {
    String facility = 'Hongkong CURA Healthcare Center'
    String visitDate = '27/08/2026'
    String comment = 'Automated happy path appointment booking.'

    WebUI.openBrowser('')
    WebUI.navigateToUrl('https://katalon-demo-cura.herokuapp.com/')

    WebUI.waitForElementClickable(byId('btn-make-appointment'), 10)
    WebUI.click(byId('btn-make-appointment'))

    WebUI.waitForElementVisible(byId('txt-username'), 10)
    WebUI.setText(byId('txt-username'), 'John Doe')
    WebUI.setEncryptedText(byId('txt-password'), 'XV7iB5TALmQ=')
    WebUI.click(byId('btn-login'))

    WebUI.waitForElementVisible(byId('btn-book-appointment'), 10)
    WebUI.selectOptionByValue(byId('combo_facility'), facility, false)
    WebUI.click(byId('chk_hospotal_readmission'))
    WebUI.click(byId('radio_program_medicaid'))
    WebUI.setText(byId('txt_visit_date'), visitDate)
    WebUI.setText(byId('txt_comment'), comment)
    WebUI.click(byId('btn-book-appointment'))

    WebUI.verifyTextPresent('Appointment Confirmation', false)
    WebUI.verifyTextPresent(facility, false)
    WebUI.verifyTextPresent('Yes', false)
    WebUI.verifyTextPresent('Medicaid', false)
    WebUI.verifyTextPresent(visitDate, false)
    WebUI.verifyTextPresent(comment, false)
} finally {
    WebUI.closeBrowser()
}
