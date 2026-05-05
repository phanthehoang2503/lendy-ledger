# Lendy Ledger

Lendy Ledger is a personal debt management application for Android. It provides a structured way to track money lent and borrowed, featuring a clean interface and robust data management.

## Key Features

*   **Debt and Loan Tracking**: Record lending and borrowing activities.
*   **Transaction History**: Detailed logs of repayments and additional loans with balance snapshots.
*   **Visual Analytics**: Financial trends visualization using MPAndroidChart.
*   **Data Recovery**: Soft-delete mechanism to hide contacts while preserving historical data.
*   **Material 3 Design**: Compliant with modern Android design standards, featuring edge-to-edge support.
*   **Performance**: Optimized UI interactions using ListAdapter and DiffUtil.

## Architecture

The project follows the MVVM (Model-View-ViewModel) architectural pattern:

*   **Model**: Room Database entities and a Singleton Repository for data coordination.
*   **View**: Material 3 UI components utilizing ViewBinding.
*   **ViewModel**: Manages UI state and communicates with the repository using LiveData.

## Tech Stack

*   **Language**: Java 17+
*   **Database**: Room Persistence Library (SQLite).
*   **UI Components**: Material Components (M3), ViewPager2, RecyclerView.
*   **Libraries**:
    *   **Lombok**: Used for reducing boilerplate code.
    *   **MPAndroidChart**: Used for financial data visualization.
    *   **Timber**: Used for logging.
*   **Concurrency**: ExecutorService for background database operations.

## Database Design and Integrity

The application utilizes a relational schema to ensure data consistency:
*   **Data Integrity**: Foreign Key constraints with ON DELETE CASCADE prevent orphaned records.
*   **Soft Delete**: Allows removing contacts from the active list without losing financial context.
*   **Snapshots**: Transaction records include balance snapshots to maintain historical accuracy.

## Getting Started

### Prerequisites
*   Android Studio Koala (2024.1.1) or newer.
*   Android SDK 34 (Upside Down Cake).
*   JDK 17.

### Installation
1.  Clone the repository:
    ```bash
    git clone https://github.com/phanthehoang2503/lendy-ledger.git
    ```
2.  Open the project in Android Studio.
3.  Sync project with Gradle files.
4.  Build and run on an emulator or physical device.

## Contribution and Standards
*   **Coding Style**: Adheres to standard Android Java conventions.
*   **Documentation**: Business logic is documented using Vietnamese Javadoc.
*   **UI Binding**: Strict use of ViewBinding (no findViewById).

---
*Developed for personal financial management.*
