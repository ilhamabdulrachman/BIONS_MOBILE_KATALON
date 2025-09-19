<?xml version="1.0" encoding="UTF-8"?>
<WebServiceRequestEntity>
   <description></description>
   <name>Login_API</name>
   <tag></tag>
   <elementGuidId>7c2ed2a7-339b-412a-8b04-b8ed462b988d</elementGuidId>
   <selectorMethod>BASIC</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>false</useRalativeImagePath>
   <autoUpdateContent>false</autoUpdateContent>
   <connectionTimeout>-1</connectionTimeout>
   <followRedirects>true</followRedirects>
   <httpBody></httpBody>
   <httpBodyContent>{
  &quot;text&quot;: &quot;{\n  \&quot;userId\&quot;: \&quot;1B029\&quot;,\n  \&quot;password\&quot;: \&quot;hKQjNKyF5DiaZ7IfTY/I7QMm84+l2oe3aLW5n0p+Y+bWOZUMDZSThPsh7Ue3+sAv4uelX2JYE5zDQ5/W9ilU8zZEQVnbzPeYlexVAiVTHJXwZ8gChaYQNaX7rwL7t46G5raV+0+D7hoHawYitgi53vPXMRxxekQxPjrhILEQ/OulUxU91IZP+l2XbPqK/UiAOewxlkzrBv6KfwhTwedi0c/H1bhkjF0Cy6f+yOjznmKfuPn3lUixK/vvq/+dUrpJxzGJHJEKd/Frgzps8Rcu1KYox2WyX+bQrsxvfAcuU2LKtvB/pHLhjjvA5lo5XEPTWAZ9Lo14eYO135K9oc19tg\u003d\u003d\&quot;,\n  \&quot;pin\&quot;: \&quot;hKQjNKyF5DiaZ7IfTY/I7QMm84+l2oe3aLW5n0p+Y+bWOZUMDZSThPsh7Ue3+sAv4uelX2JYE5zDQ5/W9ilU8zZEQVnbzPeYlexVAiVTHJXwZ8gChaYQNaX7rwL7t46G5raV+0+D7hoHawYitgi53vPXMRxxekQxPjrhILEQ/OulUxU91IZP+l2XbPqK/UiAOewxlkzrBv6KfwhTwedi0c/H1bhkjF0Cy6f+yOjznmKfuPn3lUixK/vvq/+dUrpJxzGJHJEKd/Frgzps8Rcu1KYox2WyX+bQrsxvfAcuU2LKtvB/pHLhjjvA5lo5XEPTWAZ9Lo14eYO135K9oc19tg\u003d\u003d\&quot;\n}\n&quot;,
  &quot;contentType&quot;: &quot;application/json&quot;,
  &quot;charset&quot;: &quot;UTF-8&quot;
}</httpBodyContent>
   <httpBodyType>text</httpBodyType>
   <httpHeaderProperties>
      <isSelected>true</isSelected>
      <matchCondition>equals</matchCondition>
      <name>Content-Type</name>
      <type>Main</type>
      <value>application/json</value>
      <webElementGuid>6a07d631-4f09-42bc-a747-145fc62b7c42</webElementGuid>
   </httpHeaderProperties>
   <katalonVersion>10.0.1</katalonVersion>
   <maxResponseSize>-1</maxResponseSize>
   <migratedVersion>5.4.1</migratedVersion>
   <path></path>
   <restRequestMethod>POST</restRequestMethod>
   <restUrl>https://wsDEV.bions.id/fo/auth-bions/login</restUrl>
   <serviceType>RESTful</serviceType>
   <soapBody></soapBody>
   <soapHeader></soapHeader>
   <soapRequestMethod></soapRequestMethod>
   <soapServiceEndpoint></soapServiceEndpoint>
   <soapServiceFunction></soapServiceFunction>
   <socketTimeout>-1</socketTimeout>
   <useServiceInfoFromWsdl>true</useServiceInfoFromWsdl>
   <verificationScript>import static org.assertj.core.api.Assertions.*

import com.kms.katalon.core.testobject.RequestObject
import com.kms.katalon.core.testobject.ResponseObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webservice.verification.WSResponseManager

import groovy.json.JsonSlurper
import internal.GlobalVariable as GlobalVariable

RequestObject request = WSResponseManager.getInstance().getCurrentRequest()

ResponseObject response = WSResponseManager.getInstance().getCurrentResponse()</verificationScript>
   <wsdlAddress></wsdlAddress>
</WebServiceRequestEntity>
