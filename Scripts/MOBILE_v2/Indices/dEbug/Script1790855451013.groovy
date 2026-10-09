import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import internal.GlobalVariable as GlobalVariable
import com.utilities.BionsSocketClient as BionsSocketClient

String userId = GlobalVariable.G_userId
String password = GlobalVariable.G_password
String host = GlobalVariable.G_tradinghost
int feedPort = GlobalVariable.G_feedport.toInteger()
String clientIp = GlobalVariable.G_clientip

BionsSocketClient feedClient = new BionsSocketClient()
Map feedResult = feedClient.loginFeed(host, feedPort, userId, password, clientIp)
KeywordUtil.logInfo('Feed Login: ' + feedResult.success)

// Coba beberapa variasi simbol yang mungkin dikenali sebagai IHSG
List<String> kandidatSimbol = ['IHSG', 'COMPOSITE', 'JCI', 'IDXRG', 'IDXCOMPOSITE']

for (String simbol : kandidatSimbol) {
    KeywordUtil.logInfo('='.multiply(50))
    KeywordUtil.logInfo('MENCOBA SIMBOL: ' + simbol)
    KeywordUtil.logInfo('='.multiply(50))

    List quoteRow = feedClient.getStockQuoteSnapshot(simbol, 4000)

    if (quoteRow != null) {
        Map quote = BionsSocketClient.parseStockQuoteRow(quoteRow)
        KeywordUtil.logInfo('stockCode: ' + quote.stockCode + ' | boardCode: ' + quote.boardCode)
        KeywordUtil.logInfo('Open: ' + quote.open + ' | High: ' + quote.high + ' | Low: ' + quote.low)
        if (quote.open != 0.0 || quote.high != 0.0) {
            KeywordUtil.logInfo('🎯 SEPERTINYA BERHASIL - data tidak nol!')
        }
    } else {
        KeywordUtil.logInfo('Tidak ada response untuk simbol ini')
    }
}

feedClient.closeSocket()