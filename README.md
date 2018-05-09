Setup environment to run the visualApp application on AWS EC2:

	1.	Create an AWS EC2 instance and add the IAM role that has administrative privileges.
	2.	SCP Upload the visualAPP.tar application onto EC2 using AWS credentials and EC2 IP address
	3.	SSH into the EC2 instance
	4.	Run the following commands on EC2 instance: 
		a. Unzip application: tar xzvf visualAPP.tar
		b. Install JAVA SDK: look up how to on internet
		c. Install apache maven compiler on Amazon Linux: look up how to on internet
		d. Add both maven compiler and JAVA SDK to your environment path variable (if they are not in there already)

To run the application on edge-cloud architecture: 

	1.	type the following (replace ??? with your AWS account accessKeyId and secretKey) each in a separate terminal:

		MAVEN_OPTS="-Daws.accessKeyId=??? -Daws.secretKey=???" mvn compile -PdbWriter exec:java

		MAVEN_OPTS="-Daws.accessKeyId=??? -Daws.secretKey=???" mvn compile -Pwebserver exec:java

		Open web browser: type http://localhost:8080 (if not working, use http://localhost:8080/overview.html)

	2.	To change data rate:
		a. Open visualApp/src/main/java/org/example/basicApp/ddb/DynamoDBWriter.java, 
		b. Go to Line 116 and change the value from sleep(1000) to sleep(5000) for 5 sec and sleep(10000) for 10 sec.
		c. Open visualApp/src/main/static-content/wwwroot/overview.js, 
		d. go to line 269 and change the 1000 to 5000 or 10000 based on step 2.b.

	3.	To change name of DynamoDB:
		a. Edit visualApp/pom.xml (rename the DB table to whatever you want to)

	4.	To change number of users:
		a. Open visualApp/src/main/java/org/example/basicApp/ddb/DynamoDBWriter.java
		b. Go to Line 62 and change numUsers=1 to 5, 10, etc.

To run the application on cloud only architecture: 

	1.	type the following (replace ??? with your AWS account accessKeyId and secretKey) each in a separate terminal:

		MAVEN_OPTS="-Daws.accessKeyId=??? -Daws.secretKey=???" mvn compile -Pstream-writer exec:java

		MAVEN_OPTS="-Daws.accessKeyId=??? -Daws.secretKey=???" mvn compile -PclientApp exec:java

		MAVEN_OPTS="-Daws.accessKeyId=??? -Daws.secretKey=???" mvn compile -Pwebserver exec:java

		Open web browser: type http://localhost:8080 (if not working, use http://localhost:8080/overview.html)

	2.	To change data rate:

		a. Open visualApp /src/main/java/org/example/basicApp/writer/MeasurementWriter.java, 
		b. Go to Line 29 and change the value from 1000 to 5000 for 5 sec and 10000 for 10 sec.
		c. Open visualApp/src/main/static-content/wwwroot/overview.js, 
		d. Go to line 269 and change the 1000 to 5000 or 10000 based on step 2.a.

	3.	To change name of DynamoDB: Edit visualApp/pom.xml (rename: sample-application.name, sample-application.stream, and sample-application.measurement-table, to whatever you like)

	4.	To change number of users: Open visualApp/src/main/java/org/example/basicApp/ writer/MeasurementPutter.java

b.	Go to Line 23 and change numUsers=1 to 5, 10, etc.

