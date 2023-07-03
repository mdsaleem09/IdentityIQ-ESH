
FIELD VALUE FRAMEWORK README

Quick Deployment Steps:
1. Copy the files SPCONF_FieldValue_RuleLibrary.xml and SPCONF_FieldValueMappings_Custom.xml to a customer config folder.

2. Create mappings in the Custom object in the form of a direct attribute mapping, rule, script or velocity statement, using the supplied sample as a template.
   Direct mapping example:
     <Field name="givenName" type="attribute" value="firstname"/>
   Rule (method) example:
     <Field defaultValue="Corporate HQ" name="physicalDeliveryOfficeName" type="rule" value="getCustomOffice"/>
   Rule (script) example:
     <Field name="displayName" type="rule">
       <Value>
         <Script>
           <Source>
             return identity.getDisplayableName();
           </Source>
         </Script>
        </Value>
      </Field>
   Velocity example:
      <Field name="department" type="velocity" value="$identity.getStringAttribute(&quot;department&quot;)"/>
    
3. Add methods referenced in the Custom object to your custom rule library .

See documentation for full details.