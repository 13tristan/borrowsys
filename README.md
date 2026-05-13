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

# Facility / Equipment Borrowing System - Final Revised Version

## Database
1. Import `221borrowapp_final_simplified.sql` in phpMyAdmin or MySQL.
2. The script drops and recreates the database named `221borrowapp`.
3. All tables use InnoDB and all foreign keys are included.
4. Stored routines included:
    - `auth_RegisterBorrower`
    - `admin_AddCustodian`
    - `admin_SetCustodianStatus`
    - `borrower_CreateBorrowRequest`
    - `borrower_CancelRequest`
    - `borrower_UpdateAccountInfo`
    - `custodian_ProcessBorrowRequest`
    - `custodian_LogReturn`
    - `fn_BorrowItemCount`
    - `fn_UserActiveBorrowCount`

## Main transaction cycle
1. Borrower views available items.
2. Borrower or custodian creates a borrow request.
3. Custodian approves/rejects the request.
4. Approved requests generate borrow records and borrow items.
5. Custodian logs returned items.
6. The system updates item availability and condition status.

## Demo accounts
- Admin: `admin@slu.edu.ph` / `password123`
- Custodian: `juan.delacruz@slu.edu.ph` / `password123`
- Borrower: `mark.bautista@slu.edu.ph` / `password123`

## JDBC interfaces demonstrated
- `Statement`: dashboard and inventory status views.
- `PreparedStatement`: filtered views, inserts, and updates.
- `CallableStatement`: stored procedures used for registration, requests, approvals, returns, and account status updates.

