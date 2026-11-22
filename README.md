GPU Utilization Management

Java Swing application for the COSC230 Object-Oriented Programming course project (Fall 2026, lab section B2). It manages access to 4 simulated NVIDIA H100 GPUs: students request accounts, submit jobs within their time allowance, and an administrator approves requests, grants extra time and advances a simulated clock.

Team
Kinan Zein Aldean (100068489): model classes, Validator, AccountService, saved-line format, sample data, admin Requests and Students panels
Yousef Allam (100068083): SimulationClock, Scheduler, AppContext, AdminFrame, DashboardPanel, AllJobsPanel
Ali Abul Khair (100068799): FileHelper, DataStore, DemoDataBuilder, LoginFrame, RegisterPanel, student screens, ClockSetupDialog, Main
Features
Student registration requests, approved or rejected by the administrator
Login for students and the administrator
Job submission (Small 5, Medium 15, Large 30, Very large 60 minutes), checked against the student's available time
First-come-first-served queue, lowest-numbered free GPU first
Simulated clock that moves only with the "+1 minute" button
Cancelling jobs: a queued job is free, a running job is charged only for the minutes it ran
Granting extra time with a reason
Dashboard with GPUs, waiting jobs, completed jobs and student allowances
All data saved to text files in data/ and loaded again on start
Requirements
JDK 17 or newer (no external libraries)
Compile and run

From the project root folder:

javac -d out src/model/*.java src/logic/*.java src/storage/*.java src/ui/*.java src/app/*.java
java -cp out app.Main
Test accounts
Administrator: admin / gpu2026
Demo students: university IDs 1001 to 1005, password demo123
Project structure
src/model: data classes (User, Student, Administrator, Job, GPU, TimeAllowance, AccessRequest, TimeGrant, enums)
src/logic: rules and services (Validator, AccountService, Scheduler, SimulationClock)
src/storage: reading and writing the data files (FileHelper, DataStore, DemoDataBuilder)
src/ui: Swing windows and panels
src/app: Main, the entry point
data: saved students, requests, grants, jobs and clock
Rules and assumptions

See the project report.