Sizing-Rule-Readme.txt

---------------

Description:
	This sizing rule counts various objects in the IdentityIQ installation. It does not alter or create any objects in the system. The results are printed and saved to two temporary files.

Instructions for use:

Running the rule directly:
	- There are two options for using this rule: through the UI or through iiq console. Use whichever you are more confortable with.

	UI Option-
		1. Importing the file:
			a) Copy Rule-Standalone-Install-Sizing-Rule.xml to a known location.
			b) Using the IdentityIQ UI, navigate to the System Setup page.
			c) Select "Import from File" and browse to the copy of Rule-Standalone-Install-Sizing-Rule.xml. Import the file.
		2. Running the rule
			a) Navigate to the IdentityIQ debug page
			b) There will be a Run button with a dropdown selector next to it. Select "Sailpoint Sizing Rule" in the dropdown and press the run button.
			c) The rule will run and the output will be displayed. At the bottom of the output, it will indicate where it is saving the temporary files.
		3. Retrieve the files and either pass them on to SailPoint or use them for your own edification. 
			a) The json file will be used in a future sizing calculator under development.
			b) The txt file contains the same information that was displayed in the debug page.

	Console Option - 
		1. Run iiq console
		2. Import the file using the following command and substituting your file path:
			import <path to your copy of Rule-Standalone-Install-Sizing-Rule.xml>
		3. Run the rule using the following command in the console:
			rule "Sailpoint Sizing Rule"
		4. The output will be displayed. At the bottom of the output, it will indicate where it is saving the temporary files.
		5. Retrieve the files and either pass them on to SailPoint or use them for your own edification. 
			a) The json file will be used in a future sizing calculator under development.
			b) The txt file contains the same information that was displayed in the debug page.

Running the rule using a task (Requires IdentityIQ 6.2+):
	1. Importing the file:
		a) Copy Rule-Standalone-Install-Sizing-Rule.xml to a known location.
		b) Copy TaskDefinition-Optional-Sailpoint-Sizing-Rule-Task-post-identityiq-6.2.xml to a known location.
		c) Using the IdentityIQ UI, navigate to the System Setup page.
		d) Select "Import from File" and browse to the copy of Rule-Standalone-Install-Sizing-Rule.xml. Import the file.
		e) Select "Import from File" and browse to the copy of  TaskDefinition-Optional-Sailpoint-Sizing-Rule-Task-post-identityiq-6.2.xml. Import the file.
	2. Running the task
		a) Navigation to Monitor->Tasks
		b) Execute the "Sailpoint Sizing Rule Task"
		c) Click on the Task Results tab
		d) When the task completes, click the task result
		e) The output will be in a large info section in the task result.
		 f) For ease of reading, this texts may be copy/pasted into a text editor, then a find/replace done to switch the "|" character with a newline character.



Removing the rule:
	If desired the rule may be removed from the system using the iiq console with the following command:
		delete Rule "Sailpoint Sizing Rule"
		delete TaskDefinition "Sailpoint Sizing Rule Task"
		
Description of Fields reported by the Rule:		

The “Total Identities” is simply the count of all of Identity objects in the 
system.  This can also be thought of as the row count in the “spt_identity” 
table, with a WHERE clause filtering out the workgroups.

The “Active Identities” is the count of identity objects who’s “IIQDisabled” 
property is set to false.  This is usually set during the account aggregation 
of HR systems.  If we see this equal to total identities then that tells us 
either the system is configured to not read in inactive identities or it is 
misconfigured and not properly populating “IIQDisabled”.

The “Inactive Identities” is the compliment of “Active Identities”.  It is the 
count of identity objects who’s “IIQ Disabled” property is set to true.  These 
usually represent inactive people from HR who have left the organization.  
According to SailPoint’s licensing terms for IdentityIQ, inactive identities 
do not count against the number of licensed identities.

The “Uncorrelated Identities” field is the count of how many identity records 
have their “isCorrelated” property set to false.  These are often service 
accounts that fail to correlate HR or other authoritative application records.  
Sometimes applications are configured with incorrect correlation that ignores 
some accounts and they end up in this count.

The “Identity Snapshots” is a peg counter for how many snapshots of previous 
identity object states are captured in the system. This has implications for 
performance; any number higher than the total identity count is a red flag here.

The “License Identities” is the count of Identity records that are active (have
their “IIQDisabled” property set to false) and also are correlated (have their 
“isCorrelated” property set to true).  These are identities that have 
affirmatively active HR or other authoritative source accounts.  These 
identities should count against the license agreements according to how our 
licenses are written.		

