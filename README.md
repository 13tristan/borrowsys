# Project Setup Guide

## Getting Started

Follow these steps to properly set up and run the project:

### 1. Clone the Repository

First, clone this repository to your local machine:

```
git clone <your-repository-url>
```

### 2. Create the `lib` Folder

After cloning, create a folder named `lib` in the root directory of the project.

> ⚠️ Make sure the `lib` folder is **outside** the `src` folder.

Your project structure should look something like this:

```
project-root/
├── src/
├── lib/
└── ...
```

### 3. Add the Connector/J JAR File

Place the Connector/J `.jar` file inside the `lib` folder.

### 4. Install Connector/J (If Not Installed)

If you do not already have the Connector/J driver:

* Download and install it from the official source.
* Once downloaded, locate the `.jar` file.
* Copy and paste the `.jar` file into the `lib` folder you created earlier.


### 5. Configure IntelliJ IDEA to Recognize the JAR

* Open your project in IntelliJ IDEA.
* Go to File → Project Structure → Modules.
* Select your module (usually the project name) and go to the Dependencies tab.
* Click the + button → JARs or directories.
* Navigate to the lib folder inside the project and select the Connector/J .jar file.
