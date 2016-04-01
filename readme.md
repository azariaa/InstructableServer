This software uses Gradle to handle dependencies and builds a single jar which can be executed.
In order to use the DB, you must have MySQL installed. You must also execute "dbCreationScript.sql" to create a database.

To run LEA service simply run main in Service (no arguments are required).
To connect to the LEA service, you can run the main in ConnectToService.

If you want LEA to send real emails, set UserCredentials, email and password to your email and password. You may also be required to enable some security features on your web client. For using real calendar, you must obtain a jason_secret file from Google and place it in the resources folder.
If you do not want to send actual emails, set the "userRealtimeAgent" variable in ConnectToService to false.

The data directory includes the lexicon in lexiconEntries.txt, and examples in examples.csv.
"lexiconSyn.txt" is used to define synonyms and all relevant entires in lexciconEntries.txt are cloned accordingly.

You must create a "logging" folder.

If you use this software for an academic paper please cite: 
Azaria, Amos, Jayant Krishnamurthy, and Tom M. Mitchell. "Instructable Intelligent Personal Agent.", AAAI'16, (2016).