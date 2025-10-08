# 🍪 House Of Foods

**An Android application built with Kotlin and Jetpack Compose for managing homemade food orders, kitchen preparation, and business analytics — inspired by the warmth and authenticity of home-cooked flavors.**

---

## 📱 Overview

**House Of Foods** is a modern Android app designed for a homemade snacks and sweets brand to handle daily operations — from taking orders to kitchen preparation and tracking business performance.  
Built using **Kotlin**, **Jetpack Compose**, and **Firebase**, it features a clean, brand-inspired UI and supports offline-safe data synchronization.

---

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material Design 3)
- **Database:** Firebase Cloud Firestore
- **Storage:** Firebase Storage (optional for assets/images)
- **Reports:** PDF & Excel export
- **Architecture:** MVVM + Repository Pattern
- **Tools:** Android Studio, Firebase SDKs, Gradle

---

## 🌈 UI Theme & Branding

- **Primary Colors:** Light Yellow (background), Navy Blue (accent text), White (cards)
- **Font Style:** Rounded and friendly handwritten-style headers
- **Theme:** Light Mode throughout
- **App Icon:** `hof_insta_dp.png`

---

## 🚀 Features

### 🧾 Order Management
- Add new orders with:
    - Auto-suggest for repeat customers
    - Auto-generated Order IDs (`HOFDDMMYY###`)
    - Dynamic dropdowns for delivery and payment modes
- Confirmation popup upon successful order submission

### 📋 Orders Master List
- Display customer info, items, prices, delivery & payment modes
- Update status via checkboxes (Payment Received / Prepared / Dispatched / Delivered)
- Filter by date and status

### 👩‍🍳 Kitchen Prep List
- Aggregate total quantities per item across all orders
- Mark items as prepared
- Filter by date

### 📊 Dashboard & Analytics
- View daily, weekly, and monthly reports
- Metrics: Revenue, Orders, AOV, Retention, Popular Items, Regions
- Item-wise pie chart and exportable PDF reports

### 💾 Data Export
- Export **Orders** and **Kitchen Prep Lists** → Excel
- Export **Dashboard Reports** → PDF
- Cleanly formatted with headers and structure

---

## 🔥 Firebase Structure

