# DCSA Core

This repository is part of DCSA's reference implementation of the API with the purpose of validating the API in practice.
The repository contains functionality shared among different applications, i.e. the core DCSA API functionality.
It is packaged as a jar, and uploaded to GitHub packages, to be downloaded via Maven



To build manually, run:
-----------------------------------------
```
mvn clean install
```

# How to use DCSA Core packages

To use Github Packages, Github Authentication must be set up, even if the packages are public (This seems to be limitation on GitHubs end)
This is done like this:

To do this, follow these steps:
 
##Creating a Github Personal Access Token 
Go to https://github.com/settings/tokens and click "Generate new token"
Write a note for the token, for example "MAVEN_PACKAGES_PAT"
Select read:packages, delete:packages, and write:packages. 
Click Generate Token
Copy the token, we will need it soon. If you leave this page without copying the token, you will need to generate a new one
Note: Treat this token as you would treat any password.
 
## Setting up settings.xml 
 
1. Go to your maven directory. Default directory on Windows is C:\Users\USERNAME\.m2 . If you don't have this directory, you may need to install Maven first.
2. Create a file called "settings.xml"
3. Paste the following into the file:
```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">

  <activeProfiles>
    <activeProfile>github</activeProfile>
  </activeProfiles>

  <profiles>
    <profile>
      <id>github</id>
      <repositories>
        <repository>
          <id>github</id>
          <name>GitHub dcsa Apache Maven Packages</name>
          <url>https://maven.pkg.github.com/dcsaorg/DCSA-Core</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>

        <repository>
          <id>central</id>
          <url>https://repo1.maven.org/maven2</url>
          <releases><enabled>true</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>

  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_NAME</username>
      <password>YOUR_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```
 
    4. Replace YOUR_GITHUB_NAME with your Github name, and replace YOUR_PERSONAL_ACCESS_TOKEN with the token generated earlier
    5. Save the file. You should now be able to use dcsa Github packages in your projects
